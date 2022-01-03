# Exercise: SSH protocol

## Preparation

Start the base image and install the required software. We will require the following packages:

*   openssh-server,
*   openssh-client,
*   wireshark,
*   apache2,
*   curl.

Run `sudo apt update` to update the package list from repositories. Then install the required software with `sudo apt install <list-the-packages>`. Note that while installing the wireshark, when asked `Should non-superusers be able to capture packets?` select `yes`. (If you make a mistake and select `no`, you can change your selection by running `sudo dpkg-reconfigure wireshark-common`.) Then add your user to the group `wireshark` with the following command: `sudo usermod -a -G wireshark $USER`. Shutdown the virtual machine.

Clone the base image twice (use linked clone and regenerate the MAC address) and name the new machines `ssh-server` and `ssh-client`.

Configure both machines to use a single network interface card (NIC): Disable (if they are not already disabled) `Adapter 2`, `Adapter 3` and `Adapter 4` in `Machine > Settings > Network`, and make sure that `Adapter 1` is enabled. You may put both machines either on `Bridged` or in the same `NAT network`.

Assert that machines can ping each other.

### Machine `ssh-server`

As part of the set-up, let's first change the hostname of this machine to `ssh-server`. (This is not related to SSH, but it will help us to know more easily which machine we are using when working in the terminal.)

First, open file `/etc/hosts` and add the following line: `127.0.1.1 ssh-server`. Save the file. Next, run `sudo hostnamectl set-hostname ssh-server`. To apply the changes, restart the terminal. Observe that now each line in the terminal starts with `isp@ssh-server`.

