package io.github.opendonationassistant.keycloak.stomp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.keycloak.models.ProtocolMapperModel;

class StompTopicScopeMapperTest {

  @Test
  void mapperIdAndConfigPropertiesAreStable() {
    StompTopicScopeMapper mapper = new StompTopicScopeMapper();

    assertEquals("oda-stomp-topic-scopes", mapper.getId());
    assertEquals("STOMP topic scopes", mapper.getDisplayType());
    assertEquals("openid-connect", mapper.getProtocol());
    assertTrue(
        mapper.getConfigProperties().stream()
            .anyMatch(property -> "routing.key.template".equals(property.getName())));
  }

  @Test
  void mapperIsIncludedWhenTheFlagIsAbsent() {
    assertTrue(StompTopicScopeMapper.includeInAccessToken(modelWith(Map.of())));
  }

  @Test
  void mapperIsIncludedWhenNoConfigMapIsPresent() {
    assertTrue(StompTopicScopeMapper.includeInAccessToken(new ProtocolMapperModel()));
  }

  @Test
  void mapperHonoursAnExplicitlyDisabledFlag() {
    assertFalse(
        StompTopicScopeMapper.includeInAccessToken(
            modelWith(Map.of("include.in.access.token", "false"))));
    assertTrue(
        StompTopicScopeMapper.includeInAccessToken(
            modelWith(Map.of("include.in.access.token", "TRUE"))));
  }

  private static ProtocolMapperModel modelWith(Map<String, String> config) {
    ProtocolMapperModel model = new ProtocolMapperModel();
    model.setConfig(config);
    return model;
  }
}
