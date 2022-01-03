package isp.keyagreement;

import fri.isp.Agent;
import fri.isp.Environment;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.X509EncodedKeySpec;

public class AgentCommunicationDH {
    public static void main(String[] args) {

        final Environment env = new Environment();

        env.add(new Agent("alice") {
            @Override
            public void task() throws Exception {
                final KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
                kpg.initialize(2048);

                // Generate key pair
                final KeyPair keyPair = kpg.generateKeyPair();

                // send "PK" to bob ("PK": A = g^a, "SK": a)
                send("bob", keyPair.getPublic().getEncoded());
                print("My contribution: A = g^a = %s",
                        hex(keyPair.getPublic().getEncoded()));

                // get PK from bob
                final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(receive("bob"));
                final DHPublicKey bobPK = (DHPublicKey) KeyFactory.getInstance("DH")
                        .generatePublic(keySpec);

                // Run the agreement protocol
                final KeyAgreement dh = KeyAgreement.getInstance("DH");
                dh.init(keyPair.getPrivate());
                dh.doPhase(bobPK, true);

                // generate a shared AES key
                final byte[] sharedSecret = dh.generateSecret();
                print("Shared secret: g^ab = B^a = %s", hex(sharedSecret));

                // By default the shared secret will be 32 bytes long,
                // but our cipher requires keys of length 16 bytes
                // IMPORTANT: It is safer to not create the session key directly from
                // the shared secret, but derive it using key derivation function
                // (will be covered later)
                final SecretKeySpec aesKey = new SecretKeySpec(sharedSecret,
                        0, 16, "AES");

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
                final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(
                        receive("alice"));
                final DHPublicKey alicePK = (DHPublicKey) KeyFactory.getInstance("DH")
                        .generatePublic(keySpec);

                final DHParameterSpec dhParamSpec = alicePK.getParams();

                // create your own DH key pair
                final KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
                kpg.initialize(dhParamSpec);
                final KeyPair keyPair = kpg.generateKeyPair();
                send("alice", keyPair.getPublic().getEncoded());
                print("My contribution: B = g^b = %s",
                        hex(keyPair.getPublic().getEncoded()));

                final KeyAgreement dh = KeyAgreement.getInstance("DH");
                dh.init(keyPair.getPrivate());
                dh.doPhase(alicePK, true);

                final byte[] sharedSecret = dh.generateSecret();
                print("Shared secret: g^ab = A^b = %s", hex(sharedSecret));
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
