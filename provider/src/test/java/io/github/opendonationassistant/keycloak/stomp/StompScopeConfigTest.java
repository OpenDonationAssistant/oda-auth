package io.github.opendonationassistant.keycloak.stomp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StompScopeConfigTest {

  private static final String SUBSCRIPTION_SCOPES =
    "${prefix}write:${vhost}/stomp-subscription*##" +
    "${prefix}configure:${vhost}/stomp-subscription*";

  @Test
  void emptyConfigOnlyGrantsReadOnTheUsersTopic() {
    StompScopeConfig config = StompScopeConfig.from(Map.of());

    assertEquals(List.of("stomp.read:*/amq.topic/alice.events"), config.scopesFor("alice"));
    assertTrue(config.audiences().isEmpty());
  }

  @Test
  void adminConsolePrefilledValuesAreParsedWithTheirSeparator() {
    StompScopeConfig config = StompScopeConfig.from(
      Map.of("extra.scopes", SUBSCRIPTION_SCOPES, "audience", "stomp")
    );

    assertEquals(
      List.of(
        "stomp.read:*/amq.topic/alice.events",
        "stomp.write:*/stomp-subscription*",
        "stomp.configure:*/stomp-subscription*"
      ),
      config.scopesFor("alice")
    );
    assertEquals(List.of("stomp"), config.audiences());
  }

  @Test
  void clearedFieldsAreTreatedAsNotConfigured() {
    // Keycloak removes empty config values before persisting, so both shapes must mean "none".
    StompScopeConfig cleared = StompScopeConfig.from(
      Map.of("extra.scopes", "", "audience", "")
    );
    StompScopeConfig absent = StompScopeConfig.from(
      Map.of("extra.scopes", "##", "audience", "   ")
    );

    assertEquals(List.of("stomp.read:*/amq.topic/alice.events"), cleared.scopesFor("alice"));
    assertTrue(cleared.audiences().isEmpty());
    assertEquals(cleared.scopesFor("alice"), absent.scopesFor("alice"));
    assertTrue(absent.audiences().isEmpty());
  }

  @Test
  void barePrefixGetsTrailingDotAndCustomPatternsAreRendered() {
    StompScopeConfig config = StompScopeConfig.from(
      Map.of(
        "scope.prefix", "stomp",
        "vhost.pattern", "oda",
        "exchange", "oda.topic",
        "routing.key.template", "user.${username}.*",
        "extra.scopes", "${prefix}read:${vhost}/${exchange}/${username}.events"
      )
    );

    assertEquals(
      List.of(
        "stomp.read:oda/oda.topic/user.alice.*",
        "stomp.read:oda/oda.topic/alice.events"
      ),
      config.scopesFor("alice")
    );
  }

  @Test
  void wildcardVhostIsPreservedWhileUsernameWildcardsAreEscaped() {
    StompScopeConfig config = StompScopeConfig.from(Map.of("extra.scopes", ""));

    assertEquals(
      List.of("stomp.read:*/amq.topic/a%2Ab%2Fc.events"),
      config.scopesFor("a*b/c")
    );
  }

  @Test
  void newlineSeparatedValuesAreAcceptedForRestAndClients() {
    StompScopeConfig config = StompScopeConfig.from(
      Map.of(
        "extra.scopes", "${prefix}write:${vhost}/q*\n${prefix}read:${vhost}/r*",
        "audience", "stomp\naccount"
      )
    );

    assertEquals(
      List.of(
        "stomp.read:*/amq.topic/alice.events",
        "stomp.write:*/q*",
        "stomp.read:*/r*"
      ),
      config.scopesFor("alice")
    );
    assertEquals(List.of("stomp", "account"), config.audiences());
  }

  @Test
  void extraScopesSkipBlankSegmentsAndDuplicates() {
    StompScopeConfig config = StompScopeConfig.from(
      Map.of(
        "extra.scopes",
        "${prefix}read:${vhost}/${exchange}/${username}.events####${prefix}write:${vhost}/q*",
        "user.attribute", " topic "
      )
    );

    assertEquals(
      List.of("stomp.read:*/amq.topic/alice.events", "stomp.write:*/q*"),
      config.scopesFor("alice")
    );
    assertEquals("topic", config.userAttribute());
  }
}
