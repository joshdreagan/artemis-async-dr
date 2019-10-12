# artemis-async-dr

## Standalone

You can either use 2 separate machines, or use the same machine and different ports to represent different DCs.

- Create some Artemis Broker instances. _Note: You can add whatever extra args you'd like. See https://activemq.apache.org/components/artemis/documentation/latest/using-server.html for more info._

```
cd $PROJECT_ROOT
$ARTEMIS_HOME/bin/artemis create --name dc1 --user client --password password ./brokers/dc1
$ARTEMIS_HOME/bin/artemis create --name dc2 --user client --password password --port-offset 1 ./brokers/dc2
```

- Add the connectors, addresses, diverts, bridges, and plugins from the [dc1-broker.xml](./artemis-configs/dc1-broker.xml) & [dc2-broker.xml](./artemis-configs/dc2-broker.xml) files to each respective generated '$ARTEMIS_INSTANCE/etc/broker.xml' files.

- Build and copy the transformers/plugins jar into each of the broker instance lib directories.

```
cd $PROJECT_ROOT/artemis-transformers
mvn clean install
cp target/artemis-transformers-*.jar $PROJECT_ROOT/brokers/dc1/
cp target/artemis-transformers-*.jar $PROJECT_ROOT/brokers/dc2/
```

- Run the broker instances in separate terminals/windows.

```
$PROJECT_ROOT/dc1/bin/artemis run
$PROJECT_ROOT/dc2/bin/artemis run
```

- Start 2 separate MySQL instances. I used the docker image located at https://hub.docker.com/_/mysql.

```
docker run --name mysql1 -e MYSQL_DATABASE=example -e MYSQL_ROOT_PASSWORD=Abcd1234 -p 3306:3306 -d mysql:5.7
docker run --name mysql2 -e MYSQL_DATABASE=example -e MYSQL_ROOT_PASSWORD=Abcd1234 -p 3307:3306 -d mysql:5.7
```

- Run the idempotent notification processors in separate terminals/windows.

```
cd $PROJECT_ROOT/idempotent-notification-processor
## Terminal/Window 1
mvn spring-boot:run '-Damqphub.amqp10jms.remote-url=amqp://localhost:61616' '-Dspring.datasource.url=jdbc:mysql://localhost:3306/example?autoReconnect=true&useSSL=false' '-Dserver.port=8080' '-Dmanagement.server.port=8081'
## Terminal/Window 2
mvn spring-boot:run '-Damqphub.amqp10jms.remote-url=amqp://localhost:61617' '-Dspring.datasource.url=jdbc:mysql://localhost:3307/example?autoReconnect=true&useSSL=false' '-Dserver.port=8090' '-Dmanagement.server.port=8091'
```

- Open a terminal/window and run the sample producer.

```
cd $PROJECT_ROOT/camel-producer
mvn spring-boot:run '-Damqphub.amqp10jms.remote-url=amqp://localhost:61616' '-Damqp.destination.name=FOO'
```

- Open a terminal/window and run the sample consumer.

```
cd $PROJECT_ROOT/camel-consumer
mvn spring-boot:run '-Damqphub.amqp10jms.remote-url=amqp://localhost:61616' '-Damqp.destination.name=FOO'
```

At any point, you can kill the producer or consumer and restart them pointed at the other DC. You can also fail back at any time. You can only have the clients running in one of the DCs at any point in time for a given queue in order to prevent duplicates. If you had multiple queues, the clients for each individual queue could be load-balanced across the DCs. This is known as "partitioned active/active". All replication is asynchronous (ie, "near real-time"). So your potential for message loss/duplication will be a factor of the latency of your replication.

## OpenShift

You can either spin up 2 separate instances of OpenShift, or you can use 2 separate namespaces in a single OpenShift to represent your DCs.

- Build the transformers/plugins jar and create the build configuration in each DC (or namespace).

```
cd $PROJECT_ROOT/artemis-transformers
mvn clean install
oc new-build -D $'FROM registry.redhat.io/amq7/amq-broker:7.5\nUSER root\nRUN mkdir /home/jboss/broker\nRUN mkdir /home/jboss/broker/lib\nRUN chmod -R a+rw /home/jboss/broker\nCOPY *.jar /home/jboss/broker/lib/\nRUN chown -R jboss:root /home/jboss/broker' --name=custom-amq7
oc start-build custom-amq7 --from-dir=target
```

- Generate the keystores/truststores for the DCs and client

