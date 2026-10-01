# Public API for frontend development

Target API: `https://api.mohsendev20.ir/api/v1`.
Frontend origin: `http://localhost:5173`.
Ubuntu server: `85.198.54.245`.

The files in `deploy/nginx` are installation files, not an indication that
HTTPS is already running. Complete the steps below in order. Stop if a command
fails. The existing Java service stays on `127.0.0.1:8080`; PostgreSQL stays
on loopback. Nginx forwards `/api/` and the Swagger documentation routes to Java. Actuator remains
available through the administrator's SSH session.

## 1. DNS and firewall

In the existing DNS provider's panel, create this record:

The apex domain already hosts a website. Keep its existing nameservers,
website records, and email records. Start with the existing hosting control
panel's Zone Editor or DNS Management page. The registrar's Change DNS and
Child NS forms manage nameservers, not ordinary A records. If the hosting
panel cannot edit the authoritative DNS zone, ask the current provider to add
the API record; creating a record in an unrelated DNS zone will not publish it.

| Type | Name | Value | TTL |
| --- | --- | --- | --- |
| A | `api` | `85.198.54.245` | 300 seconds, or the panel default |

Some providers require the full name `api.mohsendev20.ir`. Use DNS-only mode
if the provider offers a proxy option. Do not change the domain's nameservers
or its other records. Do not add an AAAA record for this IPv4-only setup.

In the server provider's firewall, allow inbound TCP ports 80 and 443 from
`0.0.0.0/0`. Port 80 is needed for certificate validation and renewal; API
traffic will use HTTPS on 443. Keep the existing SSH rule. Do not open ports
8080 or 5432.

On the Mac, verify that this returns `85.198.54.245` before requesting a
certificate:

```bash
dig +short A api.mohsendev20.ir
```

## 2. Release the CORS change

The application's `CORS_ALLOWED_ORIGINS` setting defaults to
`http://localhost:5173`. The CORS filter handles approved browser preflight
requests before JWT authentication. Actual API requests still require their
normal authentication and permissions.

Commit the changed application code, tests, and deployment documentation,
push to `main`, and wait for CI. Then publish a new release tag such as
`v1.0.2`, provided that tag is unused. Wait for the deployment to succeed.
An older release does not contain the CORS fix.

For additional frontend addresses, set a comma-separated value in the
server's existing `/etc/education-system/app.env`, then restart the service:

```text
CORS_ALLOWED_ORIGINS=http://localhost:5173,https://your-frontend.example
```

An origin includes its scheme, hostname, and port, but no path or trailing
slash. `http://127.0.0.1:5173` and `http://localhost:5173` are different
origins. Use explicit origins. An empty value disables cross-origin access.
CORS is a browser rule, not a replacement for authentication or a restriction
on tools such as curl.

## 3. Copy the Nginx files from the Mac

Run from the repository root on the Mac, using the administrator key:

```bash
scp -i ~/.ssh/education_ci_lab -o IdentitiesOnly=yes \
  deploy/nginx/education-api-bootstrap.conf \
  deploy/nginx/education-api.conf \
  deploy/nginx/reload-education-nginx \
  root@85.198.54.245:/root/
```

Then connect:

```bash
ssh -i ~/.ssh/education_ci_lab -o IdentitiesOnly=yes root@85.198.54.245
```

The remaining installation commands run on Ubuntu as `root`, not on the Mac.

## 4. Install the certificate-validation site

```bash
apt update
apt install -y nginx snapd
install -d -o root -g root -m 755 /var/www/letsencrypt
install -o root -g root -m 644 /root/education-api-bootstrap.conf /etc/nginx/sites-available/education-api
ln -s /etc/nginx/sites-available/education-api /etc/nginx/sites-enabled/education-api
nginx -t
systemctl enable --now nginx
systemctl reload nginx
```

The `ln` command is for first installation. If the link already exists,
verify that it points at this file before proceeding. The bootstrap site
serves certificate challenges only; requests to the API return 404 at this
stage. It does not expose application credentials over plain HTTP.

