//package dbraynard.nidum.httpservice
//
//
//import akka.actor.{ActorIdentity, ActorPath, ActorRef, ActorSystem, Identify, Props}
//import akka.persistence.journal.leveldb.SharedLeveldbJournal
//import akka.persistence.journal.leveldb.SharedLeveldbStore
//import akka.pattern.ask
//
//import scala.concurrent.duration._
//import akka.util.Timeout
//
//import scala.concurrent.Await
//
//
//
///**
//  * Created by osboxes on 09/07/16.
//  */
//object InitSharedStore {
//
//  def init(system: ActorSystem, startStore: Boolean, path: ActorPath) = {
//
//
//    if (startStore)
//      system.actorOf(Props[SharedLeveldbStore], "store")
//
//    // register the shared journal
//    import system.dispatcher
//
//
//    implicit val timeout = Timeout(15.seconds)
//    val f = system.actorSelection(path) ? Identify(None)
//
//    f.onSuccess {
//      case ActorIdentity(_, Some(ref)) => {
//
//        println("setting sharedleveljournal now")
//        SharedLeveldbJournal.setStore(ref, system)
//      }
//      case _ => {
//
//        system.log.error("Shared journal not started at {}", path)
//        system.terminate
//      }
//    }
//
//    f.onFailure {
//      case _ =>
//        system.log.error("Lookup of shared journal at {} timed out", path)
//        system.terminate
//    }
//
//    Await.result(f, 10 seconds)
//
//
//
//
//
//  }
//
//}
