# ODA Login Theme for Keycloak

A modern, clean login theme for self-hosted [Keycloak](https://www.keycloak.org/).
Extends the built-in `keycloak` theme, so registration, password reset, OTP and
other auth pages inherit the same look. All branding and connection values are
configurable via environment variables.

## Features

- Modern, responsive design (mobile-first, WCAG AA contrast, visible focus states)
- Inter font, OKLCH color tokens, no Bootstrap blue
- Password visibility toggle
- Social / identity-provider login buttons
- Configurable footer links (home, privacy, terms, support)
- Brand name, tagline, logo and colors configurable via env vars
- Works with Keycloak 26.x (custom `footer.ftl`, `stylesCommon`)

## Theme structure

```
themes/oda/login/
├── theme.properties          # parent theme, kc* class mapping, env-configurable branding
├── template.ftl              # HTML skeleton (logo header, card, footer)
├── login.ftl                 # login form
├── footer.ftl                # configurable footer links
├── messages/
│   └── messages_en.properties
└── resources/
    ├── css/styles.css        # design system (CSS custom properties)
    ├── js/login.js           # password visibility toggle
    └── img/logo.svg          # default logo
```

## Quick start (Docker Compose)

```bash
cp env.example .env
# edit .env to set your branding, database and admin credentials
docker compose up -d
```

Then open the Admin Console (`http://localhost:8080`), go to
**Realm Settings → Themes → Login Theme**, select **`oda`** and save.

## Configuration

Copy `env.example` to `.env` and adjust. All variables are optional — sensible
defaults are built into the theme.

### Theme branding

| Variable | Default | Description |
|----------|---------|-------------|
| `ODA_BRAND_NAME` | `ODA` | Brand name in header and footer |
| `ODA_BRAND_TAGLINE` | *(empty)* | Short tagline under the logo |
| `ODA_LOGO_URL` | *(bundled logo.svg)* | Absolute URL to a custom logo |
| `ODA_SUPPORT_EMAIL` | *(empty)* | Support email in footer (link hidden when empty) |
| `ODA_PRIVACY_URL` | *(empty)* | Privacy policy link in footer |
| `ODA_TERMS_URL` | *(empty)* | Terms of service link in footer |
| `ODA_HOME_URL` | *(empty)* | Home page link in footer |
| `ODA_PRIMARY_COLOR` | `oklch(0.5635 0.2408 260.8178)` | Primary brand color (any CSS color) |
| `ODA_PRIMARY_FOREGROUND` | `#ffffff` | Text color on primary buttons |
| `ODA_ACCENT_COLOR` | `oklch(0.6489 0.2370 26.9728)` | Accent color for background decoration |

### Keycloak server

| Variable | Default | Description |
|----------|---------|-------------|
| `KC_HOSTNAME` | `localhost` | Public hostname clients use to reach Keycloak |
| `KC_PROXY_HEADERS` | `forwarded` | Proxy headers mode (`none`/`forwarded`/`xforwarded`) |
| `KC_HTTP_PORT` / `KC_HTTPS_PORT` | `8080` / `8443` | Ports exposed by the container |
| `KC_DB` | `postgres` | Database vendor |
| `KC_DB_URL` | `jdbc:postgresql://postgres:5432/keycloak` | JDBC connection URL |
| `KC_DB_USERNAME` / `KC_DB_PASSWORD` | `keycloak` / `change_me` | Database credentials |
| `KC_BOOTSTRAP_ADMIN_USERNAME` / `KC_BOOTSTRAP_ADMIN_PASSWORD` | `admin` / `change_me` | Initial admin user |

> **Security**: change `change_me` values before deploying. Never commit the real
> `.env` file.

## Deployment options

### 1. Docker volume mount (used by docker-compose.yml)

```bash
docker run --name keycloak -p 8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=change_me \
  -e ODA_BRAND_NAME=ODA \
  -v "$PWD/themes/oda:/opt/keycloak/themes/oda" \
  quay.io/keycloak/keycloak:26.5 \
  start-dev
```

### 2. Copy into the server

```bash
cp -r themes/oda /opt/keycloak/themes/
```

## SELinux note (Fedora / RHEL family with podman)

If Keycloak returns a 500 with `Template not found for name "login.ftl"` in the
logs and you are on an SELinux-enforcing host, the bind-mounted theme directory
is probably not readable by the container. The mount keeps a host label (e.g.
`user_home_t`) that the container's `container_t` domain cannot read.

Fix: add `:Z` to the volume mount in `docker-compose.yml`:

```yaml
    volumes:
      - ./themes/oda:/opt/keycloak/themes/oda:Z
```

then recreate the stack (`podman-compose down && podman-compose up -d`). The
`:Z` relabels the mount to `container_file_t`; it is ignored on non-SELinux
systems.

### 3. Bake into a custom image

```dockerfile
FROM quay.io/keycloak/keycloak:26.5 AS builder
COPY --chown=keycloak:keycloak themes/oda /opt/keycloak/themes/oda
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.5
COPY --from=builder /opt/keycloak/ /opt/keycloak/
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
```

### 4. Prebuilt image from GitHub Container Registry

Pushes to this repo build `./Dockerfile` and publish it to
`ghcr.io/opendonationassistant/oda-login-page`. Tags are the GitHub Actions run
number, plus `latest` for the default branch:

```bash
docker run -p 8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=change_me \
  ghcr.io/opendonationassistant/oda-login-page:latest start-dev
```

The package is intended to be public. If the first publish leaves it private,
flip **Packages → oda-login-page → Settings → Change visibility** once, or set
the org default package visibility to public.

## Development

Disable theme caching while iterating on templates/CSS:

```bash
bin/kc.sh start-dev \
  --spi-theme--static-max-age=-1 \
  --spi-theme--cache-themes=false \
  --spi-theme--cache-templates=false
```

## Customization

- **Colors**: set `ODA_PRIMARY_COLOR` / `ODA_ACCENT_COLOR` env vars, or edit the
  `:root` block in `resources/css/styles.css`.
- **Logo**: set `ODA_LOGO_URL` to an absolute URL, or replace
  `resources/img/logo.svg`.
- **Footer links**: set `ODA_HOME_URL`, `ODA_PRIVACY_URL`, `ODA_TERMS_URL`,
  `ODA_SUPPORT_EMAIL`.
- **Messages**: add/override keys in `messages/messages_en.properties` (or add
  `messages_<locale>.properties` for other languages).
- **Class mapping**: the `kc*` properties in `theme.properties` map Keycloak's
  template classes to the `oda-*` classes styled in `styles.css`.

## Notes

- The theme is mounted read-only into the container; the container runs as user
  `keycloak` (UID 1000), so ensure mounted files are readable (`chmod -R a+rX`).
- Templates may need small updates when upgrading Keycloak across major versions.
- For production, terminate TLS at a reverse proxy and run with `start` instead
  of `start-dev`.