Now, let's regenerate the SSH server keys for this machine. Make sure you provide an empty passphrase when asked. (Can you think of a problem that could occur, if you provide a passphrase?). Name the keys according to `HostKey` directive in `/etc/ssh/sshd_config` file. (Note, generally you'd need only a single key -- I recommend using [Ed25519](https://en.wikipedia.org/wiki/Curve25519) -- but let's demonstrate the use of various keys.)

    sudo ssh-keygen -t ecdsa -f /etc/ssh/ssh_host_ecdsa_key
    sudo ssh-keygen -t rsa   -f /etc/ssh/ssh_host_rsa_key
    sudo ssh-keygen -t dsa   -f /etc/ssh/ssh_host_dsa_key
    sudo ssh-keygen -t ed25519 -f /etc/ssh/ssh_host_ed25519_key

## Assignments

I'll assume that the IP of the `ssh-client` is `$CLIENT` and the IP of the `ssh-server` is `$SERVER`. When running the commands below, substitute the the variable names with appropriate values.

### Username/Password client authentication, server authentication

*   On `ssh-client`, use the terminal to connect to the server: `ssh isp@$SERVER`
*   Since you are connecting for the first time, you have to authenticate the server's public key. Does the public-key fingerprint match the actual fingerprint on the remote machine? To check the fingerprint, go to the next step.
*   To find out the server's public key fingerprint, switch to the `ssh-server` machine. The fingerprint depends on the type of the key that the server is using. Issue one of the following commands on the server to see the public key fingerprint. (Note that this is something you'd usually do before you'd connect to the server for the first time.)
    *   For ECDSA key: `ssh-keygen -lf /etc/ssh/ssh_host_ecdsa_key.pub`
    *   For ED25519 key: `sh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub`
    *   For RSA key: `ssh-keygen -lf /etc/ssh/ssh_host_rsa_key.pub`
    *   For DSA key: `ssh-keygen -lf /etc/ssh/ssh_host_dsa_key.pub`
*   **Question 1.** Switch back to the `ssh-client` and make sure that the displayed fingerprint is correct. Obviously, the **authentication should fail if fingerprints mismatch.** If that were the case, what kind of an attack could be taking place?
*   Switch to `ssh-client` and input `yes`, if the displayed fingerprint matches the actual one. Finally, provide the password (`ssh-client`) and you should be logged in. Observe how the terminal input line changed from `isp@isp` to `isp@ssh-server` when you connected: this let's you know that your terminal is now connected to the `ssh-server`. Log-out by either inputting `exit`, `logout`, or by pressing `ctrl+d`.
*   Now let's change the SSH keypairs on the `ssh-server`:
    *   `sudo ssh-keygen -t ecdsa -f /etc/ssh/ssh_host_ecdsa_key`
    *   `sudo ssh-keygen -t rsa -f /etc/ssh/ssh_host_rsa_key`
    *   `sudo ssh-keygen -t dsa -f /etc/ssh/ssh_host_dsa_key`
    *   `sudo ssh-keygen -t ed25519 -f /etc/ssh/ssh_host_ed25519_key`
*   On `ssh-client`, reconnect to the `ssh-server`. You should get an flashy warning. What does it mean?
*   On `ssh-client`, remove the saved fingerprints from `~/.ssh/known_hosts` by running the command that the SSH client suggests and reconnect to the server.

Did you remember to authenticate the server's fingerprint or you simply input `yes` when asked?

### Authenticating the client with its public key

*   On `ssh-client`, regenerate user's `ssh-client` SSH keys. Note that sice we are generating client SSH keys, which are stored inside user `ssh-client` home folder (`~/.ssh` and not in `/etc/ssh` directory), you should run the following commands as a normal user (not with `sudo`):
    *   `ssh-keygen -t rsa`
    *   `ssh-keygen -t dsa`
    *   `ssh-keygen -t ecdsa`
*   On `ssh-client`, connect onto `ssh-server` and try to authenticate yourself your public key. Note that if the server asks for your password, the public-key login failed and the server failed-over to plain username-password authentication: `ssh -i ~/.ssh/id_rsa isp@$SERVER`. (As a useful debugging note, running all ssh commands with `-v` switch turns on the verbose mode which prints out useful information.)
*   To enable public key authentication, you have to (1) copy your public key to the remote computer and then (2) enable and link it to specific account. Both actions can be done with `ssh-copy-id` which copies public key to the chosen account and adds public key to authorized keys list. Simply run: `ssh-copy-id isp@$SERVER`.
*   Once the key has been copied and added to the authorized_keys list, try connecting and authenticating using only public keys: `ssh $SERVER`. You should now login to server without providing password. (We can even omit the username, since the username on the server and on the client are the same.)
*   Finally, let's disable password-based login attempts and always require client authentication with public keys. On the `ssh-server`, open file `/etc/ssh/sshd_config` and add command `PasswordAuthentication no`. Save the file and restart the SSH server with `sudo service ssh restart`.

    Because we have already copied our public key to the server, our client will by default try to authenticate itself with the public key. So we have to explicitly state that we want to authenticate with the username/password pair, if we want to test the most recent change. Run the following on `ssh-client`: `ssh -o PreferredAuthentications=password -o PubkeyAuthentication=no $SERVER`. If you configured the sever correctly, the connection attempt should be rejected.

### Tunneling with SSH

Imagine that you'd like to access a service on a remote machine, but that service is configured to allow `localhost` connections only. To alleviate such limitations, we can use SSH to create a _tunnel_ between our machine and the machine running the service and then access the service as if it was running on our machine.

The SSH will instantiate a local network socket that will forward all traffic to the service socket on the remote machine. To the service on the remote machine, the tunnel will be completely transparent: all requests that are sent through the tunnel will appear to the service as normal requests originating from `localhost`.

To make this example more concrete, let's configure the Apache webserver on the `ssh-server` to allow `localhost` connections only. Open file `/etc/apache2/sites-available/000-default.conf` and add the following snippet that limits the access to the Apache served pages:

    <Directory /var/www/html>
        Require ip 127.0.0.1/8
    </Directory>

Reload the Apache configuration file with `sudo service apache2 reload`.

If you access the apache on the server, the access should be granted. Try by running `curl localhost`.

If you do the same on the `ssh-client` (you have to replace `localhost` with ip of the server), you should get an HTML page saying that the access is not allowed.

Now, let's circumvent this access control by accessing the Apache server from the `ssh-client` with the help of a SSH tunnel.

On `ssh-client`, set up a tunnel by issuing `ssh -L 127.0.0.1:8080:127.0.0.1:80 -N $SERVER`

The `-L` switch denotes local port-forwarding and the `-N` prevents executing remote commands; this is useful for only setting up forwarded ports and not actually running terminal on the remote machine.

After you have run the command, open a new terminal (the current one is ssh'd to the server), and run `curl localhost:8080`. (Alternatively, open Firefox in `Private browsing (ctrl+shift+p)` mode and navigate to `http://localhost:8080`; private browsing mode is needed to disable caching, which may interfere with testing.) You are now able to access the Apache2 on the server machine, as if you were physically on the server machine. The only catch is that you have to explicitly specify the tunneled port number.

**Question 2.** Open another terminal on the `ssh-server` and observe Apache access log as you request pages on the isp machine with `curl localhost:8080`. You can see the real-time access log by running `tail -f /var/log/apache2/access.log`.

What is the IP address of the client that is issuing the HTTP requests? Why? (Press `ctrl+c` to exit the tail program.)

### Reverse SSH Tunneling

A reverse SSH tunnel is similar to a normal SSH tunnel, the difference is in the agent that initiates the tunnel. In a reverse SSH tunnel, the machine that provides the service is also the machine that sets up the tunnel. (Contrary to the local port-forwarding where the tunnel was set up by the machine that consumed the provided service.)

Using SSH, the computer that provides the service can establish a remote channel that forwards a given port on the service-providing machine to a local port on the service-consuming machine.

Again, to make the example more concrete, let's configure the `ssh-server` machine to use a firewall that blocks all traffic except outgoing SSH connections. As before, we'll have to disable IPv6\. To disable IPv6, open file `/etc/sysctl.conf` and add the following lines at the end of the file:

    net.ipv6.conf.all.disable_ipv6 = 1
    net.ipv6.conf.default.disable_ipv6 = 1
    net.ipv6.conf.lo.disable_ipv6 = 1

Activate changes by running `sudo sysctl -p`. The terminal should output the lines you added in the previous step. You should run this command each time you start up the image; IPv6 turns on by default at start.

Next, you may reuse the iptables script from the previous week's lab session. Modify the script to contain the following entries:

    iptables -A INPUT -i lo -j ACCEPT
    iptables -A OUTPUT -o lo -j ACCEPT
    iptables -A INPUT  -m state --state ESTABLISHED,RELATED -j ACCEPT
    iptables -A OUTPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
    iptables -A OUTPUT  -p tcp --dport 22 -m state --state NEW -j ACCEPT

Additionally, remove the Apache access control that we added in the previous assignment by commenting out (or deleting) the following lines in `/etc/apache2/sites-available/000-default.conf`:

    <Directory /var/www/html>
        Require ip 127.0.0.1/8
    </Directory>

Remember to reload the configuration once you have changed the file: `sudo service apache2 reload`.

At this point, you should be able to `curl localhost` on the `ssh-server` machine, while a `curl $SERVER` run on the `ssh-client` should fail. Moreover, you should also be unable to ssh into server from `ssh-client`; the firewall should block both HTTP and SSH access from the outside.

Now, the `ssh-server` machine is allowed to connect onto `ssh-client` and establish a reverse tunnel that will allow `ssh-client` to access the Apache pages on `ssh-server`. On the `ssh-server`, run the following:

`ssh -R 127.0.0.1:8080:127.0.0.1:80 -N isp@$CLIENT`

With the reverse tunnel set up, you should be able to `curl localhost:8080` on the `ssh-client` and access the contents of the Apache server pages on the `ssh-server` machine.

### Assignments

*   Use wireshark to see the messages that get exchanged during the communication set up. (If you are unable to see network interfaces when you run wireshark, you most likely did not configure it properly. You can either go through the configuration part (described at the beginning) again or run wireshark with `sudo`.)
*   Explore programs `scp` and `rsync`. A common usecase for SSH is to transfer files between machines. Learn about the following commands and use them to copy files between `ssh-client` and `ssh-server`:
    *   [scp](http://linux.die.net/man/1/scp) -- secure copy (remote file copy program)
    *   [rsync](http://linux.die.net/man/1/rsync) -- a fast, versatile, remote (and local) file-copying tool

## References

*   [SSH: The Secure Shell: The Definitive Guide](http://docstore.mik.ua/orelly/networking_2ndEd/ssh)
*   [http://www.sfu.ca/~dgnapier/ssha.pdf](http://www.sfu.ca/~dgnapier/ssha.pdf)

