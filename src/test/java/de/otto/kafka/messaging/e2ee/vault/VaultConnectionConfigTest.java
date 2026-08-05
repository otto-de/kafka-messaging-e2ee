package de.otto.kafka.messaging.e2ee.vault;

import static org.assertj.core.api.Assertions.assertThat;

import de.otto.kafka.messaging.e2ee.vault.VaultConnectionConfig.VaultAppRole;
import de.otto.kafka.messaging.e2ee.vault.VaultConnectionConfig.VaultAwsIamLogin;
import org.junit.jupiter.api.Test;

class VaultConnectionConfigTest {

  @Test
  void shouldCreateTokenBasedConfig() {
    VaultConnectionConfig result = VaultConnectionConfig.builder()
        .address("http://the-vault-address")
        .token("some-token")
        .build();

    assertThat(result.address()).isEqualTo("http://the-vault-address");
    assertThat(result.token()).isEqualTo("some-token");
    assertThat(result.appRole()).isNull();
    assertThat(result.awsIamLogin()).isNull();
  }

  @Test
  void shouldCreateAppRoleBasedConfig() {
    VaultConnectionConfig result = VaultConnectionConfig.builder()
        .address("http://the-vault-address")
        .appRole(VaultAppRole.builder()
            .path("some-path")
            .roleid("some-roleid")
            .secretid("some-secretid")
            .build())
        .build();

    assertThat(result.address()).isEqualTo("http://the-vault-address");
    assertThat(result.appRole()).isNotNull();
    assertThat(result.token()).isNull();
    assertThat(result.awsIamLogin()).isNull();

    VaultAppRole appRoleResult = result.appRole();
    assertThat(appRoleResult.path()).isEqualTo("some-path");
    assertThat(appRoleResult.roleid()).isEqualTo("some-roleid");
    assertThat(appRoleResult.secretid()).isEqualTo("some-secretid");
  }

  @Test
  void shouldCreateAwsIamLoginConfigWithDefaultValues() {
    VaultConnectionConfig result = VaultConnectionConfig.builder()
        .address("http://the-vault-address")
        .awsIamLogin(VaultAwsIamLogin.builder()
            .role("some-role")
            .iamRequestHeaders("{\"Content-Length\": [34]}")
            .build())
        .build();

    assertThat(result.address()).isEqualTo("http://the-vault-address");
    assertThat(result.awsIamLogin()).isNotNull();
    assertThat(result.token()).isNull();
    assertThat(result.appRole()).isNull();

    VaultAwsIamLogin awsIamLoginResult = result.awsIamLogin();
    assertThat(awsIamLoginResult.role()).isEqualTo("some-role");
    assertThat(awsIamLoginResult.iamRequestUrlBase64()).isEqualTo("aHR0cHM6Ly9zdHMuYW1hem9uYXdzLmNvbS8=");
    assertThat(awsIamLoginResult.iamRequestBodyBase64()).isEqualTo("QWN0aW9uPUdldENhbGxlcklkZW50aXR5JlZlcnNpb249MjAxMS0wNi0xNQ==");
    assertThat(awsIamLoginResult.iamRequestHeadersBase64()).isEqualTo("eyJDb250ZW50LUxlbmd0aCI6IFszNF19");
    assertThat(awsIamLoginResult.awsAuthMount()).isEqualTo("aws");
  }

  @Test
  void shouldCreateAwsIamLoginConfig() {
    VaultConnectionConfig result = VaultConnectionConfig.builder()
        .address("http://the-vault-address")
        .awsIamLogin(VaultAwsIamLogin.builder()
            .role("some-role")
            .iamRequestUrl("some-request-url")
            .iamRequestBody("some-request-body")
            .iamRequestHeaders("some-request-headers")
            .awsAuthMount("some-awsAuthMount")
            .build())
        .build();

    assertThat(result.address()).isEqualTo("http://the-vault-address");
    assertThat(result.awsIamLogin()).isNotNull();
    assertThat(result.token()).isNull();
    assertThat(result.appRole()).isNull();

    VaultAwsIamLogin awsIamLoginResult = result.awsIamLogin();
    assertThat(awsIamLoginResult.role()).isEqualTo("some-role");
    assertThat(awsIamLoginResult.iamRequestUrlBase64()).isEqualTo("c29tZS1yZXF1ZXN0LXVybA==");
    assertThat(awsIamLoginResult.iamRequestBodyBase64()).isEqualTo("c29tZS1yZXF1ZXN0LWJvZHk=");
    assertThat(awsIamLoginResult.iamRequestHeadersBase64()).isEqualTo("c29tZS1yZXF1ZXN0LWhlYWRlcnM=");
    assertThat(awsIamLoginResult.awsAuthMount()).isEqualTo("some-awsAuthMount");
  }
}