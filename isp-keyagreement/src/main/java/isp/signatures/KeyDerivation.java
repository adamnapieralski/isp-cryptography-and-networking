package isp.signatures;

import fri.isp.Agent;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;

public class KeyDerivation {
    public static void main(String[] args) throws Exception {
        // password from which the key will be derived
        final String password = "hunter2";

        // a random, public and fixed string
        final byte[] salt = "89fjh3409fdj390fk".getBytes(StandardCharsets.UTF_8);

        // use PBKDF2 with the password, salt, and number of iterations and required bits
        final SecretKeyFactory pbkdf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        final KeySpec specs = new PBEKeySpec(password.toCharArray(), salt,
                10000, 128);
        final SecretKey generatedKey = pbkdf.generateSecret(specs);

        System.out.printf("key = %s%n", Agent.hex(generatedKey.getEncoded()));
        System.out.printf("len(key) = %d bytes%n", generatedKey.getEncoded().length);

        final String message = "Hello World!";

        // for example, use the derived key as the HMAC key
        final Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(generatedKey.getEncoded(), "HmacSHA256"));
        System.out.printf("HMAC[%s] = %s%n", message, Agent.hex(hmac.doFinal(message.getBytes())));

    }
}
