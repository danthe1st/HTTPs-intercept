# HTTPs intercept
A simple program that intercepts HTTPs traffic using Netty and BouncyCastle

## Disclaimer
ONLY USE THIS TOOL FOR EDUCATIONAL PURPOSES!  
DO NOT USE IT ON DEVICES OF PEOPLE WITHOUT THEIR INFORMED CONSENT!

This program has capabilities that can be used to perform a Man-In-The-Middle-Attack on HTTPs traffic
allowing to read or modify all communication done with it.

Furthermore, it is dangerous to import CA certificates into the trust store of a browser or Operating System.  
If a malicous entity gets access to the key,
they can read or change most encrypted communication performed using the device trusting the certificate.

## How?
The `certs.sh` script creates a CA certificate and a template certificate for the server.
The CA certificate is exported into a file `root.pem`.
This file can then be imported in trust stores of browsers or Operating Systems.
Adding a CA certificate to the Linux trust store is explained [here](https://askubuntu.com/a/94861/966107).

The Java program loads the keys and certificate and when it receives an HTTPs request,
it decrypts it, encrypts it again for the intended server and forwards it.
Responses from the server are also decrypted and re-encrypted before being sent to the client.
This is commonly known as a "Man in the Middle" attack.

When an HTTPs request is received, the program uses the unencrypted Server Name Identification (SNI) part
of the Client Hello TLS packet in order to identify both the server to send it to
as well as the host to target when signing the generated certificate.

Furthermore, the script `reroute.sh` can configure `iptables` to route all traffic of a specified user
through the program such that that traffic is intercepted.

## Why
I thought it might be interesting to possibly build a simple ad-blocker-like program
operating on the system-level by attacking TLS.

## Testing Setup
- run the `certs.sh` script in order to generate a CA and template server certificate.
  This generates the following files:
  - `root.pem` and `root.crt` containing the CA certificate
  - `interceptor.jks` containing the keystore with the private keys and certificates - This file is PRIVATE
  - `.secrets` containing the passphrases for the keystore and private keys - This file is PRIVATE
- Run the program (the main class is `io.github.danthe1st.httpsintercept.HttpsIntercept`)
  This requires the files `interceptor.jks` and `.secrets` to be located in the current working directory.
- The program should listen on port `1337` and forward requests to port `443`.
  In order to make a request to `example.com` using `curl`, the following command can be used:
```bash
curl --connect-to example.com:1337:127.0.0.1 https://example.com:1337 --cacert root.pem
```