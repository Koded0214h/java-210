# Deploying to kodedlabs

One nginx host on port 80: the landing page at `/`, and each of the 4 Spring
Boot apps reverse-proxied behind its own path prefix (`/serp-insights/`,
`/vehicle-tracker/`, `/ayo/`, `/dead-or-wounded/`). See `nginx.conf` at the
repo root for the actual config — this doc is the steps to get four apps
running persistently and nginx pointed at them.

## 1. Prerequisites on the VPS

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven nginx git
git clone <this-repo-url> java210
cd java210
```

## 2. Build all four jars

```bash
(cd serp-insights && mvn -q package -DskipTests)
(cd vehicle-tracker/web && mvn -q package -DskipTests)
(cd ayo/web && mvn -q package -DskipTests)
(cd dead-or-wounded/web && mvn -q package -DskipTests)
```

Each produces a `target/*.jar`. Note the exact jar filename per project —
Maven includes the version (`1.0.0`) in the name.

## 3. Run each app as a systemd service

Ports: `8081` serp-insights, `8082` vehicle-tracker, `8083` ayo,
`8084` dead-or-wounded — these must match `nginx.conf`.

Create `/etc/systemd/system/java210-serp-insights.service`:

```ini
[Unit]
Description=Java210 - serp-insights
After=network.target

[Service]
WorkingDirectory=/home/<user>/java210/serp-insights
ExecStart=/usr/bin/java -jar target/serp-insights-1.0.0.jar --server.port=8081
Restart=on-failure
User=<user>

[Install]
WantedBy=multi-user.target
```

Repeat for the other three, changing `WorkingDirectory`, the jar filename,
the port, and the service name:

- `java210-vehicle-tracker.service` → `vehicle-tracker/web`, port `8082`
- `java210-ayo.service` → `ayo/web`, port `8083`, and set
  `Environment=GEMINI_API_KEY=your-key` under `[Service]` if you want to
  override the key baked into `application.properties`
- `java210-dead-or-wounded.service` → `dead-or-wounded/web`, port `8084`

Then:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now java210-serp-insights java210-vehicle-tracker java210-ayo java210-dead-or-wounded
sudo systemctl status java210-ayo   # spot-check one
```

## 4. Point nginx at it

```bash
sudo cp nginx.conf /etc/nginx/sites-available/java210
sudo ln -s /etc/nginx/sites-available/java210 /etc/nginx/sites-enabled/java210
sudo rm -f /etc/nginx/sites-enabled/default
sudo mkdir -p /var/www/java210
sudo cp -r landing/* /var/www/java210/
sudo nginx -t && sudo systemctl reload nginx
```

## 5. Firewall

Only port 80 (and 22 for SSH) needs to be open publicly — the four app
ports (8081–8084) are proxied internally via `127.0.0.1`, not exposed
directly:

```bash
sudo ufw allow 22
sudo ufw allow 80
sudo ufw enable
```

## 6. Verify

```bash
curl -I http://<your-ip>/                       # landing page
curl -I http://<your-ip>/ayo/                   # each app
curl -X POST http://<your-ip>/ayo/api/game/new
```

## Updating after a git pull

```bash
cd java210 && git pull
(cd <project> && mvn -q package -DskipTests)
sudo systemctl restart java210-<project>
```

No nginx reload needed for app-only changes — only if `nginx.conf` or the
landing page itself changes.
