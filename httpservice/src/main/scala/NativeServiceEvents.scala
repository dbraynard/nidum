package dbraynard.nidum.httpservice

import scala.collection.mutable.ArrayBuffer


object NativeServiceEvents {

  //Base event type. Following the event sourceing pattern, events of this
  //parent type are persisted to an actor system's corresponding journal.
  //They can be queried using the Akka Persistence Query extension. In addition,
  //query adapters can be written to add a filter to the event queries.
  sealed trait ServiceEvt {
    val serviceId: String
  }

  //Service end event
  case class ServiceEndEvt(serviceId: String, timestamp: Long) extends ServiceEvt



  //Native event fired when a fibonacci actor is initiated
  case class FibInitEvt(serviceId: String, initTimestamp: Long, scheduledEndTimestamp: Long) extends ServiceEvt

  //Native event which is fired after the Fibonacci cmd is initiated for a given input
  //commanded by the RunFibCmd command.
  case class FibBeginCalculationEvt(
                                     serviceId: String,
                                     timestamp: Long,
                                     input: Int) extends ServiceEvt

  //Native event which is fired after the Fibonacci sequence is
  //calculated for a given input which is commanded by the RunFibCmd command.
  case class FibCalculatedEvt(
                               serviceId: String,
                               timestamp: Long,
                               input: Int,
                               output: ArrayBuffer[BigInt]) extends ServiceEvt




}
