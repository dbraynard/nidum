package dbraynard.nidum.httpservice

import akka.actor.{ActorPath, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.http.scaladsl.server.Directives._
import dbraynard.nidum.httpservice.NativeServiceCmdProtocol._
import dbraynard.nidum.httpservice.NativeServiceDto.{FibInputDto, _}
import org.joda.time.format.DateTimeFormat
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._

import scala.collection.mutable.ArrayBuffer
import scala.io.StdIn
import scala.util.{Failure, Success}
import scala.concurrent.duration._




object WebServer {

  private val log = LoggerFactory.getLogger(this.getClass())
  private val argumentsError = """
   Please run the service with the required arguments: " <httpIpAddress>" <httpPort> "<akkaHostIpAddress>" <akkaport> """


  def main(args: Array[String]): Unit = {

    val conf =
      """akka.remote.netty.tcp.hostname="%hostname%"
       akka.remote.netty.tcp.port=%port%
      """.stripMargin

    log.info("Starting HttpApp.")

    assert(args.length == 4, argumentsError)

    val httpHost = args(0)
    val httpPort = args(1).toInt

    val akkaHostname = args(2)
    val akkaPort = args(3).toInt


    val config = ConfigFactory.parseString( conf.replaceAll("%hostname%",akkaHostname)
      .replaceAll("%port%",akkaPort.toString)).withFallback(ConfigFactory.load())



    implicit val system = ActorSystem("ClusterSystem", config)

//    //Initialize shared store if this application is the primary node
//    //But, always register the actor system object.
//    InitSharedStore.init(
//      system,
//      (akkaPort == 2551),
//      path = ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store")
//    )



    implicit val materializer = ActorMaterializer()
    //needed for future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher


    val regionProxyForProcessor = ClusterBoot.bootProxyRegions(system)


    lazy val fmt = DateTimeFormat.forPattern("yyyy-MM-dd-HH:mm")

    // formats for un-marshalling and marshalling
    implicit val fibInputDtoFormat = jsonFormat3(FibInputDto)
    implicit val fibInputStartedDtoFormat = jsonFormat3(FibServiceStartedDto)
    implicit val serviceErrorFormat = jsonFormat3(ServiceError)


    implicit val timeout = Timeout(30000 seconds)

    //declare the route structure
    val route: Route = {
      get {
        pathPrefix("fib") {

          //give user help info if "fib" alone is entered
          pathEnd {
            complete("The Fibonacci Service, enter fib/[serviceId]/[serviceTimeoutSec]/[inputValue] to use this service")
          } ~
          //get service id
          pathPrefix(Segment) { serviceId =>

            pathPrefix(IntNumber) { serviceTimeout =>

              path(IntNumber) { input =>

                val timeoutEpochMilliseconds = System.currentTimeMillis() + serviceTimeout*1000

                //send cmd to cluster
                val initFuture = regionProxyForProcessor ?
                  InitFibCmd(
                    serviceId,
                    timeoutEpochMilliseconds
                  )

                onComplete(
                  //handle when cmd completes
                initFuture) {

                  case Success(ack) => ack match {

                    case ServiceEndedAck(serviceId: String, timestamp: Long) => {

                      complete(ServiceError(serviceId, "Service lifetime has ended."))

                    }

                    case InitFibAck(serviceId, persistenceId, initTimestamp) => {

                      //complete(FibServiceStartedDto(serviceId, initTimestamp))

                      //pass the input value

                      //send cmd to cluster
                      val runFibFuture = regionProxyForProcessor ?
                        RunFibCmd(
                          serviceId,
                          input)

                      onComplete(runFibFuture) {

                        case Success(ack) => ack match {

                          case RunFibAck(serviceId, input , timestamp, output: ArrayBuffer[BigInt]) => {

                            val outString = output.mkString(",")

                            complete(outString)

                          }
                          case RunFibInvalidAck(serviceId: String, input: Int, timestamp: Long, msg: String ) => {

                            //invalid input to Fibonacci method
                            complete("Invalid value provided: " + input + ". Message:" + msg)

                          }
                          case ServiceEndedAck(serviceId: String, timestamp: Long) => {

                            //service could ended in the middle of initiating
                            //service and calling fib service. Highly unlikely.
                            complete(ServiceError(serviceId, "Service lifetime has ended."))

                          }

                          case _ =>
                            complete("error, should never reach this line")
                        }
                        case Failure(t) => {
                          t.printStackTrace()
                          complete(ServiceError("Error", "Error calling service, service id: " + serviceId, t.getMessage))
                        }
                      }
                    }
                    case InvalidServiceAck(id, msg) =>
                      complete(ServiceError("ERROR - invalid service ack", id, msg))

                  }
                  case Failure(t) =>
                    t.printStackTrace()
                    complete(ServiceError("Error", "Error calling service, service id: " + serviceId, t.getMessage))

                }

                //complete("error, should never reach this line")
              } ~
                path(Segment) { mistake =>

                  complete("Invalid input, input must be non-negative integer.")
                }
            }
          }
        }
      } ~
      post {
        path("fib") {
          extract(_.request) { e =>
            entity(as[FibInputDto]) {
              fib => onComplete(
                //ask the proxy region processor with the Cmd object
                (regionProxyForProcessor ?
                  InitFibCmd(
                    fib.serviceId,
                    fmt.parseDateTime(fib.scheduledEndDateTime).getMillis)
                  ).mapTo[ServiceAck]) {

                case Success(ack) => ack match {
                  case InitFibAck(serviceId, persistenceId, initTimestamp) => {

                    complete(FibServiceStartedDto(serviceId, persistenceId, initTimestamp))
                  }
                  case InvalidServiceAck(id, msg) =>
                    complete(ServiceError("ERROR - invalid service ack",id, msg))
                }
                case Failure(t) =>
                  t.printStackTrace()
                  complete(ServiceError("Error", "Error calling service, service id: " + fib.serviceId, t.getMessage))
              }
            }

          }
        }
      }
    }


    val bindingFuture = Http().bindAndHandle(route, httpHost, httpPort)
    println(s"server online at http://${httpHost}:${httpPort}/\nPress Return to stop...")

    StdIn.readLine()

//    bindingFuture.flatMap(_.unbind()) //trigger unbinding from the port
//        .onComplete(_ => {
//
//      //need shutdown cluster ???
//
//      system.terminate()     //shut down actor system
//
//    })


  }
}
