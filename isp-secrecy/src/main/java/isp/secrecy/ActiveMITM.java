package isp.secrecy;

import fri.isp.Agent;
import fri.isp.Environment;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;

public class ActiveMITM {
    public static void main(String[] args) throws Exception {
        // David and FMTP server both know the same shared secret key
        final Key key = KeyGenerator.getInstance("AES").generateKey();

        final Environment env = new Environment();

        env.add(new Agent("david") {
            @Override
            public void task() throws Exception {
                final String message = "prf.denis@fri.si\n" +
                        "david@fri.si\n" +
                        "Some ideas for the exam\n\n" +
                        "Hi! Find attached <some secret stuff>!";


                final Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
                aes.init(Cipher.ENCRYPT_MODE, key);
                final byte[] pt = message.getBytes(StandardCharsets.UTF_8);
                print(hex(pt));
                final byte[] ct = aes.doFinal(message.getBytes(StandardCharsets.UTF_8));
                final byte[] iv = aes.getIV();
                print("sending: '%s' (%s)", message, hex(ct));
                send("server", ct);
                send("server", iv);
            }
        });

        env.add(new Agent("student") {
            @Override
            public void task() throws Exception {
                final byte[] bytes = receive("david");
                final byte[] iv = receive("david");
                print(" IN: %s", hex(bytes));
                // As the person-in-the-middle, modify the ciphertext
                // so that the FMTP server will send the email to you
                // (Needless to say, you are not allowed to use the key
                // that is being used by david and server.)

                // AES handles the text by splitting into 16-bytes block
                // first block includes email receiver, hereby original and new (desired) one are defined
                final byte[] orgReceiverBlock = "prf.denis@fri.si".getBytes(StandardCharsets.UTF_8);
                final byte[] newReceiverBlock = "isp21.@gmail.com".getBytes(StandardCharsets.UTF_8);
                final byte[] xorReceivers = new byte[16];
                // in decryption process, deciphered text is xor-ed with IV, which outputs plain text
                // knowing original plain text for the first block
                // I mark differing bits between original and desired plain text by xoring them (in xorReceivers)
                // and manipulate IV, so that when it is xored with the deciphered text, it will flip specific bits
                // producing the desired text
                for (int i = 0; i < 16; i++) {
                    xorReceivers[i] = (byte) (orgReceiverBlock[i] ^ newReceiverBlock[i]);
                    iv[i] = (byte) (xorReceivers[i] ^ iv[i]);
                }

                // Unfortunately, original receiver email: prf.denis@fri.si and new one defined in the task: isp21@gmail.com
                // have different number of bytes (original - 16, new one - 15)
                // at first I tried to somehow modify also ciphertext itself (first block of it, to influence the second block)
                // so I could use e.g. "isp21@gmail.com\n" and in the second block change "\ndavid..." to "david..."
                // but modifying the first block of cipher text damages the first block of deciphered text (since the key
                // is applied to different cipher text, and I couldn't find a way to correct it with IV
                // However, I'm most probably be missing something
                // Nevertheless, then I thought that in practice, sending email to gmail address offers some tricks that can be leveraged here,
                // like the one that gmail ignores periods "." in the address, so this way I increased size of new receiver to convenient 16 bytes
                // source: https://gmail.googleblog.com/2008/03/2-hidden-ways-to-get-more-from-your.html
                // Tricky as it is, this could work anyway :)
                // Hopefully you'll let us know how this should be solve properly, I'd be grateful for that
                // Kind regards, Adam Napieralski

                print("OUT: %s", hex(bytes));
                send("server", bytes);
                send("server", iv);
            }
        });

        env.add(new Agent("server") {
            @Override
            public void task() throws Exception {
                final byte[] ct = receive("david");
                final byte[] iv = receive("david");
                final Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
                aes.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
                final byte[] pt = aes.doFinal(ct);
                final String message = new String(pt, StandardCharsets.UTF_8);

                print("got: '%s' (%s)", message, hex(ct));
            }
        });

        env.mitm("david", "server", "student");
        env.start();
    }
}
