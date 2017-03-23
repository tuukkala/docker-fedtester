#!/bin/bash

federate(){
    upstream_cluster_hosts=""
    fed_hosts=$feds
    fed_path="/${VHOST}?heartbeat=10&connection_timeout=60"
    fed_hosts=$feds
    oifs=$IFS
    IFS=';'
    for fed in $UPSTREAM_HOSTS; do            
        if [ -n "$upstream_cluster_hosts" ] ; then
            upstream_cluster_hosts+=","
        fi
        upstream_cluster_hosts+="\"amqp://${CLUSTER_USER}:${CLUSTER_PASSWD}@${fed}${fed_path}\""
    done
    IFS=$oifs            
    federation_upstream_json='{
"uri":['"$upstream_cluster_hosts"'],
"ack-mode":"no-ack",
"trust-user-id":true,
"message-ttl":1000,
"max-hops":3,
"expires":10000
}'      
    rabbitmqctl set_parameter -p $VHOST federation-upstream "federation" "$federation_upstream_json"
    
    policy_json='{
"federation-upstream-set":"all"
}'      
    rabbitmqctl set_policy -p $VHOST --apply-to exchanges "federate" ".*" "$policy_json"    
}

hostname=`hostname`
RABBITMQ_NODENAME=${RABBITMQ_NODENAME:-rabbit}

rabbitmq-plugins enable rabbitmq_federation --offline
rabbitmq-plugins enable rabbitmq_federation_management --offline

echo "Start server"
/usr/sbin/rabbitmq-server -detached
sleep 20

#rabbitmqctl wait /var/lib/rabbitmq/mnesia/$RABBITMQ_NODENAME\@$HOSTNAME.pid

if [ -z "$CLUSTER_WITH" -o "$CLUSTER_WITH" = "$hostname" ]; then
    echo "Create admin user"    
    if [ -z "$CLUSTER_USER" ]; then
        echo "User is required."
        exit 255
    fi
    if [ -z "$CLUSTER_PASSWD" ]; then
        echo "Password is required."
        exit 255
    fi
    
    rabbitmqctl add_user $CLUSTER_USER $CLUSTER_PASSWD
    rabbitmqctl set_user_tags $CLUSTER_USER administrator
    rabbitmqctl delete_user guest
  
    if [ -n "$VHOST" ]; then
        echo "Create virtual host"
        rabbitmqctl add_vhost $VHOST
        rabbitmqctl set_permissions -p $VHOST $CLUSTER_USER ".*" ".*" ".*"
        federate
    fi    
else
  echo "Join cluster"

  rabbitmqctl stop_app

  echo "Joining cluster $CLUSTER_WITH"
  rabbitmqctl join_cluster ${ENABLE_RAM:+--ram} $RABBITMQ_NODENAME@$CLUSTER_WITH

  rabbitmqctl start_app

fi

tail -f /var/log/rabbitmq/*

