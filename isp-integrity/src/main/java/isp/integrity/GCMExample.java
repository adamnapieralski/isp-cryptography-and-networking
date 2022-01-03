package isp.integrity;

import fri.isp.Agent;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;

/**
 * An example of using the authenticated encryption cipher.
 * <p>
 * During the encryption, the Galois-Counter mode automatically
 * creates a MAC and verifies it during the decryption.
 * <p>
 * What happens, if the cipher text gets modified?
 * What happens, if the IV gets modified?
 * What happens, if the key is incorrect?
 */
public class GCMExample {
    public static void main(String[] args) throws Exception {
        // shared key
        final SecretKey sharedKey = KeyGenerator.getInstance("AES").generateKey();

        // the payload
        final String message = "this is my message";
        final byte[] pt = message.getBytes(StandardCharsets.UTF_8);
        System.out.printf("MSG: %s%n", message);
        System.out.printf("PT:  %s%n", Agent.hex(pt));

        // encrypt
        final Cipher alice = Cipher.getInstance("AES/GCM/NoPadding");
        alice.init(Cipher.ENCRYPT_MODE, sharedKey);
        final byte[] ct = alice.doFinal(pt);
        System.out.printf("CT:  %s%n", Agent.hex(ct));

        // send IV
        final byte[] iv = alice.getIV();
        System.out.printf("IV:  %s%n", Agent.hex(iv));

        // decrypt
        final Cipher bob = Cipher.getInstance("AES/GCM/NoPadding");
        // the length of the MAC tag is either 128, 120, 112, 104 or 96 bits
        // the default is 128 bits
        final GCMParameterSpec specs = new GCMParameterSpec(128, iv);
        bob.init(Cipher.DECRYPT_MODE, sharedKey, specs);
        final byte[] pt2 = bob.doFinal(ct);
        System.out.printf("PT:  %s%n", Agent.hex(pt2));
        System.out.printf("MSG: %s%n", new String(pt2, StandardCharsets.UTF_8));
    }
}
