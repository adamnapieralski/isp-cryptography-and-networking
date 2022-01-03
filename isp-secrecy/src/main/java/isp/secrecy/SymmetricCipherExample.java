package isp.secrecy;

import fri.isp.Agent;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.security.Key;

/**
 * EXERCISE:
 * - Study the example
 * - Test different ciphers
 *
 * https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/crypto/Cipher.html
 */
public class SymmetricCipherExample {
    public static void main(String[] args) throws Exception {
        final String message = "Hi Bob, this is Alice.";
        System.out.println("[MESSAGE] " + message);

        // STEP 1: Alice and Bob agree upon a cipher and a shared secret key
        final Key key = KeyGenerator.getInstance("RC4").generateKey();

        final byte[] pt = message.getBytes();
        System.out.println("[PT] " + Agent.hex(pt));

        //  STEP 2: Create a cipher, encrypt the PT and, optionally, extract cipher parameters (such as IV)
        final Cipher encrypt = Cipher.getInstance("RC4");
        encrypt.init(Cipher.ENCRYPT_MODE, key);
        final byte[] cipherText = encrypt.doFinal(pt);

        // STEP 3: Print out cipher text (in HEX) [this is what an attacker would see]
        System.out.println("[CT] " + Agent.hex(cipherText));

        /*
         * STEP 4.
         * The receiver creates a Cipher object, defines the algorithm, the secret key and
         * possibly additional parameters (such as IV), and then decrypts the cipher text
         */
        final Cipher decrypt = Cipher.getInstance("RC4");
        decrypt.init(Cipher.DECRYPT_MODE, key);
        final byte[] dt = decrypt.doFinal(cipherText);
        System.out.println("[PT] " + Agent.hex(dt));

        // Todo: What happens if the key is incorrect? (Try with RC4 or AES in CTR mode)

        // STEP 5: Create a string from a byte array
        System.out.println("[MESSAGE] " + new String(dt));
    }
}
