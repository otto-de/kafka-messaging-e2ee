package de.otto.kafka.messaging.e2ee.vault;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Some helper methods for dealing with decoding the encryption keys from vault
 */
public interface VaultHelper {

  /**
   * Parse the encryption key based on the vault value.
   *
   * @param base64AndUrlEncodedKey the base64- and url-encoded encryption key
   * @return the decoded encryption key
   */
  static byte[] decodeBase64Key(String base64AndUrlEncodedKey) {
    // decode URL-encoding
    String base64Key = urlDecode(base64AndUrlEncodedKey);
    // remove "carriage return" and "line feed" from the vault secret
    String cleanBase64Key = base64Key.replaceAll("((\\r?\\n)|(\\r))", "");
    return Base64.getDecoder().decode(cleanBase64Key);
  }

  private static String urlDecode(String urlEncodedValue) {
    return URLDecoder.decode(urlEncodedValue, StandardCharsets.US_ASCII);
  }
}