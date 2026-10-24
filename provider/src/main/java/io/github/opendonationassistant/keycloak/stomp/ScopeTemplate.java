package io.github.opendonationassistant.keycloak.stomp;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders {@code ${name}} placeholders in scope templates.
 *
 * <p>Unknown placeholders are left untouched: a misconfigured mapper must not break token
 * issuance, and leaving the literal text makes the mistake visible in the issued token.
 */
final class ScopeTemplate {

  private static final Pattern PLACEHOLDER = Pattern.compile(
    "\\$\\{([A-Za-z0-9_.-]+)}"
  );
  private static final Map<Character, String> SEGMENT_ESCAPES = Map.of(
    '%',
    "%25",
    '*',
    "%2A",
    '/',
    "%2F"
  );

  private ScopeTemplate() {}

  static String render(String template, Map<String, String> values) {
    if (template == null || template.isBlank()) {
      return "";
    }
    Matcher matcher = PLACEHOLDER.matcher(template);
    StringBuilder rendered = new StringBuilder();
    while (matcher.find()) {
      String replacement = values.get(matcher.group(1));
      matcher.appendReplacement(
        rendered,
        Matcher.quoteReplacement(
          replacement == null ? matcher.group(0) : replacement
        )
      );
    }
    matcher.appendTail(rendered);
    return rendered.toString();
  }

  /**
   * Encodes characters that RabbitMQ interprets as wildcards or scope separators, so a username
   * cannot widen its own permissions (for example {@code alice*} must not grant every topic).
   */
  static String encodeSegment(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder encoded = new StringBuilder(value.length());
    for (char character : value.toCharArray()) {
      encoded.append(
        SEGMENT_ESCAPES.getOrDefault(character, String.valueOf(character))
      );
    }
    return encoded.toString();
  }
}
