package isp.integrity;

import fri.isp.Agent;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class HMACExample {
    public static void main(String[] args) throws Exception {

        final String message = "We would like to provide data integrity for this message.";

        /*
         * STEP 1.
         * Select HMAC algorithm and get new HMAC object instance.
         * Standard Algorithm Names
         * http://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html
         */
        final Mac alice = Mac.getInstance("HmacSHA256");

        /*
         * STEP 1.
         * Alice and Bob agree upon a shared secret session key that will be
         * used for hash based message authentication code.
         */
        final Key key = KeyGenerator.getInstance("HmacSHA256").generateKey();

        /*
         * STEP 3.
         * Initialize HMAC and provide shared secret session key. Create an HMAC tag.
         */
        alice.init(key);
        final byte[] tag1 = alice.doFinal(message.getBytes(StandardCharsets.UTF_8));

        /*
         * STEP 4.
         * Print out HMAC.
         */
        final String messageHmacAsString = Agent.hex(tag1);
        System.out.println("HMAC: " + messageHmacAsString);

        /*
         * STEP 5.
         * Bob verifies the tag.
         */
        final Mac bob = Mac.getInstance("HmacSHA256");
        bob.init(key);
        final byte[] tag2 = bob.doFinal(message.getBytes(StandardCharsets.UTF_8));

        // Is the mac correct?

        // Never compare MACs this way
        System.out.println(verify1(tag1, tag2));

        // Better
        System.out.println(verify2(tag1, tag2));

        // Even better
        System.out.println(verify3(tag1, tag2, key));
    }

    public static boolean verify1(byte[] tag1, byte[] tag2) {
        /*
            FIXME: This is insecure
            - The comparison is done byte by byte
            - The comparator returns false immediately after the first inequality of bytes is found
            (Use CTRL+click and see how the  Arrays.equals() is implemented)
         */
        return Arrays.equals(tag1, tag2);
    }

    public static boolean verify2(byte[] tag1, byte[] tag2) {
        /*
            FIXME: Defense #1

            The idea is to compare all bytes

            Important: A "smart" compiler may try to optimize this code
            and end the loop prematurely and thus work against you ...
         */

        if (tag1 == tag2)
            return true;
        if (tag1 == null || tag2 == null)
            return false;

        int length = tag1.length;
        if (tag2.length != length)
            return false;

        // This loop never terminates prematurely
        byte result = 0;
        for (int i = 0; i < length; i++) {
            result |= tag1[i] ^ tag2[i];
        }
        return result == 0;
    }

    public static boolean verify3(byte[] tag1, byte[] tag2, Key key)
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
