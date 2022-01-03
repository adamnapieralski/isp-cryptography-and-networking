package isp.keyagreement;

import fri.isp.Agent;
import fri.isp.Environment;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.X509EncodedKeySpec;

public class AgentCommunicationECDH {
    public static void main(String[] args) {

        final Environment env = new Environment();

        env.add(new Agent("alice") {
            @Override
            public void task() throws Exception {
                final KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
                kpg.initialize(256);

                // Generate key pair
                final KeyPair keyPair = kpg.generateKeyPair();

                // send "PK" to bob ("PK": A = g^a, "SK": a)
                send("bob", keyPair.getPublic().getEncoded());
                print("My contribution to ECDH: %s", hex(keyPair.getPublic().getEncoded()));

                // get PK from bob
                final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(receive("bob"));
                final ECPublicKey bobPK = (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(keySpec);

                // Run the agreement protocol
                final KeyAgreement dh = KeyAgreement.getInstance("ECDH");
                dh.init(keyPair.getPrivate());
                dh.doPhase(bobPK, true);

                // generate a shared AES key
                final byte[] sharedSecret = dh.generateSecret();
                print("Shared secret: %s", hex(sharedSecret));

                // By default the shared secret will be 32 bytes long,
                // our cipher requires keys of length 16 bytes
                final SecretKeySpec aesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");

                final Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
                aes.init(Cipher.ENCRYPT_MODE, aesKey);

                final byte[] ct = aes.doFinal("Hey Bob!".getBytes(StandardCharsets.UTF_8));
                final byte[] iv = aes.getIV();

                send("bob", iv);
                send("bob", ct);

                print("I'm, done!");
            }
        });

        env.add(new Agent("bob") {
            @Override
            public void task() throws Exception {
                // get PK from alice
                final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(receive("alice"));
                final ECPublicKey alicePK = (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(keySpec);

                final ECParameterSpec dhParamSpec = alicePK.getParams();

                // create your own DH key pair
                final KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
                kpg.initialize(dhParamSpec);
                final KeyPair keyPair = kpg.generateKeyPair();
                send("alice", keyPair.getPublic().getEncoded());
                print("My contribution to ECDH: %s", hex(keyPair.getPublic().getEncoded()));

                final KeyAgreement dh = KeyAgreement.getInstance("ECDH");
                dh.init(keyPair.getPrivate());
                dh.doPhase(alicePK, true);

                final byte[] sharedSecret = dh.generateSecret();
                print("Shared secret: %s", hex(sharedSecret));
                final SecretKeySpec aesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");

                final Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
                final byte[] iv = receive("alice");
                final byte[] ct = receive("alice");
                aes.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
                final byte[] pt = aes.doFinal(ct);

                print("I got: %s", new String(pt, StandardCharsets.UTF_8));
            }
        });

        env.connect("alice", "bob");
        env.start();
    }
}
