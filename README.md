# HTTPs intercept
A simple program that intercepts HTTPs traffic using Netty and BouncyCastle

## How?
The `certs.sh` script creates a CA certificate and a template certificate for the server.
The CA certificate is exported into a file `root.pem`.
This file can then be imported in trust stores of browsers or Operating Systems.

The Java program loads the keys and certificate and when it receives an HTTPs request,
it decrypts it, encrypts it again for the intended server and forwards it.
Responses from the server are also decrypted and re-encrypted before being sent to the client.
This is commonly known as a "Man in the Middle" attack.

Furthermore, the script `reroute.sh` can configure `iptables` to route all traffic of a specified user
through the program such that that traffic is intercepted.

## Why
I thought it might be interesting to possibly build a simple ad-blocker-like program
operating on the system-level by attacking TLS.

## Disclaimer
ONLY USE THIS TOOL FOR EDUCATIONAL PURPOSES!  
DO NOT USE IT ON DEVICES OF PEOPLE WITHOUT THEIR INFORMED CONSENT!

This program has capabilities that can be used to perform a Man-In-The-Middle-Attack on HTTPs traffic
allowing to read or modify all communication done with it.

Furthermore, it is dangerous to import CA certificates into the trust store of a browser or Operating System.  
If a malicous entity gets access to the key,
they can read or change most encrypted communication performed using the device trusting the certificate.
