# nidum - Fibonacci Restful Service

#### Design Goals
1.	Implement a distributed, fault-tolerant, scalable, multi-tenant, and observable service.
2.	Establish a general framework/template on top of all of Akka's framework features for quickly creating additional microservices.

#### Basic Instructions
1.	Run vagrant up, or build on the host machine with `./gradlew build`, `./gradlew runNode` (in one console), and `./gradlew runHttp` (in another).
2.	Connect to box with vagrant ssh or ssh vagrant@192.168.40.10 (password vagrant) if using Vagrant
3.	Get Instructions: `curl 127.0.0.1:9000/fib`
4.	Run `curl 127.0.0.1:9000/fib/myserv1/60/10` (as an example command to the Fibonacci service)
5.	Alternately, if host machine is able to access the guest box, then browse to: `127.0.0.1:9000/fib/myserv1/60/10`

#### Restful Command Description
The curl command has 4 parts, using the above example:

1.	**/fib/** for fibanacci service
2.	**/myserv1/** for your own user or service instance (creates an Akka entity with this value as part of the persistence id)
3.	**/60/** is the service timeout in seconds. The first request to fib service initializes a "service" and it will close after 60 seconds. Commands can be issued again under the same service name. However, the timeout path item must be present, but its value is ignored during subsequent calls. After the given time span has passed, future calls to the fib service with the given service instance will return a message indicating that the service has ended.
4.	**/10** is the input value for the Fibanacci service. The service will return the first 10 values in the Fibanaccni sequence.

#### Additional Notes on "service instance" and "timeout"
The "service instance" in this restful service is meant to represent a controlled allocation of usage of the given micro service. The restful url includes a "timeout" value as a user input to enable a simple demonstration of timing out a service. However, such a timeout value would need to be be originated by some user policy system and applied by a middle-tier component in a production system.


#### Services/JVMs

This Akka application is configured to run one Http service and one Node service. The Http service (JVM 1) contains a cluster region proxy and uses Routing DSL to integrate the HttpRequest/Responses with the cluster backend through the region proxy. The Node service (JVM 2) hosts the Fibonacci actor entities (instances) and handles the requests. Additional nodes can be added to the cluster through simply starting up additional nodes (instructions below) and providing them with a new port number (e.g. 2553, 2554, etc.). 
They should all use the same application.conf file which specifies the seed node(s) for joining the Akka cluster.

#### Starting up additional nodes

To start up additional nodes:

1.	Add a new task entry (e.g. "runNode2") to the build.gradle file

 ```groovy
 task runNode2(type:JavaExec) {
 main = 'dbraynard.nidum.httpservice.ClusterNodeApp'
 classpath = sourceSets.main.runtimeClasspath
 args = ["127.0.0.1", 2553]
 setStandardInput(System.in)
 }
 ```
2.	Execute using `./gradlew runNode2`

The default node allocation strategy should distribute new shards to the additional node(s).

#### Notes on Persistence Storage

This Akka application is configured to use the Cassandra Persistence plugin. Therefore, the vagrant script installs docker and pulls a cassandra docker image. The Virtual Box VM hosting the container is set to use 2 cpus and 8 GB of ram which should suffice.
Fibonacci Service Implementation Details
The Fibonacci actor implementation maintains a cache of the sequence as part of its state object which is expanded as larger sequence requests are made. Each instance of the actor (aka. entity) corresponds to a service id passed in the restful uri. A history of requests are also retained as part of the actor state object.

#### Next features to be added

1.	Web service to stream all users' fib requests in real-time using Akka Persistency Query
2.	Web service to stream a specific fib user's requests in real-time using Akka Persistency Query

