# ODA Login Theme for Keycloak

A modern, clean login theme for self-hosted [Keycloak](https://www.keycloak.org/), plus a
small protocol mapper that scopes RabbitMQ STOMP topics per user.
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
- `STOMP topic scopes` protocol mapper: injects per-user RabbitMQ OAuth2 scopes
  (`stomp.read:*/amq.topic/<name>.events`) into the access token `scope` claim

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

## Provider structure

```
provider/
├── pom.xml                  # Keycloak 26.8 dependencies (provided), Java 17
└── src/
    ├── main/java/io/github/opendonationassistant/keycloak/stomp/
    │   ├── StompTopicScopeMapper.java   # SPI entry point (protocol mapper)
    │   ├── StompScopeConfig.java        # mapper config -> scope strings
    │   └── ScopeTemplate.java           # ${placeholder} rendering + encoding
    ├── main/resources/META-INF/services/
    │   └── org.keycloak.protocol.ProtocolMapper
    └── test/java/...                    # unit tests for the scope logic
```

## Quick start (Docker Compose)

```bash
cp env.example .env
# edit .env to set your branding, database and admin credentials
docker compose up -d --build
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

## STOMP topic scope mapper

`StompTopicScopeMapper` (provider id `oda-stomp-topic-scopes`) appends RabbitMQ OAuth2
scopes to the access token's `scope` claim. The topic name comes from the user's
`preferred_username` (override it with a user attribute).

For the user `alice`, the default configuration produces:

```json
{
  "preferred_username": "alice",
  "aud": ["account", "stomp"],
  "scope": "openid profile email stomp.read:*/amq.topic/alice.events stomp.write:*/stomp-subscription* stomp.configure:*/stomp-subscription*"
}
```

### How the topic is authorized

RabbitMQ's `rabbitmq_auth_backend_oauth2` reads only the `scope` claim (plus any
`auth_oauth2.additional_scopes_key` entries); a separate claim named `stomp.read` is ignored.
The scope grammar is:

```
<resource_server_id>.<permission>:<vhost>/<exchange>[/<routing_key>]
```

with `permission` one of `configure`, `read`, `write`. STOMP maps `/topic/X` to exchange
`amq.topic` with routing key `X`, so `/topic/alice.events` needs read permission on
`alice.events`:

```
stomp.read:*/amq.topic/alice.events
```

> **Note**: `stomp.read:*/topic/alice.events` is **not** valid — there is no `topic/` segment in
> the grammar, and `topic` would be parsed as the exchange name. The mapper emits
> `<vhost>/amq.topic/<routing key>`.

The two extra scopes are needed because a STOMP `SUBSCRIBE` creates an auto-deleted queue
(`stomp-subscription<guid>`) and binds it to `amq.topic`.

### RabbitMQ prerequisites

```ini
# rabbitmq.conf
auth_backends.1 = rabbit_auth_backend_oauth2
auth_oauth2.resource_server_id = stomp
auth_oauth2.verify_aud = true            # default
```

`resource_server_id` also defines the default scope prefix (`stomp.`). Set
`auth_oauth2.scope_prefix` explicitly only if you need a prefix that differs from the id.

The mapper adds `stomp` to `aud` so the audience check passes. Change `scope.prefix` and
`audience` together if you use a different resource server id.

### Mapper configuration

| Property | Default | Description |
|---|---|---|
| `include.in.access.token` | `true` | Emit the scopes into access tokens. Disable to keep the mapper configured but inactive. |
| `scope.prefix` | `stomp.` | Prefix for every scope. A bare id (`stomp`) gets the trailing dot automatically. |
| `vhost.pattern` | `*` | RabbitMQ vhost pattern (`*` = every vhost). |
| `exchange` | `amq.topic` | Exchange used for STOMP `/topic/...` destinations. |
| `routing.key.template` | `${username}.events` | Topic suffix granted for reading. Placeholders: `${username}`, `${vhost}`, `${exchange}`, `${prefix}`. |
| `extra.scopes` | `write`/`configure` on `stomp-subscription*` | One scope per line; clear the field to disable. |
| `audience` | `stomp` | Added to `aud`. Clear the field to leave the audience untouched. |
| `user.attribute` | *(empty)* | User attribute holding the topic name; empty = `preferred_username`. |

Usernames are percent-encoded for the scope segment (`*` → `%2A`, `/` → `%2F`, `%` → `%25`),
so a username cannot widen its own wildcard permissions. If the username is blank, no scope is
added at all.

### Enabling the mapper (Admin Console)

The mapper is a provider, not a realm setting, so it must be attached once:

1. **Client scopes → Create client scope** (for example `stomp-scopes`), protocol
   `openid-connect`, type **Default**.
2. Open the scope → **Mappers → Configure a new mapper** and pick **STOMP topic scopes**.
3. Keep or adjust the fields above, then **Save**.
4. Assign the scope to the clients that request STOMP tokens: **Clients → <client> →
   Client scopes → Add client scope → `stomp-scopes` (Default)**.
5. Request a token and decode it:

   ```bash
   curl -s -d "client_id=widget" -d "username=alice" -d "password=..." \
     -d "grant_type=password" \
     http://localhost:8080/realms/ODA/protocol/openid-connect/token \
     | jq -r .access_token | cut -d. -f2 | base64 -d 2>/dev/null | jq .scope
   ```

The mapper only writes to access tokens; ID tokens and `/userinfo` are untouched.

## Deployment options

### 1. Docker volume mount (used by docker-compose.yml for the theme)

```bash
docker run --name keycloak -p 8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=change_me \
  -e ODA_BRAND_NAME=ODA \
  -v "$PWD/themes/oda:/opt/keycloak/themes/oda" \
  quay.io/keycloak/keycloak:26.8.0 \
  start-dev
```

> The mapper is a JAR provider and is **not** available with this plain-image command; use the
> custom image (`docker compose up -d --build`) when you need it.

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

`./Dockerfile` does this in three stages:

1. build `provider/` with Maven (Java 17),
2. copy the theme and `oda-keycloak-stomp-mapper.jar` into `/opt/keycloak/providers`,
3. run `kc.sh build` so the provider is registered ahead of time.

```bash
docker build -t oda-login-page:local .
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

Build and test the mapper (produces `provider/target/oda-keycloak-stomp-mapper.jar`):

```bash
mvn -f provider/pom.xml package
```

The provider JAR is baked into the image, so rebuild the container after changing provider
code. The theme is bind-mounted, so template/CSS edits only need a browser refresh.

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
- **Topic scopes**: change the mapper settings in the Admin Console, or edit the
  defaults in `StompScopeConfig.java` and rebuild the image.

## Notes

- The theme is mounted read-only into the container; the container runs as user
  `keycloak` (UID 1000), so ensure mounted files are readable (`chmod -R a+rX`).
- Templates may need small updates when upgrading Keycloak across major versions.
- The mapper is compiled against Keycloak **26.8** internal SPIs
  (`keycloak-server-spi-private`, `keycloak-services`). Bump `keycloak.version` in
  `provider/pom.xml` and rebuild the image on every Keycloak upgrade.
- For production, terminate TLS at a reverse proxy and run with `start` instead
  of `start-dev`.