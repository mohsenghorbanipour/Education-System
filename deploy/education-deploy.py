#!/usr/bin/python3 -I

import fcntl
import grp
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import signal
import stat
import subprocess
import sys
import tempfile
import time
import urllib.request
import zipfile


APP_DIR = Path("/opt/education-system")
INCOMING_DIR = Path("/home/deploy/incoming")
LOCK_FILE = Path("/run/education-system-deploy.lock")
SERVICE = "education-system.service"
HEALTH_URL = "http://127.0.0.1:8080/actuator/health"
MAX_JAR_BYTES = 256 * 1024 * 1024
HEALTH_TIMEOUT = 180
COMMAND_ENV = {"PATH": "/usr/sbin:/usr/bin:/sbin:/bin", "LANG": "C.UTF-8"}


class DeploymentError(RuntimeError):
    pass


def validate_arguments(arguments):
    if len(arguments) != 2:
        raise DeploymentError("Usage: education-deploy COMMIT_SHA JAR_SHA256")
    commit, checksum = arguments
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise DeploymentError("Commit must be a 40-character lowercase Git SHA.")
    if not re.fullmatch(r"[0-9a-f]{64}", checksum):
        raise DeploymentError("Checksum must be a lowercase SHA-256 digest.")
    return commit, checksum


def require_root_owned(path, directory=False):
    info = path.lstat()
    correct_type = stat.S_ISDIR(info.st_mode) if directory else stat.S_ISREG(info.st_mode)
    if not correct_type or info.st_uid != 0 or info.st_mode & 0o022:
        raise DeploymentError(f"{path} must be root-owned and not writable by group or others.")


def copy_candidate(commit, destination):
    source = INCOMING_DIR / f"{commit}.jar"
    with destination.open("xb") as output:
        subprocess.run(
            [
                "/usr/sbin/runuser", "-u", "deploy", "--",
                "/usr/bin/head", "-c", str(MAX_JAR_BYTES + 1), str(source),
            ],
            stdout=output,
            check=True,
            timeout=60,
            env=COMMAND_ENV,
        )
    if not 0 < destination.stat().st_size <= MAX_JAR_BYTES:
        raise DeploymentError("JAR must contain between 1 byte and 256 MiB.")


def verify_candidate(path, expected_checksum):
    with path.open("rb") as stream:
        actual_checksum = hashlib.file_digest(stream, "sha256").hexdigest()
    if actual_checksum != expected_checksum:
        raise DeploymentError("Uploaded JAR checksum does not match the tested artifact.")
    try:
        with zipfile.ZipFile(path) as archive:
            manifest = archive.getinfo("META-INF/MANIFEST.MF")
            if manifest.file_size > 65536:
                raise DeploymentError("JAR manifest is unexpectedly large.")
            content = archive.read(manifest)
            if b"Main-Class:" not in content or b"Start-Class:" not in content:
                raise DeploymentError("Expected an executable Spring Boot JAR.")
    except (zipfile.BadZipFile, KeyError) as error:
        raise DeploymentError("Artifact is not a Spring Boot JAR.") from error


def set_jar_permissions(path):
    os.chown(path, 0, grp.getgrnam("education").gr_gid)
    path.chmod(0o640)


def restart_service():
    subprocess.run(
        ["/usr/bin/systemctl", "restart", SERVICE],
        check=True,
        timeout=90,
        env=COMMAND_ENV,
    )


def wait_for_health():
    deadline = time.monotonic() + HEALTH_TIMEOUT
    client = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    while time.monotonic() < deadline:
        try:
            active = subprocess.run(
                ["/usr/bin/systemctl", "is-active", "--quiet", SERVICE],
                timeout=5,
                env=COMMAND_ENV,
            )
            if active.returncode == 0:
                with client.open(HEALTH_URL, timeout=5) as response:
                    if response.status == 200 and json.load(response).get("status") == "UP":
                        return True
        except (OSError, ValueError, AttributeError, subprocess.TimeoutExpired):
            pass
        time.sleep(3)
    return False


def promote(commit, checksum):
    current = APP_DIR / "app.jar"
    previous = APP_DIR / "previous.jar"
    require_root_owned(APP_DIR, directory=True)
    require_root_owned(current)
    with tempfile.TemporaryDirectory(prefix=".deploy-", dir=APP_DIR) as directory:
        temporary = Path(directory)
        candidate = temporary / "candidate.jar"
        backup = temporary / "backup.jar"
        copy_candidate(commit, candidate)
        verify_candidate(candidate, checksum)
        set_jar_permissions(candidate)
        shutil.copy2(current, backup)
        set_jar_permissions(backup)
        os.replace(backup, previous)
        try:
            os.replace(candidate, current)
            restart_service()
            if not wait_for_health():
                raise DeploymentError("New application did not become healthy within 180 seconds.")
        except Exception as failure:
            for signum in (signal.SIGHUP, signal.SIGINT, signal.SIGTERM):
                signal.signal(signum, signal.SIG_IGN)
            try:
                shutil.copy2(previous, backup)
                set_jar_permissions(backup)
                os.replace(backup, current)
                restart_service()
                recovered = wait_for_health()
            except Exception as recovery_error:
                raise DeploymentError(
                    f"Deployment failed ({failure}); JAR recovery also failed ({recovery_error}). "
                    "Inspect the service journal. Database migrations were not reverted."
                ) from recovery_error
            if not recovered:
                raise DeploymentError(
                    f"Deployment failed ({failure}). Previous JAR was restored but is unhealthy. "
                    "Inspect the service journal. Database migrations were not reverted."
                ) from failure
            raise DeploymentError(
                f"Deployment failed ({failure}). Previous JAR is running again. "
                "Database migrations were not reverted."
            ) from failure
    print(f"Deployed commit {commit}; SHA-256 {checksum}; health UP.", flush=True)


def interrupted(signum, frame):
    raise DeploymentError(f"Deployment interrupted by signal {signum}.")


def main():
    try:
        commit, checksum = validate_arguments(sys.argv[1:])
        if os.geteuid() != 0:
            raise DeploymentError("Run this command through the configured sudo rule.")
        os.umask(0o077)
        for signum in (signal.SIGHUP, signal.SIGINT, signal.SIGTERM):
            signal.signal(signum, interrupted)
        with LOCK_FILE.open("a") as lock:
            try:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError as error:
                raise DeploymentError("Another deployment is already running.") from error
            promote(commit, checksum)
    except Exception as error:
        print(f"Deployment error: {error}", file=sys.stderr, flush=True)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
