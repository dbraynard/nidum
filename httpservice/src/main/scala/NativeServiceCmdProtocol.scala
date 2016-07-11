package dbraynard.nidum.httpservice

import scala.collection.mutable.ArrayBuffer

object NativeServiceCmdProtocol {

  //Needed to allow for distributing different services and input
  // ranges for each of those services across nodes as a sharding strategy.
  sealed trait ServiceMsg {
    val serviceId: String
  }


  //Commands and Queries:
  // This dichotomy allows the overall system to follow the
  // Command and Query Responsibility Segregation (CQRS) pattern)


  ////////////////
  // Commands
  ////////////////



  //Commands base type (sent to cmd actors)
  sealed trait ServiceCmd extends ServiceMsg


  case class CloseServiceCmd(msg: String)


  //Native Service Commands (sent to cmd actors)
  case class InitFibCmd(serviceId: String, scheduledEndTimestamp: Long) extends ServiceCmd
  case class RunFibCmd(serviceId: String, input: Int) extends ServiceCmd


  /////////////////////
  // Acknowledgements
  /////////////////////


  //Command acknowledgement base type (sent from cmd actors)
  sealed trait ServiceAck extends ServiceMsg


  case class ServiceEndedAck(serviceId: String, timestamp: Long) extends ServiceAck
  case class InvalidServiceAck(serviceId: String, msg: String) extends ServiceAck

  //Native Service Command Acknowledgements (sent from cmd actors)
  case class InitFibAck(serviceId: String, persistenceId: String, initTimestamp: Long) extends ServiceAck
  case class RunFibAck(serviceId: String, input: Int, timestamp: Long, output: ArrayBuffer[BigInt]) extends ServiceAck
  case class RunFibInvalidAck(serviceId: String, input: Int, timestamp: Long, msg: String ) extends ServiceAck

  //Queries base type (sent to query actors)
  sealed trait ServiceQuery extends ServiceMsg


}





