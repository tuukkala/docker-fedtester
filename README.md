This docker rabbitmq setup is created to allow testing federation link breakage in cluster autoheal situations after
a network split. The instructions are for windows 10 but the the same should work for linux hosts also.

Build image:
docker build -t rabbitmq-cluster .

Run:
docker-compose up -d

Let the brokers start for a bit...

Check brokers from ui.
http://localhost:15672
http://localhost:15674
Credentials: user/password

Check the federations that they are ok.

Once the brokers are up fire up the test app.
You will need to get the required libs and place the in the ClusterTester/lib folder. 
Needed libs:
amqp-client-4.1.0.jar
commons-io-1.2.jar
commons-cli-1.1.jar
slf4j-api-1.7.21.jar
slf4j-simple-1.7.22.jar 

cd ClusterTester
compile.cmd
run.cmd

The app will connect to the two clusters and print for as longs as messages are coming:
clusterTwo is still available!
clusterOne is still available!
clusterOne is still available!
clusterTwo is still available!
clusterOne is still available!
clusterTwo is still available!
clusterOne is still available!
clusterTwo is still available!

Let the app run and start creating network splits. The split command is send to the docker container. The containers are named
rabbitmq-cluster1, rabbitmq-cluster2, rabbitmq-cluster3 and rabbitmq-cluster4. The best way to to get the problem seems to be
to block a broker where the app is not connected to. 

Run the this command agains the container you want to block (e.g. rabbitmq-cluster4):
docker exec --privileged rabbitmq-cluster4 bash /usr/local/bin/block_broker.sh -time 15

Repeat untill the test app stops printing either of the cluster is still available messages. To fix the issue you need recreate the
federation policy through the admin ui.

Note that autorecovery sometimes seems to take a while and is not be mistaken for the issue.
The state is somewhat permanent when the federation link problem occures.



