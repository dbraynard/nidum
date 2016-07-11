package dbraynard.nidum.httpservice


import akka.actor.{ActorPath, ActorSystem}
import com.typesafe.config.ConfigFactory

/**
  * Created by osboxes on 07/07/16.
  */
object ClusterNodeApp extends App {


  val conf =
    """akka.remote.netty.tcp.hostname="%hostname%"
      |akka.remote.netty.tcp.port=%port%
    """.stripMargin

  val argumentsError = """
   Please run the service with the required arguments: <hostIpAddress> <port> """


  assert(args.length == 2, argumentsError)


  val hostname = args(0)
  val port = args(1).toInt

  //create an Akka config object
  val config =
  ConfigFactory.parseString( conf.replaceAll("%hostname%",hostname)
    .replaceAll("%port%",port.toString)).withFallback(ConfigFactory.load())



  // Create an Akka system
  implicit val clusterSystem = ActorSystem("ClusterSystem", config)

//  //Initialize shared store if this application is the primary node
//  //But, always register the actor system object.
//  InitSharedStore.init(
//    clusterSystem,
//    (port == 2551),
//    path = ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store")
//  )


  //Local application object
  ClusterBoot.bootRegion(clusterSystem)



}
