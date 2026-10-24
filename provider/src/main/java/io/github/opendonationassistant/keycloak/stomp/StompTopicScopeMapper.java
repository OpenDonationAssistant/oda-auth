package io.github.opendonationassistant.keycloak.stomp;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

/**
 * Adds per-user RabbitMQ OAuth2 scopes to the access token's {@code scope} claim.
 *
 * <p>RabbitMQ reads only the {@code scope} claim (and {@code additional_scopes_key} entries), so a
 * standalone custom claim would be ignored. The mapper always grants read access to
 * {@code /topic/<preferred_username>.events}; the Admin Console prefills the scopes needed for the
 * auto-created STOMP subscription queue. Cleared fields are treated as "not configured" because
 * Keycloak drops empty config values.
 */
public class StompTopicScopeMapper
  extends AbstractOIDCProtocolMapper
  implements OIDCAccessTokenMapper
{
  public static final String PROVIDER_ID = "oda-stomp-topic-scopes";
  static final String INCLUDE_IN_ACCESS_TOKEN = "include.in.access.token";

  private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = createConfigProperties();

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public String getDisplayCategory() {
    return TOKEN_MAPPER_CATEGORY;
  }

  @Override
  public String getDisplayType() {
    return "STOMP topic scopes";
  }

  @Override
  public String getHelpText() {
    return "Adds RabbitMQ OAuth2 scopes (read on the user's STOMP topic) to the scope claim.";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return CONFIG_PROPERTIES;
  }

  /**
   * Overrides the base behaviour on purpose: {@code AbstractOIDCProtocolMapper} requires
   * {@code access.token.claim=true} in the mapper config, so a mapper created through the REST API
   * with an empty config would silently emit nothing. Here an absent flag means "include".
   */
  @Override
  public AccessToken transformAccessToken(
    AccessToken token,
    ProtocolMapperModel mappingModel,
    KeycloakSession session,
    UserSessionModel userSession,
    ClientSessionContext clientSessionCtx
  ) {
    if (!includeInAccessToken(mappingModel)) {
      return token;
    }
    setClaim(token, mappingModel, userSession, session, clientSessionCtx);
    return token;
  }

  static boolean includeInAccessToken(ProtocolMapperModel mappingModel) {
    Map<String, String> config = mappingModel.getConfig();
    return config == null || !"false".equalsIgnoreCase(config.get(INCLUDE_IN_ACCESS_TOKEN));
  }

  @Override
  protected void setClaim(
    IDToken token,
    ProtocolMapperModel mappingModel,
    UserSessionModel userSession,
    KeycloakSession session,
    ClientSessionContext clientSessionCtx
  ) {
    if (!(token instanceof AccessToken accessToken)) {
      return;
    }

    StompScopeConfig config = StompScopeConfig.from(mappingModel.getConfig());
    String username = resolveUsername(userSession.getUser(), config.userAttribute());
    if (username == null || username.isBlank()) {
      // Fail closed: a blank username must never render a wildcard-only scope.
      return;
    }

    Set<String> scopes = new LinkedHashSet<>();
    String existingScope = accessToken.getScope();
    if (existingScope != null && !existingScope.isBlank()) {
      scopes.addAll(Arrays.asList(existingScope.trim().split("\\s+")));
    }
    scopes.addAll(config.scopesFor(username));
    accessToken.setScope(String.join(" ", scopes));
    config.audiences().forEach(accessToken::addAudience);
  }

  private static String resolveUsername(UserModel user, String attribute) {
    if (user == null) {
      return null;
    }
    if (!attribute.isBlank()) {
      String attributeValue = user.getFirstAttribute(attribute);
      if (attributeValue != null && !attributeValue.isBlank()) {
        return attributeValue;
      }
    }
    return user.getUsername();
  }

  private static List<ProviderConfigProperty> createConfigProperties() {
    return List.of(
      new ProviderConfigProperty(
        INCLUDE_IN_ACCESS_TOKEN,
        "Include in access token",
        "Add the scopes to access tokens. Disable to keep the mapper configured but inactive.",
        ProviderConfigProperty.BOOLEAN_TYPE,
        "true"
      ),
      new ProviderConfigProperty(
        "scope.prefix",
        "Scope prefix",
        "Prefix for every emitted scope. RabbitMQ prepends '<resource_server_id>.'; a bare id " +
        "gets the trailing dot automatically.",
        ProviderConfigProperty.STRING_TYPE,
        StompScopeConfig.DEFAULT_PREFIX
      ),
      new ProviderConfigProperty(
        "vhost.pattern",
        "Virtual host pattern",
        "RabbitMQ vhost the scopes apply to; '*' matches every vhost.",
        ProviderConfigProperty.STRING_TYPE,
        StompScopeConfig.DEFAULT_VHOST
      ),
      new ProviderConfigProperty(
        "exchange",
        "Topic exchange",
        "Exchange used for STOMP topic destinations. '/topic/X' maps to 'amq.topic' with " +
        "routing key 'X'.",
        ProviderConfigProperty.STRING_TYPE,
        StompScopeConfig.DEFAULT_EXCHANGE
      ),
      new ProviderConfigProperty(
        "routing.key.template",
        "Routing key template",
        "Routing key granted for reading. Placeholders: ${username}, ${vhost}, ${exchange}, " +
        "${prefix}.",
        ProviderConfigProperty.STRING_TYPE,
        StompScopeConfig.DEFAULT_ROUTING_KEY_TEMPLATE
      ),
      new ProviderConfigProperty(
        "extra.scopes",
        "Extra scopes",
        "One scope per value. The Admin Console prefills the STOMP subscription-queue scopes; " +
        "clear the field to remove them. REST/CLI clients may separate values with ## or " +
        "newlines.",
        ProviderConfigProperty.MULTIVALUED_STRING_TYPE,
        String.join("##", StompScopeConfig.DEFAULT_EXTRA_SCOPES)
      ),
      new ProviderConfigProperty(
        "audience",
        "Audience",
        "Audience added to the token. The Admin Console prefills 'stomp'; clear the field to " +
        "leave the audience untouched. RabbitMQ rejects tokens whose 'aud' does not contain " +
        "resource_server_id.",
        ProviderConfigProperty.MULTIVALUED_STRING_TYPE,
        StompScopeConfig.DEFAULT_AUDIENCE
      ),
      new ProviderConfigProperty(
        "user.attribute",
        "Username attribute",
        "User attribute holding the topic name. Empty means use preferred_username.",
        ProviderConfigProperty.STRING_TYPE,
        ""
      )
    );
  }
}
