package dbraynard.nidum.httpservice


import akka.actor.{Actor, ActorLogging}
import com.typesafe.scalalogging.LazyLogging

import scala.language.implicitConversions


/**
  * Created by osboxes on 10/06/16.
  */
trait ALogging extends ActorLogging {

  //required trait for children of this trait
  this: Actor =>

  //Offer any implementing type of ALogging (e.g. BidProcessor)
  //a method called logX which takes a parameter of any type V
  //such that that parameter is passed to FLog and assigned to an


  //
  implicit def toLoggingx[V](v: V): GLog[V] = GLog(v)

  case class GLog[V](v: V) {

    //a fluent style log call which calls
    //log.X with passing the object
    def logInfo(f: V => String): V = {
      log.info(f(v));
      v
    }

    def logDebug(f: V => String): V = {
      log.debug(f(v));
      v
    }

    def logError(f: V => String): V = {
      log.error(f(v));
      v
    }

    def logWarn(f: V => String): V = {
      log.warning(f(v));
      v
    }

    def logTest(f: V => String): V = {
      println(f(v));
      v
    }

  }

}



trait LLogging extends LazyLogging{

  implicit def toLoggingb[V](v: V) : FLog[V] = FLog(v)

  case class FLog[V](v : V)  {
    def logInfo(f: V => String): V = {logger.info(f(v)); v}
    def logDebug(f: V => String): V = {logger.debug(f(v)); v}
    def logError(f: V => String): V = {logger.error(f(v)); v}
    def logWarn(f: V => String): V = {logger.warn(f(v)); v}
    def logTest(f: V => String): V = {println(f(v)); v}
  }
}
