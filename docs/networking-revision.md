# Networking revision

## Setting up machine
```
sudo apt update

sudo apt-get install openssh-client=1:8.2p1-4ubuntu0.2 openssh-server strongswan apache2 wireshark git curl
```
- hq: NAT Network: (first create a new one: Preferences > Networks); Internal Network: hq-net

- br: —//—, Internal Network: br-net


```
sudo nano /etc/netplan/01-network-manager-all.yaml

network:
  version: 2
  ethernets:
    enp0s3:
      dhcp4: true
      dhcp-identifier: mac
    enp0s8:
      addresses: [10.1.0.0/16]

sudo netplan apply
```

## VPN
```
sudo nano /etc/ipsec.conf

config setup

conn %default
        ikelifetime=60m
        keylife=20m
        rekeymargin=3m
        keyingtries=1
        keyexchange=ikev2
        authby=secret

conn net-net
        leftsubnet=10.1.0.0/16
        leftfirewall=yes
        leftid=@hq
        right=$BRANCH_IP (PUBLIC IP ADDR!!!)
        rightsubnet=10.2.0.0/16
        rightid=@branch
        auto=add


sudo nano /etc/ipsec.secrets

@hq @br : PSK "this_is_my_psk"


sudo ipsec restart

sudo ipsec up net-net

(debug)
sudo ipsec start --nofork
```

## SSH
Setup server:
```
sudo ssh-keygen -t ecdsa -f /etc/ssh/ssh_host_ecdsa_key
sudo ssh-keygen -t rsa   -f /etc/ssh/ssh_host_rsa_key
sudo ssh-keygen -t dsa   -f /etc/ssh/ssh_host_dsa_key
sudo ssh-keygen -t ed25519 -f /etc/ssh/ssh_host_ed25519_key
```

Setup client:
```
ssh-keygen -t ecdsa

ssh-copy-id isp@$SERVER
```

Allow only public-key auth:
```
sudo nano etc/ssh/sshd_config

PasswordAuthentication no

sudo service ssh restart
```