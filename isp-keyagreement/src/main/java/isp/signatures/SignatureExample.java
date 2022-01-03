package isp.signatures;

import fri.isp.Agent;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;

public class SignatureExample {
    public static void main(String[] args) throws Exception {

        // https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#Signature
        final String signingAlgorithm =
                "SHA256withRSA";
//         "SHA256withDSA";
//        "SHA256withECDSA";
        final String keyAlgorithm =
                "RSA";
//         "DSA";
//         "EC";


        // The message we want to sign
        final String document = "We would like to sign this.";

        /*
         * STEP 1.
         * We create a public-private key pair using standard algorithm names
         * http://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html
         */
        final KeyPair key = KeyPairGenerator.getInstance(keyAlgorithm).generateKeyPair();

        /*
         * Alice creates Signature object defining Signature algorithm.
         */
        final Signature signer = Signature.getInstance(signingAlgorithm);

        /*
         * We initialize the signature object with
         * - Operation modes (SIGN) and
         * - provides appropriate ***Private*** Key
         */
        signer.initSign(key.getPrivate());

        // Finally, we load the document into the signature object and sign it
        signer.update(document.getBytes(StandardCharsets.UTF_8));
        final byte[] signature = signer.sign();
        System.out.println("Signature: " + Agent.hex(signature));

        /*
         * To verify the signature, we create another signature object
         * and specify its algorithm
         */
        final Signature verifier = Signature.getInstance(signingAlgorithm);

        /*
         * We have to initialize in the verification mode. We only need
         * to know public key of the signer.
         */
        verifier.initVerify(key.getPublic());

        // Check whether the signature is valid
        verifier.update(document.getBytes(StandardCharsets.UTF_8));

        if (verifier.verify(signature))
            System.out.println("Valid signature.");
        else
            System.err.println("Invalid signature.");
    }
}
