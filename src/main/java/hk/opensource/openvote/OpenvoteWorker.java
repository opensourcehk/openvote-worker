package hk.opensource.openvote;

import java.util.Properties;
//import java.util.Date;
//import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.io.File;
import java.io.FileInputStream;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

import org.apache.log4j.*;
import org.apache.log4j.Logger;

public class OpenvoteWorker implements Runnable {

    public static Logger logger = LogManager.getLogger(OpenvoteWorker.class.getName());

    private Properties config;
    private String workerName;
    private String topic;
    private String[] bindings;
    private String hostname;

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private QueueingConsumer consumer;

    
    public OpenvoteWorker(Properties config, String name) throws Exception {
        this.config = config;
        this.workerName = name;

        this.topic = this.config.getProperty("mq_topic");
        this.hostname = this.config.getProperty("mq_host");
        this.bindings = this.config.getProperty("mq_binding").split(":");

        this.factory = new ConnectionFactory();
        this.factory.setHost(hostname);

    }
   
    public void connect() throws Exception {
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclarePassive(this.topic);
        String queueName = channel.queueDeclare("openvote", true, false, false, null).getQueue();

        for(String bindingKey : this.bindings){
            //System.out.println(bindingKey);
            channel.queueBind(queueName, this.topic, bindingKey);
        }

        logger.info(" [*] " + this.workerName + " is Waiting for messages. To exit press CTRL+C");

        channel.basicQos(1);

        this.consumer = new QueueingConsumer(channel);
        boolean autoAck = false;
        channel.basicConsume(queueName, autoAck, consumer);        

    }

    public void run() {
        while (true) {
            QueueingConsumer.Delivery delivery = null;
            String message = "";
            try {
                delivery = consumer.nextDelivery();
                message = new String(delivery.getBody());
                String routingKey = delivery.getEnvelope().getRoutingKey();

                try {    
                    logger.debug(this.workerName + " [x] Received '" + routingKey + "':'" + message + "'");
                    //push into Database
                } catch (Exception e) {
                    logger.error("Error :" + e.toString(), e);
                }

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

            } catch (IllegalArgumentException e) {
                logger.warn("FAIL SENT: "+ message);
            } catch (ShutdownSignalException e) {
                logger.error("Error :" + e.toString() + " " + this.workerName + " stopped.", e);
                return;
            } catch (Exception e) {
                logger.error("Error :" + e.toString(), e);
            }
        }
    }


}
