#!/bin/bash
set -e

./target/https-intercept > intercept.log 2>&1 &
interceptPID="$!"

sleep 2
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

# assert logs
grep "Received request: GET example.com/" < intercept.log > /dev/null

cat intercept.log
rm intercept.log

echo "successfully verified executable"
