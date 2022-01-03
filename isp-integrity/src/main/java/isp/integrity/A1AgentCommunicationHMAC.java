package isp.integrity;

import fri.isp.Agent;
import fri.isp.Environment;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * TASK:
 * Assuming Alice and Bob know a shared secret key, provide integrity to the channel
 * using HMAC implemted with SHA256. Then exchange ten messages between Alice and Bob.
 * <p>
 * https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/crypto/Cipher.html
 */
public class A1AgentCommunicationHMAC {
    public static void main(String[] args) throws Exception {
        /*
         * Alice and Bob share a secret session key that will be
         * used for hash based message authentication code.
         */
        final Key key = KeyGenerator.getInstance("HmacSHA256").generateKey();

        final Environment env = new Environment();

        env.add(new Agent("alice") {
            @Override
            public void task() throws Exception {
                final String text = "I hope you get this message intact. Kisses, Alice.";
                final byte[] pt = text.getBytes(StandardCharsets.UTF_8);
                final Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(key);
                for (int i = 0; i < 10; i++) {
                    final byte[] tag = mac.doFinal(pt);
                    print(hex(tag));
                    send("bob", pt);
                    send("bob", tag);
                }
            }
        });

        env.add(new Agent("bob") {
            @Override
            public void task() throws Exception {
                final Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(key);

                for (int i = 0; i < 10; i++) {
                    final byte[] ptRcv = receive("alice");
                    final byte[] tagRcv = receive("alice");
                    final byte[] tag = mac.doFinal(ptRcv);

                    print("Received message: " + new String(ptRcv));
                    if (verifyMac(tagRcv, tag, key)) {
                        print("✓ Valid as per integrity");
                    } else {
                        print("× Invalid as per integrity");
                    }
                }
            }
        });

        env.connect("alice", "bob");
        env.start();
    }

    public static boolean verifyMac(byte[] tag1, byte[] tag2, Key key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        /*
            FIXME: Defense #2

            The idea is to hide which bytes are actually being compared
            by MAC-ing the tags once more and then comparing those tags
         */
        final Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);

        final byte[] tagtag1 = mac.doFinal(tag1);
        final byte[] tagtag2 = mac.doFinal(tag2);

        return Arrays.equals(tagtag1, tagtag2);
    }
}