```
cd $PROJECT_ROOT/artemis-configs/openshift
keytool -genkey -alias broker -keyalg RSA -keystore dc1-broker.ks
keytool -genkey -alias broker -keyalg RSA -keystore dc2-broker.ks
keytool -export -alias broker -keystore dc1-broker.ks -file dc1_broker_cert
keytool -export -alias broker -keystore dc2-broker.ks -file dc2_broker_cert
keytool -import -alias dc2-broker -keystore dc1-broker.ts -file dc2_broker_cert
keytool -import -alias dc1-broker -keystore dc2-broker.ts -file dc1_broker_cert

keytool -import -alias dc1-broker -keystore client.ts -file dc1_broker_cert
keytool -import -alias dc2-broker -keystore client.ts -file dc2_broker_cert
```

- Configure each DC/namespace

oc project dc1 ## Or 'oc login <dc1>'
oc create secret generic broker-amq-secret --from-literal=username=client --from-literal=password=password --from-literal=cluster-username=cluster --from-literal=cluster-password=password --from-literal=truststore-password=password --from-literal=keystore-password=password --from-file=broker.ts=dc1-broker.ts --from-file=broker.ks=dc1-broker.ks
oc create configmap broker-amq-configmap --from-file=broker.xml=dc1-broker.xml
oc apply -f dc1-service.yaml
oc apply -f dc1-route.yaml
oc apply -f dc1-deploymentconfig.yaml ## Make sure to change the  'spec.template.spec.containers.image' field to match the custom image generated from the previous build.

oc project dc2 ## Or 'oc login <dc2>'
oc create secret generic broker-amq-secret --from-literal=username=client --from-literal=password=password --from-literal=cluster-username=cluster --from-literal=cluster-password=password --from-literal=truststore-password=password --from-literal=keystore-password=password --from-file=broker.ts=dc2-broker.ts --from-file=broker.ks=dc2-broker.ks
oc create configmap broker-amq-configmap --from-file=broker.xml=dc2-broker.xml
oc apply -f dc2-service.yaml
oc apply -f dc2-route.yaml
oc apply -f dc2-deploymentconfig.yaml ## Make sure to change the  'spec.template.spec.containers.image' field to match the custom image generated from the previous build.
```

- Create a MySQL instance in each DC/namespace using the persistent MySQL OpenShift template.

- Install the idempotent notification processor.

```
cd $PROJECT_ROOT/idempotent-notification-processor
oc project dc1 ## Or 'oc login <dc1>'
mvn -P openshift clean install fabric8:deploy

oc project dc2 ## Or 'oc login <dc2>'
mvn -P openshift clean install fabric8:deploy
```

You can now produce/consume to/from the broker in either DC/namespace through the exposed OpenShift Route. Use the [client.ts](./artemis-configs/openshift/client.ts) that you generated in the previous step as your TrustStore. If you want to use the provided [camel-producer](./camel-producer) and [camel-consumer](./camel-consumer) code, you will need to expose the MySQL service using a NodePort and configure the 'spring.datasource.url' property with the appropriate url.

Example client runs:

```
## Producer
mvn spring-boot:run '-Damqp.destination.name=FOO' '-Damqphub.amqp10jms.remote-url=amqps://broker-amq-headless-dc1.192.168.99.102.nip.io:443?transport.verifyHost=false&transport.trustAll=true' '-Dcamel.component.amqp.username=client' '-Dcamel.component.amqp.password=password' '-Dcamel.ssl.cert-alias=dc1-broker' '-Dcamel.ssl.trust-managers.trust-store.resource=/home/user1/projects/artemis-async-dr/artemis-configs/openshift/client.ts' '-Dcamel.ssl.trust-managers.trust-store.password=password'

## Consumer
mvn spring-boot:run '-Damqp.destination.name=FOO' '-Damqphub.amqp10jms.remote-url=amqps://broker-amq-headless-dc1.192.168.99.102.nip.io:443?transport.verifyHost=false&transport.trustAll=true' '-Dcamel.component.amqp.username=client' '-Dcamel.component.amqp.password=password' '-Dcamel.ssl.cert-alias=dc1-broker' '-Dcamel.ssl.trust-managers.trust-store.resource=/home/user1/projects/artemis-async-dr/artemis-configs/openshift/client.ts' '-Dcamel.ssl.trust-managers.trust-store.password=password' '-Dspring.datasource.url=jdbc:mysql://192.168.99.102:32193/example?autoReconnect=true&useSSL=false'
```