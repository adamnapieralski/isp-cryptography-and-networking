package isp.integrity;

import fri.isp.Agent;
import fri.isp.Environment;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * TASK:
 * Assuming Alice and Bob know a shared secret key, secure the channel using a
 * AES in GCM. Then exchange ten messages between Alice and Bob.
 * <p>
 * https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/crypto/Cipher.html
 */
public class A2AgentCommunicationGCM {
    public static void main(String[] args) throws Exception {
        /*
         * Alice and Bob share a secret session key that will be
         * used for AES in GCM.
         */
        final Key key = KeyGenerator.getInstance("AES").generateKey();

        final Environment env = new Environment();

        env.add(new Agent("alice") {
            @Override
            public void task() throws Exception {
                final String text = "I hope you get this message intact and in secret. Kisses, Alice.";
                final byte[] pt = text.getBytes(StandardCharsets.UTF_8);

                final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

                for (int i = 0; i < 10; i++) {
                    cipher.init(Cipher.ENCRYPT_MODE, key);

                    final byte[] ct = cipher.doFinal(pt);
                    final byte[] iv = cipher.getIV();
                    print("CT: " + hex(ct));
                    print("IV: " + hex(iv));
                    send("bob", ct);
                    send("bob", iv);
                }
            }
        });

        env.add(new Agent("bob") {
            @Override
            public void task() throws Exception {
                final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

                for (int i = 0; i < 10; i++) {
                    final byte[] ctRcv = receive("alice");
                    final byte[] iv = receive("alice");
                    final GCMParameterSpec specs = new GCMParameterSpec(128, iv);
                    cipher.init(Cipher.DECRYPT_MODE, key, specs);
                    final byte[] ptRcv = cipher.doFinal(ctRcv);
                    print(new String(ptRcv));
                }
            }
        });

        env.connect("alice", "bob");
        env.start();
    }
}
