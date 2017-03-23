#!/bin/bash

drop_broker(){
    echo "Drop broker from cluster for $time seconds"
    iptables -A OUTPUT -p tcp --dport $port -j DROP && iptables -A INPUT -p tcp --dport $port -j DROP
    sleep $time
    echo "Bring broker back to cluster"
    iptables -D OUTPUT -p tcp --dport $port -j DROP && iptables -D INPUT -p tcp --dport $port -j DROP
}

time=30
port=25672

while [[ $# > 1 ]]
do
    var="$1"
    case $var in
        -time)
        time=$2
	shift
        ;;
        -port)
        port=$2
	shift
        ;;
    esac
shift	
done

drop_broker
