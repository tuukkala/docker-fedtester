# Rabbitmq cluster setup

This docker rabbitmq setup is created to allow testing federation link breakage in cluster autoheal situations after
a network split. 

## Requirements
Docker

## Create rabbimq containers

Build image:
> docker build -t rabbitmq-cluster .

Run:
> docker-compose up -d

Let the brokers start for a bit...

Check brokers from ui.
 * http://localhost:15672
 * http://localhost:15674

> Credentials: user/password

Check the federations that they are ok.

## ClusterTester

There is a super simple test app included with the source. 

## Requirements

 * [RabbitMQ Java Client](http://central.maven.org/maven2/com/rabbitmq/amqp-client/4.1.0/amqp-client-4.1.0.jar)
 * [SLF4J API](http://central.maven.org/maven2/org/slf4j/slf4j-api/1.7.21/slf4j-api-1.7.21.jar)
 * [SLF4J Simple](http://central.maven.org/maven2/org/slf4j/slf4j-simple/1.7.22/slf4j-simple-1.7.22.jar)


Build and run

> You can set an environment variable for the jar files on the classpath e.g.
>
>      $ export CP=.:amqp-client-4.1.0.jar:slf4j-api-1.7.21.jar:slf4j-simple-1.7.22.jar
>      > javac -cp %CP% ClusterTester.java
>      $ java -cp $CP ClusterTester localhost:5672,localhost:5673 localhost:5674,localhost:5675 
>
> or on Windows:
>
>      > set CP=.;amqp-client-4.1.0.jar;slf4j-api-1.7.21.jar;slf4j-simple-1.7.22.jar
>      > javac -cp %CP% ClusterTester.java
>      > java -cp %CP% ClusterTester localhost:5672,localhost:5673 localhost:5674,localhost:5675 


> The app will connect to the two clusters and print for as longs as messages are coming:
>
>      clusterTwo is still available!
>      clusterOne is still available!
>      clusterOne is still available!
>      clusterTwo is still available!

## Creating a network split

Let the app run and start creating networksplits. The split command is send to the docker container.
The containers are named:
 * rabbitmq-cluster1
 * rabbitmq-cluster2
 * rabbitmq-cluster3
 * rabbitmq-cluster4

The best way to to get the problem seems to be to block a broker where the app is not connected to. 

Run the this command agains the container you want to block (e.g. rabbitmq-cluster4):
> docker exec --privileged rabbitmq-cluster4 bash /usr/local/bin/block_broker.sh -time 15

Repeat untill the test app stops printing either of the cluster is still available messages. To fix the issue you need recreate the
federation policy through the admin ui.

Note that autorecovery sometimes seems to take a while and is not be mistaken for the issue.
The state is somewhat permanent when the federation link problem occures.



