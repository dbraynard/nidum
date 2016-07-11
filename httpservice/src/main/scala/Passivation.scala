package dbraynard.nidum.httpservice



import akka.actor.{Actor, PoisonPill, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion.Passivate



trait Passivation extends ALogging {

  this: Actor =>

  protected def passivate(receive: Receive): Receive = receive.orElse {

    //COMMENT on passivate in ShardRegion.scala
    /**
      * If the state of the entities are persistent you may stop entities that are not used to
      * reduce memory consumption. This is done by the application specific implementation of
      * the entity actors for example by defining receive timeout (`context.setReceiveTimeout`).
      * If a message is already enqueued to the entity when it stops itself the enqueued message
      * in the mailbox will be dropped. To support graceful passivation without losing such
      * messages the entity actor can send this `Passivate` message to its parent `ShardRegion`.
      * The specified wrapped `stopMessage` will be sent back to the entity, which is
      * then supposed to stop itself. Incoming messages will be buffered by the `ShardRegion`
      * between reception of `Passivate` and termination of the entity. Such buffered messages
      * are thereafter delivered to a new incarnation of the entity.
      *
      * [[akka.actor.PoisonPill]] is a perfectly fine `stopMessage`.
      */


    //tell parent actor to send us a poisonpill
    case ReceiveTimeout =>
      self.logInfo( s => s"${s} ReceivedTimeout: passivating. ")
      context.parent ! Passivate(stopMessage = PoisonPill)

    case PoisonPill => context.stop(self.logInfo (s => s"${s} PoisonPill"))

  }



}
