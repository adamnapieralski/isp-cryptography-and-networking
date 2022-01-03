package isp.integrity;

import fri.isp.Agent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageDigestExample {

    public static void main(String[] args) throws NoSuchAlgorithmException {

        final String message = "We would like to provide data integrity.";

        /*
         * STEP 1.
         * Select Message Digest algorithm and get new Message Digest object instance
         * http://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html
         */
        final MessageDigest digestAlgorithm = MessageDigest.getInstance("SHA-256");

        /*
         * STEP 2.
         * Create new hash using message digest object.
         */
        final byte[] hashed = digestAlgorithm.digest(message.getBytes(StandardCharsets.UTF_8));

        /*
         * STEP 4: Print out hash. Note we have to convert a byte array into
         * hexadecimal string representation.
         */
        final String hashAsHex = Agent.hex(hashed);
        System.out.println(hashAsHex);
    }
}
