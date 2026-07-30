# Deploying to kodedlabs

Status as of the last deploy pass: all 4 apps are built and running on
`vps3435100` (kodedlabs) as **user-level** systemd services — no root was
needed for that part (Java/Maven were installed into `~/tools` since the
box has no passwordless `sudo`). What's left is nginx + the firewall, both
of which genuinely need root — run the commands in step 3 yourself.

- serp-insights → `127.0.0.1:8081`
- vehicle-tracker → `127.0.0.1:8082`
- ayo → `127.0.0.1:8083`
- dead-or-wounded → `127.0.0.1:8084`

Domain: **`java-210.kodedlabs.com`** — point an A record at this box's IP.
`nginx.conf`'s `server_name` already expects that host.

**Port 8080, not 80:** this box already runs a Docker container
(`ship-cli-nginx-1`, an unrelated project) bound to `0.0.0.0:80` — Docker's
port binding intercepts traffic before the host's own nginx package ever
sees it, so the host nginx can't use port 80 at all here. Rather than touch
that other project's live container, java-210 runs on **8080** instead:
`http://java-210.kodedlabs.com:8080/`. The host nginx package was also
found `inactive`/`disabled` (never started) — step 3 enables it.

## 1. Toolchain (already done)

Java 17 (Temurin) and Maven were installed with no root, as tarballs under
`~/tools`, added to `PATH`/`JAVA_HOME` in `~/.bashrc`:

```bash
export JAVA_HOME="$HOME/tools/jdk-17.0.20+8"
export PATH="$JAVA_HOME/bin:$HOME/tools/apache-maven-3.9.9/bin:$PATH"
```

## 2. Services (already done)

Each app is a **user** systemd unit at `~/.config/systemd/user/java210-*.service`
(not `/etc/systemd/system/`, so no root needed):

```bash
systemctl --user status java210-serp-insights java210-vehicle-tracker java210-ayo java210-dead-or-wounded
```

`ayo` and `vehicle-tracker` have `Environment=GEMINI_API_KEY=...` /
`Environment=MAPBOX_TOKEN=...` baked into their unit files directly (not in
git — unit files are local-only). Rotate either key by editing the relevant
`~/.config/systemd/user/java210-*.service` file and running:

```bash
systemctl --user daemon-reload && systemctl --user restart java210-ayo
```

**Known gap:** `loginctl show-user kodedlabs -p Linger` is `no`, meaning
these services stop once every session for this user closes — they'll
survive normal SSH disconnects as long as *some* session stays open, but
not a full logout/reboot. Fix (needs root, one-time):

```bash
sudo loginctl enable-linger kodedlabs
```

## 3. nginx + firewall (needs root — run these yourself)

```bash
cd ~/java-210
sudo cp nginx.conf /etc/nginx/sites-available/java210
sudo ln -s /etc/nginx/sites-available/java210 /etc/nginx/sites-enabled/java210
sudo mkdir -p /var/www/java210
sudo cp -r landing/* /var/www/java210/
sudo nginx -t
sudo systemctl enable --now nginx   # was inactive+disabled — this starts it, not just reloads

sudo ufw allow 22
sudo ufw allow 8080
sudo ufw enable
```

If nginx was already running when you did this (e.g. you re-run this after
an update), use `sudo systemctl reload nginx` instead of `enable --now`.

(`sites-enabled/default` isn't currently active on this box, so there's
nothing to remove — only `somba` is enabled alongside this, on a different
port/domain, no conflict.)

## 4. Verify

```bash
curl -I http://java-210.kodedlabs.com:8080/
curl -I http://java-210.kodedlabs.com:8080/ayo/
curl -X POST http://java-210.kodedlabs.com:8080/ayo/api/game/new
```

Or by IP before DNS propagates: `curl -H "Host: java-210.kodedlabs.com" http://<ip>:8080/`

## 5. HTTPS (Let's Encrypt)

certbot (with the nginx plugin) is already installed on this box. The catch:
Let's Encrypt's HTTP-01 challenge always hits **port 80**, which belongs to
the `ship-cli` Docker container, not our host nginx on 8080. Fix: one
additive location block was added to `~/ship/ship-cli/nginx.conf` (backed
up as `nginx.conf.bak-java210` alongside it) that redirects just
`/.well-known/acme-challenge/` to our host's port 8080 — nothing else about
that container changed, and it doesn't need touching again after this.

```bash
docker exec ship-cli-nginx-1 nginx -t
docker exec ship-cli-nginx-1 nginx -s reload
```

### Phase 1 — get the certificate (needs root)

```bash
cd ~/java-210
git pull
sudo cp nginx.conf /etc/nginx/sites-available/java210
sudo mkdir -p /var/www/certbot
sudo nginx -t && sudo systemctl reload nginx

sudo certbot certonly --webroot -w /var/www/certbot \
  -d java-210.kodedlabs.com \
  --register-unsafely-without-email --agree-tos --non-interactive
```

Verify: `sudo certbot certificates` should list `java-210.kodedlabs.com`,
cert files under `/etc/letsencrypt/live/java-210.kodedlabs.com/`.

### Phase 2 — add the HTTPS server block (needs root)

Once the cert exists, add a `listen 443 ssl` server block to
`/etc/nginx/sites-available/java210` (same `location` blocks as the 8080
one, plus `ssl_certificate`/`ssl_certificate_key` pointing at the paths
above), and change the 8080 block to redirect everything except the
challenge path to HTTPS. This repo's `nginx.conf` will be updated with that
full config once the cert exists — `git pull`, re-copy, `nginx -t`, reload.

```bash
sudo ufw allow 443
sudo nginx -t && sudo systemctl reload nginx
```

Auto-renewal: `certbot` on Debian/Ubuntu installs a systemd timer
(`systemctl list-timers | grep certbot`) that renews automatically — the
webroot path stays valid indefinitely since the ship-cli redirect is
permanent, so no manual steps going forward.

## Updating after a git pull

```bash
cd ~/java-210 && git pull
source ~/.bashrc   # picks up JAVA_HOME/PATH if this is a fresh shell
(cd <project-dir> && mvn -q package -DskipTests)
systemctl --user restart java210-<name>
```

No nginx reload needed for app-only changes — only if `nginx.conf` or the
landing page itself changes, then repeat the relevant `cp`/`reload` from
step 3.
