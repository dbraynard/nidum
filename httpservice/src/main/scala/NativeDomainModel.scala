package dbraynard.nidum.httpservice


import scala.collection.mutable.ArrayBuffer


//Native class for Fibonacci values used to maintain state
final case class FibCalculations(
                                  serviceId: String,
                                  initTimestamp: Long,
                                  scheduledEndTimestamp: Long,
                                  runTimestamps: ArrayBuffer[Long],
                                  completeTimestamps: ArrayBuffer[Long],
                                  serviceEndTimestamp: Option[Long],
                                  inputs: ArrayBuffer[Int],
                                  fibSequence: Option[ArrayBuffer[BigInt]]
                                )