Check the host firewall separately from the provider firewall:

```bash
ufw status
```

If UFW is already active, allow HTTP and HTTPS:

```bash
ufw allow 80/tcp
ufw allow 443/tcp
```

Do not enable or reset UFW as part of this step. If it is absent or inactive,
the provider firewall rules still apply.

## 5. Issue the HTTPS certificate

Install the official Certbot snap if it is not already installed:

```bash
snap install --classic certbot
/snap/bin/certbot --version
```

After public DNS resolves correctly and port 80 is reachable, request a
certificate. Certbot will ask for an email address and agreement to its
certificate authority's terms:

```bash
/snap/bin/certbot certonly --webroot \
  --webroot-path /var/www/letsencrypt \
  --cert-name education-api \
  -d api.mohsendev20.ir
```

The certificate files must exist before installing the HTTPS site:

```bash
test -s /etc/letsencrypt/live/education-api/fullchain.pem
test -s /etc/letsencrypt/live/education-api/privkey.pem
```

Do not bypass browser certificate verification or use curl's `-k` option to
hide issuance or trust errors.

## 6. Replace the development administrator password

The repository seeds a publicly documented administrator password. Before
enabling the public API, replace it with a unique password that you choose
privately. If it has already been replaced, skip this step.

This command updates only the seeded account `9999999999` in `university_db`.
It reads the new password without echoing it or placing it in shell history.
It preserves the current application's legacy MD5 format; this is a test
environment and password hashing still needs to be upgraded before real use.

```bash
python3 - <<'PY'
import getpass
import hashlib
import subprocess

password = getpass.getpass("New test administrator password (at least 20 characters): ")
if len(password) < 20:
    raise SystemExit("Password is too short; no changes made.")
if password != getpass.getpass("Repeat password: "):
    raise SystemExit("Passwords differ; no changes made.")
digest = hashlib.md5(password.encode("utf-8")).hexdigest()
sql = (
    "UPDATE users SET password_hash = '" + digest + "', "
    "updated_at = CURRENT_TIMESTAMP, version = version + 1 "
    "WHERE university_number = '9999999999' RETURNING university_number;"
)
result = subprocess.run(
    ["runuser", "-u", "postgres", "--", "psql", "-X", "-q", "-t", "-A",
     "-v", "ON_ERROR_STOP=1", "-d", "university_db"],
    input=sql, text=True, capture_output=True, check=True,
)
if result.stdout.strip() != "9999999999":
    raise SystemExit("The seeded account was not found; inspect the database before continuing.")
print("Test administrator password updated.")
PY
```

Use fictional student data and test-only passwords. Give the frontend
developer application test credentials appropriate to the screens being
built. They do not need the server's SSH keys, database password, or JWT
signing secret. Password changes do not revoke previously issued JWTs.

## 7. Enable the public API and automatic renewal

After the CORS release, certificate issuance, and password change:

```bash
install -o root -g root -m 644 /root/education-api.conf /etc/nginx/sites-available/education-api
nginx -t
systemctl reload nginx
install -d -o root -g root -m 755 /etc/letsencrypt/renewal-hooks/deploy
install -o root -g root -m 755 /root/reload-education-nginx /etc/letsencrypt/renewal-hooks/deploy/education-nginx
/snap/bin/certbot renew --dry-run --run-deploy-hooks
systemctl list-timers --all 'snap.certbot.renew*'
```

The snap schedules renewals. The hook validates and reloads Nginx after a
successful renewal so it serves the new certificate. Keep the challenge
location on port 80 reachable.

## 8. Verify from the Mac

An authenticated route without a token should return HTTP 401 with the
application's JSON error. This confirms the public route reaches Java:

```bash
curl --max-time 15 -i https://api.mohsendev20.ir/api/v1/courses
```

Check the browser preflight:

```bash
curl --max-time 15 -i -X OPTIONS https://api.mohsendev20.ir/api/v1/courses \
  -H 'Origin: http://localhost:5173' \
  -H 'Access-Control-Request-Method: GET' \
  -H 'Access-Control-Request-Headers: authorization'
```

