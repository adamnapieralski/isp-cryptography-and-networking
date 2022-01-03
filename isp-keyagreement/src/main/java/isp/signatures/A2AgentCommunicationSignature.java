package isp.signatures;

import fri.isp.Agent;
import fri.isp.Environment;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;

/*
 * Assuming Alice and Bob know each other's public key, provide integrity and non-repudiation
 * to exchanged messages with ECDSA. Then exchange ten signed messages between Alice and Bob.
 */
public class A2AgentCommunicationSignature {
    public static void main(String[] args) throws Exception {
        final Environment env = new Environment();

        final String signingAlgorithm = "SHA256withECDSA";
        final String keyAlgorithm = "EC";


        // Create key pairs
        final KeyPair keyAlice = KeyPairGenerator.getInstance(keyAlgorithm).generateKeyPair();
        final KeyPair keyBob = KeyPairGenerator.getInstance(keyAlgorithm).generateKeyPair();



        env.add(new Agent("alice") {
            @Override
            public void task() throws Exception {
                // create a message, sign it,
                // and send the message, signature pair to bob
                // receive the message signarure pair, verify the signature
                // repeat 10 times

                final Signature signer = Signature.getInstance(signingAlgorithm);
                final Signature verifier = Signature.getInstance(signingAlgorithm);

                signer.initSign(keyAlice.getPrivate());
                verifier.initVerify(keyBob.getPublic());

                for (int i = 0; i < 10; i++) {
                    final String message = String.format("Hi Bob! This is my message %d, for which I take full responsibility", i);
                    final byte[] messageBt = message.getBytes(StandardCharsets.UTF_8);
                    signer.update(messageBt);

                    final byte[] signature = signer.sign();
                    print("Signature: " + Agent.hex(signature));
                    send("bob", messageBt);
                    send("bob", signature);

                    final byte[] msgRcv = receive("bob");
                    final byte[] signatureRcv = receive("bob");

                    // Check whether the signature is valid
                    verifier.update(msgRcv);

                    if (verifier.verify(signatureRcv))
                        print("Bob's signature is valid");
                    else
                        print("Bob's signature invalid.");
                }
            }
        });

        env.add(new Agent("bob") {
            @Override
            public void task() throws Exception {
                final Signature signer = Signature.getInstance(signingAlgorithm);
                final Signature verifier = Signature.getInstance(signingAlgorithm);

                signer.initSign(keyBob.getPrivate());
                verifier.initVerify(keyAlice.getPublic());

                for (int i = 0; i < 10; i++) {
                    final byte[] msgRcv = receive("alice");
                    final byte[] signatureRcv = receive("alice");

                    verifier.update(msgRcv);

                    if (verifier.verify(signatureRcv))
                        print("Alice's signature is valid");
                    else
                        print("Alice's signature invalid.");

                    final String message = String.format("Hello Alice for the %d time!", i);
                    final byte[] messageBt = message.getBytes(StandardCharsets.UTF_8);
                    signer.update(messageBt);

                    final byte[] signature = signer.sign();
                    print("Signature: " + Agent.hex(signature));
                    send("alice", messageBt);
                    send("alice", signature);
                }
            }
        });

        env.connect("alice", "bob");
        env.start();
    }
}