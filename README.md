# HTTPs intercept
A simple program that intercepts HTTPs traffic using Netty and BouncyCastle

## Disclaimer
> [!Warning]
> ONLY USE THIS TOOL FOR EDUCATIONAL PURPOSES!  
> DO NOT USE IT ON DEVICES OF PEOPLE WITHOUT THEIR INFORMED CONSENT!

This program has capabilities that can be used to perform a Man-In-The-Middle-Attack on HTTPs traffic
allowing to read or modify all communication done with it.

Furthermore, it is dangerous to import CA certificates into the trust store of a browser or Operating System.  
If a malicous entity gets access to the key,
they can read or change most encrypted communication performed using the device trusting the certificate.

## How does it work?
The `certs.sh` script creates a CA certificate and exports it into the file `root.pem` and `root.crt`.
These files can then be imported in trust stores of browsers or Operating Systems.
Adding a CA certificate to the Linux trust store is explained [here](https://askubuntu.com/a/94861/966107) (though it would be necessary to use the `root.pem` here).

The Java program loads the keys and certificate and when it receives an HTTPs request,
it decrypts the request, encrypts it again for the specified server and forwards it.
Responses from the server are also decrypted and re-encrypted before being sent to the client.
This is commonly known as a "Man in the Middle" attack.

When an HTTPs request is received, the program uses the unencrypted Server Name Identification (SNI) part
of the Client Hello TLS packet in order to identify both the server to send it to
as well as the host to target when signing the generated certificate.

Furthermore, the script `reroute.sh` can configure `iptables` to route all traffic of a specified user
through the program such that that traffic is intercepted.

## Setup
- run the `certs.sh` script in order to generate a CA certificate.
  This generates the following files:
  - `root.pem` and `root.crt` containing the CA certificate
  - `interceptor.jks` containing the keystore with the private key and certificate - This file is PRIVATE
  - `.secrets` containing the passphrases for the keystore and private key - This file is PRIVATE
- Run the program (the main class is `io.github.danthe1st.httpsintercept.HttpsIntercept`)
  This requires the files `interceptor.jks` and `.secrets` to be located in the current working directory.
- The program should listen on port `1337` and forward requests to port `443`.
  In order to make a request to `example.com` using `curl`, the following command can be used:
  ```bash
  curl --connect-to example.com:1337:127.0.0.1 https://example.com:1337 --cacert root.pem
  ```
  If the certificate is installed into the operating system truststore, the argument `--cacert root.pem` is not necessary.

It is also possible to add the certificate to the truststore of a JDK using a command similar to the following:
```bash
sudo keytool -keystore $JAVA_HOME/lib/security/cacerts -import interceptCert -file root.crt
```

With this, it will be able to intercept HTTPs traffic from Java applications as long as
- the truststore is installed to the JVM used for running the application
  (installing the certificate to another truststore doesn't make this work)
- the application uses the default truststore

### Binaries
A sample binary is automatically build [with GitHub Actions](https://github.com/danthe1st/HTTPs-intercept/actions?query=branch%3Amaster)
when a commit is pushed.
The build script can be found in the file [.github/workflows/build.yml](./.github/workflows/build.yml).

#### building binaries

The command `mvn package` generates a JAR file at a location
similar to `target/https-intercept-VERSION-jar-with-dependencies.jar`.
This JAR file can be run using `java -jar`.

A native binary can also be built using `mvn -Pnative package`.
This requires GraalVM and [some additional prerequisites](https://www.graalvm.org/latest/reference-manual/native-image/#prerequisites).

In order to customize the image build, it is possible to supply extra arguments using the property `native.extraArgs`.
For example, the following command can be used to allow device-specific optimizations:
```bash
mvn package -Pnative -Dnative.extraArgs="--march=native"
```

### Systemd setup

It is possible to create a systemd service definition with this program similar to this

```
[Unit]
Description=HTTPs intercept: https://github.com/danthe1st/HTTPs-intercept

[Service]
Type=simple
ExecStartPre=+/home/HTTPS_INTERCEPT_USER/HTTPS_INTERCEPT_DIRECTORY/reroute.sh enable USER_TO_INTERCEPT
ExecStart=/home/HTTPS_INTERCEPT_USER/HTTPS_INTERCEPT_DIRECTORY/https-intercept
ExecStopPost=+/home/HTTPS_INTERCEPT_USER/HTTPS_INTERCEPT_DIRECTORY/reroute.sh disable USER_TO_INTERCEPT
Restart=on-failure
User=HTTPS_INTERCEPT_USER
WorkingDirectory=/home/HTTPS_INTERCEPT_USER/HTTPS_INTERCEPT_DIRECTORY

[Install]
WantedBy=multi-user.target
```

In this example
- before starting forwards all traffic of the user `USER_TO_INTERCEPT`
  is changed to be forwarded through the program for interception
  - This is done by executing the `reroute.sh` script as root which sets up corresponding rules using `iptables`
- The program is started as the user `HTTPS_INTERCEPT_USER`
- This assumes the following files to be located in `/home/HTTPS_INTERCEPT_USER/HTTPS_INTERCEPT_DIRECTORY`:
  - `reroute.sh`: The script configuring rules with `iptables`
  - `https-intercept`: The built binary or a script executing the program
  - `interceptor.jks` and `.secrets` as created by `certs.sh`

# Configuration
This program can be configured by creating a file called `intercept.yaml` in the working directory.
```yaml
ignoredHosts: []
preForwardRules: []
postForwardRules: []
```

## ignored hosts
Using the configuration entry `ignoredHosts`, one can configure hostnames that should not be intercepted.
Requests to these hosts are forwarded as-is without decrypting the request.
It is possible to specify hostnames that must match exactly, parts of the hierarchy or regexes:
```yaml
ignoredHosts:
  exact:
  # This matches example.com but not somesubdomain.example.com
  - example.com
  partial:
  # This matches example.com and all subdomains
  - example.com
  # This matches all hosts ending with `.local`
  - local
  regex:
  # This matches all domains matching the given regex, e.g. example.com or example.org but not host.example.com
  - example\\.[a-zA-Z]*
```

## pre-forward rules
With the entry `preForwardRules`, it is possible to configure rules that happen before forwarding the request.
Pre-forward rules can access the request but not the response.
The rules are identified using a `type` parameter.
All rules have a `hostMatcher` parameter configuring in the same format as `ignoredHosts` which hosts they should apply to.

Currently, the only implemented rule (`setHeader`) allows setting HTTP request headers.
```yaml
preForwardRules:
# set a header for all requests
# in this case, we are setting the "Referer" header to "localhost"
- type: setHeader
  headers:
    Referer: localhost
# set a header for requests to example.com
- type: addHeader
  hostMatcher:
    exact:
    - example.com
  headers:
    X-My-Custom-Header: some-header-value
```

## post-forward rules
Similarlarly to `preForwardRules`, `postForwardRules` can be used to configure rules that are processed after forwarding the request.
These rules can access and modify the response.
As with Pre-forward rules, Post-forward rules are identified with a `type` parameter and can be filtered using a `ignoredHosts` specification.

Currently, the only implemented rule (`htmlBasedBlock`) blocks responses based on the HTML response.
This rule checks whether an element matching a CSS selector exists matching a regex.
```yaml
# This rule attempts to block rick rolls based on the <title> element
# it is not very reliable as rickrolls may have different <title>s and YouTube may load it in different ways
- type: htmlBasedBlock
  # This rule only applies to YouTube and its subdomains
  hostMatcher:
    partial:
    - youtube.com
  # if a <title> containing the text "Never gonna give you up" is found, the response is not sent to the client
  selector: title
  matcher: .*Never gonna give you up.*
  # it is possible to set a custom HTTP status for the blocked response
  status: 500
  # a path to an HTML file containing the new response can be specified as follows
  responsePath: /path/to/block/information.html
```
