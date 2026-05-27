package com.ua.petadoption.user_service.client;

import com.ua.petadoption.commons.exception.ServiceException;
import com.ua.petadoption.user_service.dto.KeycloakTokenResponse;
import com.ua.petadoption.user_service.exception.UserErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class KeycloakTokenClient {

    private static final String CLAIM_ACCESS_TOKEN = "access_token";
    private static final String CLAIM_REFRESH_TOKEN = "refresh_token";
    private static final String CLAIM_EXPIRES_IN = "expires_in";
    private static final String CLAIM_REFRESH_EXPIRES_IN = "refresh_expires_in";

    private final RestClient restClient;

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.gateway-client-id}")
    private String clientId;

    @Value("${keycloak.gateway-client-secret}")
    private String clientSecret;

    public KeycloakTokenClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public KeycloakTokenResponse getToken(String email, String password) {
        log.debug("Requesting token from Keycloak for user {}", email);
        MultiValueMap<String, String> form = baseForm("password");
        form.add("username", email);
        form.add("password", password);
        return requestToken(form);
    }

    public KeycloakTokenResponse refreshToken(String refreshToken) {
        log.debug("Refreshing token via Keycloak");
        MultiValueMap<String, String> form = baseForm("refresh_token");
        form.add("refresh_token", refreshToken);
        return requestToken(form);
    }

    private MultiValueMap<String, String> baseForm(String grantType) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", grantType);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        return form;
    }

    @SuppressWarnings("unchecked")
    private KeycloakTokenResponse requestToken(MultiValueMap<String, String> form) {
        Map<String, Object> response = restClient.post()
                .uri(serverUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ServiceException(HttpStatus.UNAUTHORIZED, UserErrorCode.INVALID_CREDENTIALS);
                })
                .body(Map.class);

        return new KeycloakTokenResponse(
                (String) response.get(CLAIM_ACCESS_TOKEN),
                (String) response.get(CLAIM_REFRESH_TOKEN),
                ((Number) response.get(CLAIM_EXPIRES_IN)).longValue(),
                ((Number) response.get(CLAIM_REFRESH_EXPIRES_IN)).longValue()
        );
    }
}
