import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.rabbitmq.client.impl.DefaultExceptionHandler;

public class ClusterTester {

    public static final String EXCHANGE = "heartbeats";

    public static void main(String argv[]) throws IOException, TimeoutException, URISyntaxException, InterruptedException {
        List<Address> brokers = new ArrayList<>();
        for (String addr : argv[0].split(",")) {
            URI uri = new URI("dummy://" + addr);
            brokers.add(new Address(uri.getHost(), uri.getPort()));
        }
        List<Address> brokers2 = new ArrayList<>();
        for (String addr : argv[1].split(",")) {
            URI uri = new URI("dummy://" + addr);
            brokers2.add(new Address(uri.getHost(), uri.getPort()));
        }
        new ClusterTester(brokers, "clusterOne");
        new ClusterTester(brokers2, "clusterTwo");

        while (true) {
            Thread.sleep(1000);
        }
    }

    public ClusterTester(List<Address> brokers, String message) throws IOException, TimeoutException {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
        ConnectionFactory factory = new ConnectionFactory();
        factory.setExceptionHandler(new ConnectionMonitor(message));
        factory.setTopologyRecoveryEnabled(true);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(200);
        factory.setRequestedHeartbeat(0);
        factory.setConnectionTimeout(500);
        factory.setHandshakeTimeout(1000);
        factory.setSharedExecutor(executor);
        factory.setUsername("user");
        factory.setPassword("password");
        factory.setVirtualHost("cluster");

        Connection connection = factory.newConnection(brokers.toArray(new Address[brokers.size()]));
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE, "fanout", true, false, null);

        String queueName = ManagementFactory.getRuntimeMXBean().getName();
        channel.queueDeclare(queueName, false, false, true, null);
        channel.queueBind(queueName, EXCHANGE, "");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {
                String msg = new String(body, "UTF-8");
                if (!message.equals(msg)) {
                    System.out.println(msg + " is still available!");
                }
            }
        };

        executor.submit(() -> {
            try {
                channel.basicConsume(queueName, consumer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        executor.scheduleAtFixedRate(() -> {
            try {
                channel.basicPublish(EXCHANGE, "", MessageProperties.BASIC, message.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 100, 1000, TimeUnit.MILLISECONDS);

    }

    private static class ConnectionMonitor extends DefaultExceptionHandler implements RecoveryListener {

        private final String clusterName;

        public ConnectionMonitor(String clusterName) {
            this.clusterName = clusterName;
        }

        @Override
        public void handleUnexpectedConnectionDriverException(Connection connection, Throwable exception) {
            System.out.println(clusterName + ": Unexpected connection exception" + exception.getMessage());
        }

        @Override
        public void handleConnectionRecoveryException(Connection connection, Throwable exception) {
            System.out.println(clusterName + ": Connection recovery exception " + exception.getMessage());
        }

        @Override
        public void handleConsumerException(Channel channel,
                                            Throwable exception,
                                            com.rabbitmq.client.Consumer consumer,
                                            String consumerTag,
                                            String methodName) {
            super.handleConsumerException(channel, exception, consumer, consumerTag, methodName);
            System.out.println(clusterName + ": Channel consume exception. Exception: " + exception.getMessage());
        }

        @Override
        public void handleRecovery(Recoverable recoverable) {
            System.out.println(clusterName + ": Connection recovered.");
        }

        @Override
        public void handleRecoveryStarted(Recoverable arg0) {
            System.out.println(clusterName + ": Autorecovery started.");
        }

    }

}
