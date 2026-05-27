package com.ua.petadoption.user_service.client;

import com.ua.petadoption.commons.exception.ServiceException;
import com.ua.petadoption.user_service.dto.KeycloakTokenResponse;
import com.ua.petadoption.user_service.exception.UserErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class KeycloakTokenClientTest {

    private MockRestServiceServer mockServer;
    private KeycloakTokenClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        client = new KeycloakTokenClient(RestClient.builder(restTemplate));
        ReflectionTestUtils.setField(client, "serverUrl", "http://keycloak");
        ReflectionTestUtils.setField(client, "realm", "test");
        ReflectionTestUtils.setField(client, "clientId", "gateway-client");
        ReflectionTestUtils.setField(client, "clientSecret", "secret");
    }

    @Test
    void getToken_validCredentials_shouldReturnTokenResponse() {
        mockServer.expect(requestTo("http://keycloak/realms/test/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token":"access-token",
                        "refresh_token":"refresh-token",
                        "expires_in":300,
                        "refresh_expires_in":2592000}
                        """, MediaType.APPLICATION_JSON));

        KeycloakTokenResponse result = client.getToken("user@test.com", "password");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.expiresIn()).isEqualTo(300L);
        assertThat(result.refreshExpiresIn()).isEqualTo(2592000L);
        mockServer.verify();
    }

    @Test
    void getToken_invalidCredentials_shouldThrowServiceException() {
        mockServer.expect(requestTo("http://keycloak/realms/test/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.getToken("user@test.com", "wrong-password"))
                .isInstanceOfSatisfying(ServiceException.class, se -> {
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(se.getErrorCode()).isEqualTo(UserErrorCode.INVALID_CREDENTIALS);
                });

        mockServer.verify();
    }

    @Test
    void refreshToken_validToken_shouldReturnNewTokenResponse() {
        mockServer.expect(requestTo("http://keycloak/realms/test/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token":"new-access-token",
                        "refresh_token":"new-refresh-token",
                        "expires_in":300,
                        "refresh_expires_in":2592000}
                        """, MediaType.APPLICATION_JSON));

        KeycloakTokenResponse result = client.refreshToken("old-refresh-token");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.expiresIn()).isEqualTo(300L);
        assertThat(result.refreshExpiresIn()).isEqualTo(2592000L);
        mockServer.verify();
    }

    @Test
    void refreshToken_expiredToken_shouldThrowServiceException() {
        mockServer.expect(requestTo("http://keycloak/realms/test/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.refreshToken("expired-refresh-token"))
                .isInstanceOfSatisfying(ServiceException.class, se -> {
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(se.getErrorCode()).isEqualTo(UserErrorCode.INVALID_CREDENTIALS);
                });

        mockServer.verify();
    }
}
