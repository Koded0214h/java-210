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

Domain: **`java-210.kodedlabs.com`** — point an A record at this box's IP;
`nginx.conf`'s `server_name` already expects that host (won't collide with
the existing `somba` site, which uses `somba.ddns.net` on 443).

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
sudo nginx -t && sudo systemctl reload nginx

sudo ufw allow 22
sudo ufw allow 80
sudo ufw enable
```

(`sites-enabled/default` isn't currently active on this box, so there's
nothing to remove — only `somba` is enabled alongside this.)

## 4. Verify

```bash
curl -I http://java-210.kodedlabs.com/
curl -I http://java-210.kodedlabs.com/ayo/
curl -X POST http://java-210.kodedlabs.com/ayo/api/game/new
```

Or by IP before DNS propagates: `curl -H "Host: java-210.kodedlabs.com" http://<ip>/`

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
