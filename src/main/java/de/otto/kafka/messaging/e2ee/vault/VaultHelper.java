package de.otto.kafka.messaging.e2ee.vault;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public interface VaultHelper {

  static byte[] decodeBase64Key(String base64AndUrlEncodedKey) {
    // decode URL-encoding
    String base64Key = urlDecode(base64AndUrlEncodedKey);
    // remove "carriage return" and "line feed" from the vault secret
    String cleanBase64Key = base64Key.replaceAll("((\\r?\\n)|(\\r))", "");
    return Base64.getDecoder().decode(cleanBase64Key);
  }

  private static String urlDecode(String urlEncodedValue) {
    try {
      return URLDecoder.decode(urlEncodedValue, StandardCharsets.US_ASCII.toString());
    } catch (UnsupportedEncodingException ex) {
      throw new VaultRuntimeException(ex);
    }
  }
}