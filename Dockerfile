# =============================================================================
# Custom Keycloak image: ODA login theme + STOMP topic scope mapper
# =============================================================================
# Stage 1 builds the protocol mapper JAR. Keycloak libraries are `provided`, so
# the resulting JAR contains only the mapper classes.
# =============================================================================
FROM docker.io/library/maven:3.9-eclipse-temurin-17 AS provider

WORKDIR /build
COPY provider/pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline
COPY provider/src ./src
RUN mvn -B -q package

# =============================================================================
# Stage 2 augments Keycloak: theme + providers, then a build so the provider is
# registered ahead of time.
# =============================================================================
FROM quay.io/keycloak/keycloak:26.8.0 AS builder

COPY --chown=keycloak:keycloak themes/oda /opt/keycloak/themes/oda
COPY --from=provider --chown=keycloak:keycloak \
     /build/target/oda-keycloak-stomp-mapper.jar /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build

# =============================================================================
# Stage 3 runtime image
# =============================================================================
FROM quay.io/keycloak/keycloak:26.8.0

COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