Expect HTTP 200 and `Access-Control-Allow-Origin: http://localhost:5173`.
A request to `https://api.mohsendev20.ir/actuator/health` should return 404;
the existing loopback health check used by deployment remains unchanged.

## Frontend connection

For a Vite frontend, add this to its local environment file and restart its
development server:

```text
VITE_API_BASE_URL=https://api.mohsendev20.ir/api/v1
```

The base URL is public configuration. Do not put server secrets in `VITE_*`
variables, because Vite exposes them to the browser.

Example login using credentials entered by the developer:

```javascript
const baseUrl = import.meta.env.VITE_API_BASE_URL;

const response = await fetch(`${baseUrl}/auth/login`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ universityNumber, password }),
});
const result = await response.json();
if (!response.ok) throw new Error(result.message);
const token = result.data.token;
```

Send this token for protected endpoints:

```javascript
const response = await fetch(`${baseUrl}/majors?page=0&size=20`, {
  headers: { Authorization: `Bearer ${token}` },
});
const result = await response.json();
if (!response.ok) throw new Error(result.message);
```

These requests use a bearer token, not browser cookies; `credentials:
"include"` and `mode: "no-cors"` are unnecessary. A 401 requires a valid
login/token; a 403 can indicate missing endpoint permission. Student accounts
use enrollment-specific offering endpoints, not the general course catalog.
See the main README for the complete endpoint and permission tables.

## Sources

- [Spring Framework CORS](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html)
- [NGINX reverse proxy](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/)
- [Certbot installation and renewal](https://certbot.eff.org/instructions?ws=nginx&os=snap)
- [Vite environment variables](https://vite.dev/guide/env-and-mode)

## Swagger UI

After releasing the Swagger-enabled application, install the updated Nginx site
once. Regular JAR deployments do not install files under `deploy/nginx`.

On the Mac:

```bash
cd /Users/mohsendev/IdeaProjects/education-system
scp -i ~/.ssh/education_ci_lab -o IdentitiesOnly=yes \
  deploy/nginx/education-api.conf root@85.198.54.245:/root/
ssh -i ~/.ssh/education_ci_lab -o IdentitiesOnly=yes root@85.198.54.245
```

On Ubuntu as root:

```bash
install -o root -g root -m 644 /root/education-api.conf /etc/nginx/sites-available/education-api
nginx -t && systemctl reload nginx
```

From the Mac, verify:

```bash
curl --fail-with-body -sS https://api.mohsendev20.ir/v3/api-docs -o /tmp/education-openapi.json
curl -I https://api.mohsendev20.ir/swagger-ui/index.html
```

Open `https://api.mohsendev20.ir/swagger-ui/index.html`. Use the login endpoint
with your application credentials, then enter `data.token` in **Authorize**
without the `Bearer` prefix. **Try it out** sends real requests to this server.
The relative OpenAPI server URL `/` preserves the browser's HTTPS origin.
The documentation routes are public; protected APIs still require JWT and
permissions. `/actuator/health` remains inaccessible through Nginx.
If the UI returns 404, check both the deployed application version and the
installed Nginx configuration. If API calls return 401, obtain a fresh token.

### HTTPS proxy and CORS

The application uses `server.forward-headers-strategy: native` so Tomcat processes
Nginx's `X-Forwarded-Proto` before the CORS filter. Without this, an HTTPS Swagger
POST can appear cross-origin because the internal Nginx-to-Java connection uses
HTTP, producing `403 Invalid CORS request` before login validation.

For an already deployed JAR, set this in `/etc/education-system/app.env`:

```text
SERVER_FORWARD_HEADERS_STRATEGY=native
```

Then restart `education-system` and check its loopback health endpoint. This
setting takes effect without rebuilding the JAR. Keep Java bound to loopback
and keep Nginx overwriting `Host`, `X-Forwarded-For`, and `X-Forwarded-Proto` as
configured. Same-origin Swagger requests do not need an extra allowed CORS
origin; cross-origin frontend requests still use `CORS_ALLOWED_ORIGINS`.
