#!/bin/bash

KEYSTORE_NAME="interceptor.jks"
# https://stackoverflow.com/a/44377013/10871900
# for now, the same passphrase is used for both the key and the keystore
keystorePassphrase="$(tr -dc 'A-Za-z0-9!?%=' < /dev/urandom | head -c 45)"
keyPassphrase="$keystorePassphrase"
ROOT_CERT_FILE="root.crt"
ROOT_PEM_FILE="root.pem"

certTarget="localhost"
SERVER_CN="template"

rm "$KEYSTORE_NAME"
rm "$ROOT_CERT_FILE"
rm "$ROOT_PEM_FILE"

set -e


# create CA
keytool -keystore "$KEYSTORE_NAME" -storepass "$keystorePassphrase" -alias root -dname "cn=RootCA, ou=Root_CertificateAuthority, o=CertificateAuthority, c=AT" -genkeypair -keyalg RSA -ext bc:c
chmod 600 "$KEYSTORE_NAME"

# export root CA so that it can be imported into browsers/the OS
keytool -keystore "$KEYSTORE_NAME" -storepass "$keystorePassphrase" -export -alias root > "$ROOT_CERT_FILE"
openssl x509 -in "$ROOT_CERT_FILE" -out "$ROOT_PEM_FILE" -outform PEM

# export passphrase
echo "$keystorePassphrase" > .secret
echo "$keyPassphrase" >> .secret
chmod 600 .secret
