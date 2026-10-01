# Deploying the education system

For a frontend developer to access the API over the internet, continue with
the [public HTTPS API guide](PUBLIC_API.md) after completing this deployment setup.

This guide continues the manual Ubuntu 24.04 setup: Java 21, PostgreSQL 17,
the `education` service account, a working `education-system.service`, and
`/opt/education-system/app.jar` are already installed. The service reads
`/etc/education-system/app.env` and listens on `127.0.0.1:8080`.

The `deploy` SSH account, its dedicated key, and these repository Actions
secrets must also exist:

| Secret | Value |
| --- | --- |
| `DEPLOY_HOST` | Server IPv4 address or hostname, without a username or URL scheme |
| `DEPLOY_USER` | `deploy` |
| `DEPLOY_SSH_KEY` | Complete, unencrypted OpenSSH private key for this deployment account |
| `DEPLOY_KNOWN_HOSTS` | A verified known-hosts line for the same host, obtained through the existing trusted server connection |

Keep the private key in GitHub Actions Secrets. The deployment workflow uses
strict SSH host verification. Database credentials and the application's JWT
key remain in the server's existing environment file.

## One-time server installation

From the repository directory on your Mac, copy the two reviewed installation
files using your existing administrator SSH connection:

```bash
scp deploy/education-deploy.py deploy/education-deploy.sudoers root@85.198.54.245:/root/
```

If your administrator SSH connection uses a named identity file, add
`-i ~/.ssh/education_ci_lab` to `scp`. The deployment key is for the `deploy`
account; use your administrator credentials for this installation.

On the Ubuntu server, as `root`, run these commands in order. Stop if a command
fails.

```bash
apt install -y python3 sudo
```

```bash
install -d -o deploy -g deploy -m 750 /home/deploy/incoming
chown root:education /opt/education-system /opt/education-system/app.jar
chmod 750 /opt/education-system
chmod 640 /opt/education-system/app.jar
```

Validate the sudo rule before installing it:

```bash
visudo -cf /root/education-deploy.sudoers
```

Then install the helper and rule:

```bash
install -o root -g root -m 755 /root/education-deploy.py /usr/local/sbin/education-deploy
install -o root -g root -m 440 /root/education-deploy.sudoers /etc/sudoers.d/education-deploy
visudo -c
```

The helper needs Python 3.11 or newer; Ubuntu 24.04 supplies Python 3.12.
Its isolated Python interpreter ignores user-controlled Python module paths.
The helper accepts exactly two arguments: a 40-character commit SHA and a
64-character SHA-256 checksum. Its application, service, upload directory, and
health endpoint are fixed in the root-owned file.

The `deploy` account receives passwordless sudo permission for this helper
only. Do not add it to the `sudo` group or give it write access to the helper,
systemd unit, application directory, or environment file. It uploads files
to its own incoming directory; the helper reads those files with `deploy`
permissions before validating them.

From the Mac, check the installed permission:

```bash
ssh -T -i ~/.ssh/education_ci_deploy -o IdentitiesOnly=yes -o BatchMode=yes deploy@85.198.54.245 'sudo -n -l'
```

Expect the allowed command `/usr/local/sbin/education-deploy`. This check does
not restart the application.

## How a release runs

The workflow is [ci.yml](../.github/workflows/ci.yml).

- Pushes and pull requests targeting `main` build and test the project.
- Pushing a tag matching `v*` builds and tests the tagged commit, then deploys
  the resulting artifact. The tagged commit must be reachable from `main`.
- A manual workflow run performs build and tests only.
- Failed build or test jobs prevent deployment.
- Deployments are serialized. Ordinary new commits do not cancel an active
  release deployment. Avoid pushing several release tags at once: GitHub
  concurrency retains at most one pending job and is not a FIFO release queue.

The deployment job downloads the artifact from the same workflow run,
computes its SHA-256, uploads it using SSH/SFTP on port 22, and calls the
installed helper. Only the administrator-installed helper runs with elevated
privileges; new workflow versions cannot replace it automatically.

The helper:

1. Acquires a server-side lock to reject overlapping deployments.
2. Copies at most 256 MiB from the upload, using the `deploy` account's read
   permissions and a bounded timeout.
3. Verifies the checksum and executable Spring Boot manifest.
4. Saves the current JAR as `/opt/education-system/previous.jar`.
5. Atomically replaces `app.jar` and restarts `education-system.service`.
6. Waits up to approximately three minutes for an active service and an HTTP
   200 response whose top-level JSON status is `UP`.
7. If startup fails, attempts to restore and restart the previous JAR. The
   workflow remains failed even when this recovery succeeds.

The deployment restarts a single application instance, so there is a short
interruption. It does not provide zero-downtime deployment.

Recovery restores the application JAR only. **Flyway migrations are not
reverted.** Keep migrations compatible with the previous application version
when relying on JAR recovery; plan a database backup and a separate recovery
procedure for destructive schema changes. A successful health check is a
startup check, not a complete test of the business endpoints.

The previous manual setup includes a development administrator and MD5
password storage. Keep the API on loopback during this exercise and address
that authentication setup before public use.

## Publish the first automated release

After installing the helper and checking the SSH permission, commit the
workflow, deployment files, tests, and documentation from the Mac:

```bash
git add .github/workflows/ci.yml deploy README.md
git diff --cached --stat
git commit -m "ci: deploy tested release tags to Ubuntu"
git push origin main
```

Review the branch CI result in GitHub Actions. Then tag the intended commit:

```bash
git tag -a v1.0.0 -m "First automated deployment"
git push origin v1.0.0
```

Use a new version name if that tag already exists. Do not move an existing
release tag. Tagging an older commit is possible with `git tag -a VERSION SHA`,
but that commit must contain this workflow and be reachable from `main`.

In GitHub Actions, the release run has `Build and test` followed by
`Deploy release`. Successful deployment logs include the commit SHA,
artifact checksum, and `health UP`.

On the server, inspect the service with:

```bash
systemctl status education-system --no-pager
journalctl -u education-system -n 100 --no-pager
curl --fail-with-body -sS http://127.0.0.1:8080/actuator/health
```

## SSH and deployment troubleshooting

| Failure | Check |
| --- | --- |
| Connection timeout | Port 22 must be reachable from the GitHub-hosted runner. A cloud firewall rule permitting only your home IP will block it. Determine the network access policy before changing firewall rules. |
| Host key verification failed | `DEPLOY_KNOWN_HOSTS` must match the exact host and its current verified key. |
| Permission denied (publickey) | Check the dedicated key, `deploy` authorized keys, and SSH file ownership and permissions. |
| sudo password or permission error | Check the installed sudoers rule with `visudo -c` and `sudo -l -U deploy` as root. |
| Release is not on main | The tag points to a commit outside `origin/main`. |
| Checksum or JAR validation error | The upload did not match the tested artifact, or the artifact was not an executable Spring Boot JAR. The running app has not been replaced. |
| Deployment failed and previous JAR restored | Inspect the application journal; check configuration and migration compatibility. |

Successful uploads are removed from `/home/deploy/incoming`. Failed uploads
remain there for investigation; remove them once the incident is resolved.
Only the current and previous installed JARs are retained.

## Local verification

The helper tests use temporary files and mock service control, health
responses, and privileged operations. They never connect to the server.

```bash
python3 -B -m unittest discover -s deploy/tests -v
```

They cover checksum and archive rejection, failed uploads, successful
promotion, startup failure recovery, unhealthy recovery, input validation,
installation permissions, upload privilege boundaries, and deployment locks.
The GitHub build job runs these tests before the Maven build.
