package io.github.opendonationassistant.keycloak.stomp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ScopeTemplateTest {

  @Test
  void renderReplacesKnownPlaceholders() {
    String rendered =
        ScopeTemplate.render(
            "${prefix}read:${vhost}/${exchange}/${username}.events",
            Map.of("prefix", "stomp.", "vhost", "*", "exchange", "amq.topic", "username", "alice"));

    assertEquals("stomp.read:*/amq.topic/alice.events", rendered);
  }

  @Test
  void renderLeavesUnknownPlaceholdersUntouched() {
    String rendered = ScopeTemplate.render("read:${unknown}/x", Map.of("username", "alice"));

    assertEquals("read:${unknown}/x", rendered);
  }

  @Test
  void renderReturnsEmptyStringForBlankTemplate() {
    assertEquals("", ScopeTemplate.render(null, Map.of()));
    assertEquals("", ScopeTemplate.render("  ", Map.of()));
  }

  @Test
  void encodeSegmentEscapesRabbitMqWildcardsAndSeparators() {
    assertEquals("a%2Ab", ScopeTemplate.encodeSegment("a*b"));
    assertEquals("a%2Fb", ScopeTemplate.encodeSegment("a/b"));
    assertEquals("100%25", ScopeTemplate.encodeSegment("100%"));
    assertEquals("alice", ScopeTemplate.encodeSegment("alice"));
    assertEquals("", ScopeTemplate.encodeSegment(null));
  }
}
