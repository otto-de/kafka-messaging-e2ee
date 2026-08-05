package de.otto.kafka.messaging.e2ee.vault;

import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import java.util.Base64;
import java.util.Objects;

/**
 * Basic vault connection configuration.
 *
 * @param address     the URL of the vault
 * @param token       authentification token or <code>null</code>
 * @param appRole     app role configuration or <code>null</code>
 * @param awsIamLogin AWS IAM login configuration or <code>null</code>
 */
public record VaultConnectionConfig(
    String address,
    String token,
    VaultAppRole appRole,
    VaultAwsIamLogin awsIamLogin
) {

  private static final String DEFAULT_VAULT_URL = "http://localhost:8200";

  /**
   * Constructor with all fields.
   *
   * @param address     the URL of the vault
   * @param token       authentification token or <code>null</code>
   * @param appRole     app role configuration or <code>null</code>
   * @param awsIamLogin AWS IAM login configuration or <code>null</code>
   * @see #builder()
   */
  public VaultConnectionConfig(String address, String token, VaultAppRole appRole,
      VaultAwsIamLogin awsIamLogin) {
    boolean authMethodSet = appRole != null;
    if (token != null) {
      if (authMethodSet) {
        throw new VaultConfigException(
            "Vault token must not be configured when appRole is provided");
      }
      authMethodSet = true;
    }
    if (awsIamLogin != null) {
      if (authMethodSet) {
        throw new VaultConfigException(
            "Exactly one of the methods must be used: token, appRole or awsIamLogin");
      }
      authMethodSet = true;
    }
    if (!authMethodSet) {
      throw new VaultConfigException("Neither vault token nor appRole nor awsIamLogin is provided");
    }

    this.address = Objects.requireNonNullElse(address, DEFAULT_VAULT_URL);
    this.token = token;
    this.appRole = appRole;
    this.awsIamLogin = awsIamLogin;
  }

  /**
   * Constructor with all fields.
   *
   * @param address the URL of the vault
   * @param token   authentification token or <code>null</code>
   * @param appRole app role configuration or <code>null</code>
   * @see #builder()
   */
  public VaultConnectionConfig(String address, String token, VaultAppRole appRole) {
    this(address, token, appRole, null);
  }

  /**
   * a builder
   *
   * @return a new builder instance
   */
  public static VaultConnectionConfigBuilder builder() {
    return new VaultConnectionConfigBuilder();
  }

  /**
   * Creates a RenewableVault instance based on the current vault configuration.
   *
   * @return the Vault API
   * @throws VaultException on errors related to vault
   */
  public RenewableVault createRenewableVault() throws VaultException {
    VaultConfig config = new VaultConfig()
        .address(address)
        .token(token)
        // Defaults to "VAULT_OPEN_TIMEOUT" environment variable
        .openTimeout(5)
        // Defaults to "VAULT_READ_TIMEOUT" environment variable
        .readTimeout(30)
        // we use KV Secrets Engine version 2 (that is the latest one)
        .engineVersion(2)
        .build();

    if (token != null) {
      return new RenewableVault(config);
    }
    if (appRole != null) {
      return new RenewableVault(config, appRole);
    }
    return new RenewableVault(config, awsIamLogin);
  }

  /**
   * Builder for VaultConnectionConfig
   */
  public static class VaultConnectionConfigBuilder {

    private String address;
    private String token;
    private VaultAppRole appRole;
    private VaultAwsIamLogin awsIamLogin;

    /**
     * Default constructor
     */
    public VaultConnectionConfigBuilder() {
    }

    /**
     * Creates a config object.
     *
     * @return the VaultConnectionConfig
     */
    public VaultConnectionConfig build() {
      return new VaultConnectionConfig(address, token, appRole, awsIamLogin);
    }

    /**
     * Sets the address property.
     *
     * @param address the URL of the vault. Default value is <code>http://localhost:8200</code>
     * @return this builder
     */
    public VaultConnectionConfigBuilder address(String address) {
      this.address = address;
      return this;
    }

    /**
     * Sets the token property. Either a token, an appRole or awsIamLogin must be present.
     *
     * @param token authentification token
     * @return this builder
     */
    public VaultConnectionConfigBuilder token(String token) {
      this.token = token;
      return this;
    }

    /**
     * Sets the appRole property. Either a token, an appRole or awsIamLogin must be present.
     *
     * @param appRole app role configuration
     * @return this builder
     */
    public VaultConnectionConfigBuilder appRole(VaultAppRole appRole) {
      this.appRole = appRole;
      return this;
    }

    /**
     * Sets the awsIamLogin property. Either a token, an appRole or awsIamLogin must be present.
     *
     * @param awsIamLogin AWS IAM login configuration
     * @return this builder
     */
    public VaultConnectionConfigBuilder awsIamLogin(VaultAwsIamLogin awsIamLogin) {
      this.awsIamLogin = awsIamLogin;
      return this;
    }
  }

  /**
   * Vault app role configuration.
   *
   * @param path     the app role path
   * @param roleid   the app role id
   * @param secretid the app role secret / password
   */
  public record VaultAppRole(
      String path,
      String roleid,
      String secretid
  ) {

    /**
     * Default constructor
     *
     * @param path     the app role path
     * @param roleid   the app role id
     * @param secretid the app role secret / password
     */
    public VaultAppRole {
      Objects.requireNonNull(path, "path is required");
      Objects.requireNonNull(roleid, "roleid is required");
      Objects.requireNonNull(secretid, "secretid is required");
    }

    /**
     * a builder
     *
     * @return a new builder instance
     */
    public static AppRoleBuilder builder() {
      return new AppRoleBuilder();
    }

    /**
     * Builder for VaultAppRole
     */
    public static class AppRoleBuilder {

      private String path;
      private String roleid;
      private String secretid;

      /**
       * Default constructor
       */
      public AppRoleBuilder() {
      }

      /**
       * Sets the path property.
       *
       * @param path the app role path
       * @return this builder
       */
      public AppRoleBuilder path(String path) {
        this.path = path;
        return this;
      }

      /**
       * Sets the roleid property.
       *
       * @param roleid the app role id
       * @return this builder
       */
      public AppRoleBuilder roleid(String roleid) {
        this.roleid = roleid;
        return this;
      }

      /**
       * Sets the secretid property.
       *
       * @param secretid the app role secret / password
       * @return this builder
       */
      public AppRoleBuilder secretid(String secretid) {
        this.secretid = secretid;
        return this;
      }

      /**
       * Creates a app role configuration object.
       *
       * @return the VaultAppRole
       */
      public VaultAppRole build() {
        return new VaultAppRole(path, roleid, secretid);
      }
    }
  }

  /**
   * Represent AWS IAM login configuration
   *
   * @param role                    Name of the role against which the login is being attempted. If
   *                                role is not specified, then the login endpoint looks for a role
   *                                bearing the name of the AMI ID of the EC2 instance that is
   *                                trying to login if using the ec2 auth method, or the "friendly
   *                                name" (i.e., role name or username) of the IAM principal
   *                                authenticated. If a matching role is not found, login fails.
   * @param iamRequestUrlBase64     PKCS7 signature of the identity document with all \n characters
   *                                removed.Base64-encoded HTTP URL used in the signed request. Most
   *                                likely just aHR0cHM6Ly9zdHMuYW1hem9uYXdzLmNvbS8=
   *                                (base64-encoding of https://sts.amazonaws.com/  ) as most
   *                                requests will probably use POST with an empty URI.
   * @param iamRequestBodyBase64    Base64-encoded body of the signed request. Most likely
   *                                QWN0aW9uPUdldENhbGxlcklkZW50aXR5JlZlcnNpb249MjAxMS0wNi0xNQ==
   *                                which is the base64 encoding of
   *                                Action=GetCallerIdentity&amp;Version=2011-06-15.
   * @param iamRequestHeadersBase64 Request headers Base64-encoded JSON format
   * @param awsAuthMount            Vault auth mount. Most likely <code>aws</code>.
   */
  public record VaultAwsIamLogin(
      String role,
      String iamRequestUrlBase64,
      String iamRequestBodyBase64,
      String iamRequestHeadersBase64,
      String awsAuthMount
  ) {

    /**
     * Default constructor
     *
     * @param role                    see record comment
     * @param iamRequestUrlBase64     see record comment
     * @param iamRequestBodyBase64    see record comment
     * @param iamRequestHeadersBase64 see record comment
     * @param awsAuthMount            see record comment
     */
    public VaultAwsIamLogin {
      Objects.requireNonNull(iamRequestUrlBase64, "iamRequestUrl is required");
      Objects.requireNonNull(iamRequestBodyBase64, "iamRequestBody is required");
      Objects.requireNonNull(iamRequestHeadersBase64, "iamRequestHeaders is required");
      Objects.requireNonNull(awsAuthMount, "awsAuthMount is required");
    }

    /**
     * a builder
     *
     * @return a new builder instance
     */
    public static VaultAwsIamLoginBuilder builder() {
      return new VaultAwsIamLoginBuilder();
    }
  }

  /**
   * Builder class for VaultAwsIamLogin
   */
  public static class VaultAwsIamLoginBuilder {

    private String role;
    private String iamRequestUrl;
    private String iamRequestBody;
    private String iamRequestHeaders;
    private String awsAuthMount;

    /**
     * Default constructor
     */
    public VaultAwsIamLoginBuilder() {
    }

    /**
     * Create the VaultAwsIamLogin instance
     *
     * @return the VaultAwsIamLogin with the values converted to base64
     */
    public VaultAwsIamLogin build() {
      if (awsAuthMount == null) {
        this.awsAuthMount = "aws";
      }
      if (iamRequestUrl == null) {
        this.iamRequestUrl = "https://sts.amazonaws.com/";
      }
      if (iamRequestBody == null) {
        this.iamRequestBody = "Action=GetCallerIdentity&Version=2011-06-15";
      }
      if (iamRequestHeaders == null) {
        this.iamRequestHeaders = "{}";
      }

      String iamRequestUrlBase64 = Base64.getUrlEncoder()
          .encodeToString(iamRequestUrl.getBytes());
      String iamRequestBodyBase64 = Base64.getUrlEncoder()
          .encodeToString(iamRequestBody.getBytes());
      String iamRequestHeadersBase64 = Base64.getUrlEncoder()
          .encodeToString(iamRequestHeaders.getBytes());

      return new VaultAwsIamLogin(role, iamRequestUrlBase64, iamRequestBodyBase64,
          iamRequestHeadersBase64, awsAuthMount);
    }

    /**
     * Sets the AWS IAM role
     *
     * @param role the AWS IAM role to use.
     * @return the builder
     */
    public VaultAwsIamLoginBuilder role(String role) {
      this.role = role;
      return this;
    }

    /**
     * Sets the AWS STS request url.
     *
     * @param iamRequestUrl the AWS STS request url. Default value is
     *                      <code>https://sts.amazonaws.com/</code>
     * @return the builder
     */
    public VaultAwsIamLoginBuilder iamRequestUrl(String iamRequestUrl) {
      this.iamRequestUrl = iamRequestUrl;
      return this;
    }

    /**
     * Sets the AWS STS request body.
     *
     * @param iamRequestBody the AWS STS request body. Default value is
     *                       Action=GetCallerIdentity&amp;Version=2011-06-15
     * @return the builder
     */
    public VaultAwsIamLoginBuilder iamRequestBody(String iamRequestBody) {
      this.iamRequestBody = iamRequestBody;
      return this;
    }

    /**
     * Sets the AWS STS request headers in JSON format.
     *
     * @param iamRequestHeaders the AWS STS request headers. For example:
     *                          {@code {"Content-Length": ["43"], "User-Agent": ["aws-sdk-go/1.4.12
     *                          (go1.7.1; linux; amd64)"], "X-Vault-AWSIAM-Server-Id":
     *                          ["://example.com"], "X-Amz-Date": ["20160930T043121Z"],
     *                          "Content-Type": ["application/x-www-form-urlencoded;
     *                          charset=utf-8"], "Authorization": ["AWS4-HMAC-SHA256
     *                          Credential=foo/20160930/us-east-1/sts/aws4_request,
     *                          SignedHeaders=content-length;content-type;host;x-amz-date;x-vault-server,
     *                          Signature=a69fd750a3445c4e553e1b3e79d3da90eef54047f1eb4efe8ffbc9c428c2655b"]}}
     * @return the builder
     */
    public VaultAwsIamLoginBuilder iamRequestHeaders(String iamRequestHeaders) {
      this.iamRequestHeaders = iamRequestHeaders;
      return this;
    }

    /**
     * Sets the Vault AWS mount path.
     *
     * @param awsAuthMount The vault auth mount. Default value is <code>aws</code>
     * @return the builder
     */
    public VaultAwsIamLoginBuilder awsAuthMount(String awsAuthMount) {
      this.awsAuthMount = awsAuthMount;
      return this;
    }
  }
}
