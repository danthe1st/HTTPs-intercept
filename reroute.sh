#!/bin/bash

# route all HTTPs traffic of a specific user to this program
# the program must be running under a different user
# USAGE: reroute.sh enable|disable userToIntercept

if [ -z "$2" ]; then
	echo "$0 enable|disable userToIntercept" >/dev/stderr
	exit 1
fi

if [ "$1" = "enable" ]; then
	action="-A"
elif [ "$1" = "disable" ]; then
	action="-D"
else
	echo "$0 enable|disable userToIntercept" >/dev/stderr
	exit 1
fi

interface="$(ip -4 route show default|cut -d' ' -f5)"
targetPort=443
trackerPort=1337
targetUser="$2"
targetUserId="$(id --user "$targetUser")"

markId=1337

iptables -t mangle "$action" OUTPUT -p tcp -m owner --uid-owner $targetUserId --dport $targetPort -j LOG --log-prefix='[inspector-MARK]'
iptables -t mangle "$action" OUTPUT -p tcp -m owner --uid-owner $targetUserId --dport $targetPort -j MARK --set-mark "$markId"
iptables -t nat "$action" OUTPUT -p tcp --dport "$targetPort" -m mark --mark "$markId" -j LOG --log-prefix='[inspector-REROUTE]'
iptables -t nat "$action" OUTPUT -p tcp --dport "$targetPort" -m mark --mark "$markId" -j DNAT --to-destination "127.0.0.1:$trackerPort"

# log can be viewed (by default) using the following command:
# tail -f /var/log/kern.log|grep inspector