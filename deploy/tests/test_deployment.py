import hashlib
import importlib.util
import io
from pathlib import Path
import shutil
import stat
import subprocess
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch
import zipfile


SCRIPT = Path(__file__).resolve().parents[1] / "education-deploy.py"
SPEC = importlib.util.spec_from_file_location("education_deploy", SCRIPT)
deployment = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(deployment)
COMMIT = "a" * 40


class DeploymentTests(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        self.app = self.root / "application"
        self.incoming = self.root / "incoming"
        self.app.mkdir()
        self.incoming.mkdir()
        self.current = self.app / "app.jar"
        self.current.write_bytes(b"previous working JAR")
        self.source = self.incoming / f"{COMMIT}.jar"
        with zipfile.ZipFile(self.source, "w") as archive:
            archive.writestr(
                "META-INF/MANIFEST.MF",
                "Main-Class: org.springframework.boot.loader.launch.JarLauncher\n"
                "Start-Class: com.edu.com.EducationSystemApplication\n",
            )
        self.checksum = hashlib.sha256(self.source.read_bytes()).hexdigest()
        for name, value in (("APP_DIR", self.app), ("INCOMING_DIR", self.incoming)):
            self.enterContext(patch.object(deployment, name, value))
        self.root_check = self.enterContext(patch.object(deployment, "require_root_owned"))
        self.enterContext(patch.object(deployment, "set_jar_permissions"))
        self.copy = self.enterContext(
            patch.object(
                deployment, "copy_candidate",
                side_effect=lambda commit, destination: shutil.copyfile(
                    self.incoming / f"{commit}.jar", destination
                ),
            )
        )
        self.restart = self.enterContext(patch.object(deployment, "restart_service"))
        self.health = self.enterContext(patch.object(deployment, "wait_for_health", return_value=True))
        self.enterContext(patch.object(deployment.signal, "signal"))

    def test_success_installs_new_jar_and_keeps_previous(self):
        deployment.promote(COMMIT, self.checksum)
        self.assertEqual(self.current.read_bytes(), self.source.read_bytes())
        self.assertEqual((self.app / "previous.jar").read_bytes(), b"previous working JAR")
        self.restart.assert_called_once_with()
        self.assertEqual(list(self.app.glob(".deploy-*")), [])

    def test_checksum_mismatch_does_not_replace_or_restart(self):
        with self.assertRaisesRegex(deployment.DeploymentError, "checksum"):
            deployment.promote(COMMIT, "0" * 64)
        self.assertEqual(self.current.read_bytes(), b"previous working JAR")
        self.restart.assert_not_called()
        self.assertFalse((self.app / "previous.jar").exists())

    def test_invalid_archive_does_not_replace_or_restart(self):
        self.source.write_bytes(b"not a JAR")
        checksum = hashlib.sha256(self.source.read_bytes()).hexdigest()
        with self.assertRaisesRegex(deployment.DeploymentError, "Spring Boot JAR"):
            deployment.promote(COMMIT, checksum)
        self.assertEqual(self.current.read_bytes(), b"previous working JAR")
        self.restart.assert_not_called()

    def test_copy_failure_preserves_running_release(self):
        self.copy.side_effect = subprocess.TimeoutExpired("runuser", 60)
        with self.assertRaises(subprocess.TimeoutExpired):
            deployment.promote(COMMIT, self.checksum)
        self.assertEqual(self.current.read_bytes(), b"previous working JAR")
        self.restart.assert_not_called()

    def test_unhealthy_release_restores_previous_and_reports_failure(self):
        self.health.side_effect = [False, True]
        with self.assertRaisesRegex(deployment.DeploymentError, "Previous JAR is running again"):
            deployment.promote(COMMIT, self.checksum)
        self.assertEqual(self.current.read_bytes(), b"previous working JAR")
        self.assertEqual(self.restart.call_count, 2)

    def test_restart_failure_restores_previous(self):
        self.restart.side_effect = [subprocess.CalledProcessError(1, "systemctl"), None]
        with self.assertRaisesRegex(deployment.DeploymentError, "Previous JAR is running again"):
            deployment.promote(COMMIT, self.checksum)
        self.assertEqual(self.current.read_bytes(), b"previous working JAR")
        self.assertEqual(self.restart.call_count, 2)

    def test_unhealthy_recovery_remains_a_failed_deployment(self):
        self.health.side_effect = [False, False]
        with self.assertRaisesRegex(deployment.DeploymentError, "restored but is unhealthy"):
            deployment.promote(COMMIT, self.checksum)
        self.assertEqual(self.current.read_bytes(), b"previous working JAR")


class BoundaryTests(unittest.TestCase):
    def test_rejects_paths_shell_text_and_extra_arguments(self):
        for arguments in (
            [], [COMMIT], [COMMIT, "b" * 64, "extra"],
            ["../../etc/passwd", "b" * 64],
            [COMMIT + ";id", "b" * 64], [COMMIT, "$(id)"],
        ):
            with self.subTest(arguments=arguments):
                with self.assertRaises(deployment.DeploymentError):
                    deployment.validate_arguments(arguments)
        self.assertEqual(deployment.validate_arguments([COMMIT, "b" * 64]), (COMMIT, "b" * 64))

    def test_rejects_untrusted_installation_permissions(self):
        for mode, owner in ((stat.S_IFREG | 0o660, 0), (stat.S_IFREG | 0o640, 1000),
                            (stat.S_IFLNK | 0o777, 0)):
            with self.subTest(mode=mode, owner=owner):
                with patch.object(Path, "lstat", return_value=SimpleNamespace(st_mode=mode, st_uid=owner)):
                    with self.assertRaises(deployment.DeploymentError):
                        deployment.require_root_owned(Path("/unused"))

    def test_upload_is_read_as_deploy_with_a_size_limit(self):
        def copy_command(arguments, **options):
            self.assertEqual(arguments[:5], ["/usr/sbin/runuser", "-u", "deploy", "--", "/usr/bin/head"])
            self.assertEqual(arguments[5:7], ["-c", str(deployment.MAX_JAR_BYTES + 1)])
            self.assertEqual(arguments[-1], f"/home/deploy/incoming/{COMMIT}.jar")
            options["stdout"].write(b"data")

        with tempfile.TemporaryDirectory() as directory:
            destination = Path(directory) / "candidate"
            with patch.object(deployment.subprocess, "run", side_effect=copy_command):
                deployment.copy_candidate(COMMIT, destination)
            self.assertEqual(destination.read_bytes(), b"data")

    def test_oversized_upload_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            with patch.object(deployment, "MAX_JAR_BYTES", 4):
                with patch.object(
                    deployment.subprocess, "run",
                    side_effect=lambda *args, **kwargs: kwargs["stdout"].write(b"12345"),
                ):
                    with self.assertRaisesRegex(deployment.DeploymentError, "256 MiB"):
                        deployment.copy_candidate(COMMIT, Path(directory) / "candidate")

    def test_health_requires_active_service_and_top_level_up(self):
        for body, active, expected in (
            (b'{"status":"UP"}', 0, True),
            (b'{"status":"DOWN","components":{"db":{"status":"UP"}}}', 0, False),
            (b'{"status":"UP"}', 3, False),
            (b'not JSON', 0, False),
        ):
            with self.subTest(body=body, active=active):
                response = io.BytesIO(body)
                response.status = 200
                with patch.object(deployment.urllib.request, "build_opener") as opener:
                    opener.return_value.open.return_value = response
                    with patch.object(deployment.subprocess, "run", return_value=SimpleNamespace(returncode=active)):
                        with patch.object(deployment.time, "monotonic", side_effect=[0, 0, 2]):
                            with patch.object(deployment.time, "sleep"), patch.object(deployment, "HEALTH_TIMEOUT", 1):
                                self.assertEqual(deployment.wait_for_health(), expected)

    def test_lock_rejects_a_second_deployment(self):
        with tempfile.TemporaryDirectory() as directory:
            lock_path = Path(directory) / "deploy.lock"
            with lock_path.open("a") as lock:
                deployment.fcntl.flock(lock, deployment.fcntl.LOCK_EX | deployment.fcntl.LOCK_NB)
                with patch.object(deployment, "LOCK_FILE", lock_path), patch.object(deployment, "promote") as promote:
                    with patch.object(deployment.os, "geteuid", return_value=0), patch.object(deployment.os, "umask"):
                        with patch.object(deployment.signal, "signal"), patch.object(deployment.sys, "argv", ["deploy", COMMIT, "b" * 64]):
                            with patch.object(deployment.sys, "stderr", new=io.StringIO()) as errors:
                                self.assertEqual(deployment.main(), 1)
                                self.assertIn("already running", errors.getvalue())
                    promote.assert_not_called()


if __name__ == "__main__":
    unittest.main()
