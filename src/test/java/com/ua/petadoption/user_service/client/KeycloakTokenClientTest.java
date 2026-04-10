package com.ua.petadoption.user_service.client;

import com.ua.petadoption.commons.exception.ServiceException;
import com.ua.petadoption.user_service.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

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
                        {"access_token":"access-token","refresh_token":"refresh-token","expires_in":300}
                        """, MediaType.APPLICATION_JSON));

        TokenResponse result = client.getToken("user@test.com", "password");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.expiresIn()).isEqualTo(300L);
        mockServer.verify();
    }

    @Test
    void getToken_invalidCredentials_shouldThrowServiceException() {
        mockServer.expect(requestTo("http://keycloak/realms/test/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.getToken("user@test.com", "wrong-password"))
                .isInstanceOf(ServiceException.class);

        mockServer.verify();
    }
}
