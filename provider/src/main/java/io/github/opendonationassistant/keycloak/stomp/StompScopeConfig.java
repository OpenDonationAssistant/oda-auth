package io.github.opendonationassistant.keycloak.stomp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mapper configuration resolved from {@code ProtocolMapperModel#getConfig()}.
 *
 * <p>RabbitMQ OAuth2 scopes use the grammar
 * {@code <resource_server_id>.<permission>:<vhost>/<exchange>[/<routing_key>]}, where the
 * permission is {@code configure}, {@code read} or {@code write}. STOMP {@code /topic/X} is
 * published to exchange {@code amq.topic} with routing key {@code X}, so reading
 * {@code /topic/alice.events} requires read permission on routing key {@code alice.events}.
 */
record StompScopeConfig(
  String prefix,
  String vhost,
  String exchange,
  String routingKeyTemplate,
  List<String> extraScopes,
  List<String> audiences,
  String userAttribute
) {
  static final String DEFAULT_PREFIX = "stomp.";
  static final String DEFAULT_VHOST = "*";
  static final String DEFAULT_EXCHANGE = "amq.topic";
  static final String DEFAULT_ROUTING_KEY_TEMPLATE = "${username}.events";
  static final List<String> DEFAULT_EXTRA_SCOPES = List.of(
    "${prefix}write:${vhost}/stomp-subscription*",
    "${prefix}configure:${vhost}/stomp-subscription*"
  );
  static final String DEFAULT_AUDIENCE = "stomp";

  static StompScopeConfig from(Map<String, String> config) {
    Map<String, String> safeConfig = config == null ? Map.of() : config;
    return new StompScopeConfig(
      normalizePrefix(valueOrDefault(safeConfig, "scope.prefix", DEFAULT_PREFIX)),
      valueOrDefault(safeConfig, "vhost.pattern", DEFAULT_VHOST),
      valueOrDefault(safeConfig, "exchange", DEFAULT_EXCHANGE),
      valueOrDefault(safeConfig, "routing.key.template", DEFAULT_ROUTING_KEY_TEMPLATE),
      values(safeConfig.get("extra.scopes")),
      values(safeConfig.get("audience")),
      valueOrDefault(safeConfig, "user.attribute", "")
    );
  }

  List<String> scopesFor(String username) {
    Map<String, String> values = Map.of(
      "username", ScopeTemplate.encodeSegment(username),
      "vhost", vhost,
      "exchange", exchange,
      "prefix", prefix
    );

    Set<String> scopes = new LinkedHashSet<>();
    scopes.add(prefix + "read:" + vhost + "/" + exchange + "/" + routingKeyTemplate(values));
    extraScopes
      .stream()
      .map(scope -> ScopeTemplate.render(scope, values))
      .forEach(scopes::add);
    scopes.removeIf(scope -> scope.isBlank());
    return new ArrayList<>(scopes);
  }

  private String routingKeyTemplate(Map<String, String> values) {
    return ScopeTemplate.render(routingKeyTemplate, values);
  }

  /**
   * A bare resource-server id ({@code stomp}) is completed to {@code stomp.}, matching RabbitMQ's
   * default {@code resource_server_id + "."} prefix. Any other prefix (for example a URL) is used
   * verbatim.
   */
  private static String normalizePrefix(String prefix) {
    if (prefix.isEmpty()) {
      return prefix;
    }
    return prefix.matches("[A-Za-z0-9_-]+") ? prefix + "." : prefix;
  }

  /**
   * Parses a multivalued mapper field. The Admin Console serializes values with {@code ##};
   * newlines are accepted too, so REST/CLI configuration stays readable.
   *
   * <p>A missing key means "not configured", not "use the default": Keycloak strips empty config
   * values before persisting ({@code RepresentationToModel#removeEmptyString}), so a field cleared
   * in the Admin Console arrives here as absent. Defaults come from the Admin Console form prefill
   * only.
   */
  private static List<String> values(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    return Arrays.stream(raw.split("##|\\R"))
      .map(String::trim)
      .filter(value -> !value.isEmpty())
      .toList();
  }

  private static String valueOrDefault(
    Map<String, String> config,
    String key,
    String fallback
  ) {
    String value = config.get(key);
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
