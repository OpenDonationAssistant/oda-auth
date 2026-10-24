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
      normalizePrefix(
        valueOrDefault(safeConfig, "scope.prefix", DEFAULT_PREFIX)
      ),
      valueOrDefault(safeConfig, "vhost.pattern", DEFAULT_VHOST),
      valueOrDefault(safeConfig, "exchange", DEFAULT_EXCHANGE),
      valueOrDefault(
        safeConfig,
        "routing.key.template",
        DEFAULT_ROUTING_KEY_TEMPLATE
      ),
      linesOr(safeConfig, "extra.scopes", DEFAULT_EXTRA_SCOPES),
      audienceOr(safeConfig),
      valueOrDefault(safeConfig, "user.attribute", "")
    );
  }

  List<String> scopesFor(String username) {
    Map<String, String> values = Map.of(
      "username",
      ScopeTemplate.encodeSegment(username),
      "vhost",
      vhost,
      "exchange",
      exchange,
      "prefix",
      prefix
    );

    Set<String> scopes = new LinkedHashSet<>();
    scopes.add(
      prefix +
      "read:" +
      vhost +
      "/" +
      exchange +
      "/" +
      routingKeyTemplate(values)
    );
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

  private static String valueOrDefault(
    Map<String, String> config,
    String key,
    String fallback
  ) {
    String value = config.get(key);
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  /** Audiences accept either a comma-separated or a newline-separated list. */
  private static List<String> audienceOr(Map<String, String> config) {
    if (!config.containsKey("audience")) {
      return List.of(DEFAULT_AUDIENCE);
    }
    String value = config.get("audience");
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return Arrays.stream(value.split("[,\\r\\n]+"))
      .map(String::trim)
      .filter(audience -> !audience.isEmpty())
      .toList();
  }

  /** Missing key means "use the default"; a present but blank value means "none". */
  private static List<String> linesOr(
    Map<String, String> config,
    String key,
    List<String> fallback
  ) {
    if (!config.containsKey(key)) {
      return fallback;
    }
    String value = config.get(key);
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return Arrays.stream(value.split("\\R"))
      .map(String::trim)
      .filter(line -> !line.isEmpty())
      .toList();
  }
}
