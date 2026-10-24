package io.github.opendonationassistant.keycloak.stomp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StompScopeConfigTest {

  @Test
  void defaultsGrantReadOnTheUsersTopicAndSubscriptionQueueScopes() {
    StompScopeConfig config = StompScopeConfig.from(Map.of());

    assertEquals(
        List.of(
            "stomp.read:*/amq.topic/alice.events",
            "stomp.write:*/stomp-subscription*",
            "stomp.configure:*/stomp-subscription*"),
        config.scopesFor("alice"));
    assertEquals(List.of("stomp"), config.audiences());
  }

  @Test
  void barePrefixGetsTrailingDotAndCustomPatternsAreRendered() {
    StompScopeConfig config =
        StompScopeConfig.from(
            Map.of(
                "scope.prefix", "stomp",
                "vhost.pattern", "oda",
                "exchange", "oda.topic",
                "routing.key.template", "user.${username}.*",
                "extra.scopes", "${prefix}read:${vhost}/${exchange}/${username}.events",
                "audience", ""));

    StompScopeConfig resolved = config;
    assertEquals(
        List.of(
            "stomp.read:oda/oda.topic/user.alice.*",
            "stomp.read:oda/oda.topic/alice.events"),
        resolved.scopesFor("alice"));
    assertTrue(resolved.audiences().isEmpty());
  }

  @Test
  void wildcardVhostIsPreservedWhileUsernameWildcardsAreEscaped() {
    StompScopeConfig config = StompScopeConfig.from(Map.of("extra.scopes", ""));

    assertEquals(
        List.of("stomp.read:*/amq.topic/a%2Ab%2Fc.events"), config.scopesFor("a*b/c"));
  }

  @Test
  void blankExtraScopesKeyDisablesGeneratedQueueScopes() {
    StompScopeConfig config = StompScopeConfig.from(Map.of("extra.scopes", " "));

    assertEquals(List.of("stomp.read:*/amq.topic/alice.events"), config.scopesFor("alice"));
  }

  @Test
  void audienceAcceptsCommaAndNewlineSeparatedValues() {
    StompScopeConfig config =
        StompScopeConfig.from(Map.of("audience", "stomp, account\nopenid"));

    assertEquals(List.of("stomp", "account", "openid"), config.audiences());
  }

  @Test
  void extraScopesSkipBlankLinesAndDuplicates() {
    StompScopeConfig config =
        StompScopeConfig.from(
            Map.of(
                "extra.scopes",
                "${prefix}read:${vhost}/${exchange}/${username}.events\n\n"
                    + "${prefix}write:${vhost}/q*",
                "user.attribute", " topic "));

    assertEquals(
        List.of("stomp.read:*/amq.topic/alice.events", "stomp.write:*/q*"),
        config.scopesFor("alice"));
    assertEquals("topic", config.userAttribute());
  }
}
