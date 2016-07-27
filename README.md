# load-tester

This simple load tester was created to test Red Hat A-MQ 6.2.1 as an MQTT broker.

## How To Run a Test

1. Create a properties file like properties/aws.properties and place it in the properties directory.

The load tester requires a controller server, and a set of loader clients that produce the load specified in the test scenario disseminated from the controller. An A-MQ broker instance embedded in the controller is used for IPC over JMS. Substitute the IP of your controller server in the value of the property "controller.amq.url".

> gcBroker=true	# Do you want to GC the broker(s) you want to test before the start of the test
> controller.amq.url=tcp://172.31.13.160:61616 # Connection url for the JMS connector of the controller's embedded broker
> controller.amq.config=file:config/amq/activemq.xml # Location of the ActiveMQ configuration file for the controller's embedded broker
> client.log=/home/ec2-user/client.log # Loader server Log location

2. Start the controller on the controller server. Provide the prefix of the properties file you created from #1 as the 1st argument.

```shell
controller.sh aws
```

3. The controller will prompt for the scenario you want to run. It gives you a set of predefined scenarios in the folder spring/test/broker. Either modify these, or create your own scenario and pass the location of your scenario file.

> [0] context-test-burst-failover.xml
> [1] context-test-burst-networked-2.xml
> [2] context-test-burst-networked-3.xml
> ...
> 2016-07-27 01:35:10,680 INFO  [main] loadtester.Node (Node.java:125) - Select one of the test contexts, or enter a new path to test context:

4. Start as many loader clients as needed by your scenario. For example, a scenario that has 4 sends, and 1 receive requires 5 loader clients (i.e. 5 JVM's).

 ```shell
client.sh aws
```

5. When the required # of loader clients are online, the controller will begin orchestrating the test. You cannot pause or quit the test in the "setup" phase, but you can in the "attack" phase.

## Other Setup

If you're simulating a large # of MQTT connections, make sure you modify the ulimits, and TCP/IP-, and NIC-related kernel parameters appropriately, so you don't run into issues like OOM, or SYN flood. Also, some of the test scenario included by default simulate SSL connections using the TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA cipher suite. This is supported Java 1.7 and above, so keep that in mind.