FROM quay.io/keycloak/keycloak:26.8.0 AS builder
COPY --chown=keycloak:keycloak themes/oda /opt/keycloak/themes/oda
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.8.0
COPY --from=builder /opt/keycloak/ /opt/keycloak/
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
