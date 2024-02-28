#!/bin/bash
set -e

./certs.sh

./target/https-intercept > intercept.log 2>&1 &
sleep 1
interceptPID="$!"

curl_result="$(curl --connect-to example.com:1337:127.0.0.1 https://example.com:1337 --cacert root.pem -i)"

kill "$interceptPID"

statusCode="$(echo "$curl_result" | head -n 1 | cut -d' ' -f2)"

# assert successful response
if [ "$statusCode" -ne 200 ]; then
	echo "status code does not match!"
	exit 1
fi

# assert text Example Domain in response
echo "$curl_result" | grep "Example Domain"

cat intercept.log
rm intercept.log