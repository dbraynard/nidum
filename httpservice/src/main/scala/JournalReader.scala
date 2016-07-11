package dbraynard.nidum.httpservice


import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}


import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

import scala.concurrent.duration._


object JournalReader {

  def Read(system: ActorSystem, persistenceId: String): String = {


    //needed for persistence query's internal pipeline
    implicit val mat = ActorMaterializer()(system)

    //query object for reading the journal
    val queries = PersistenceQuery(system).readJournalFor[CassandraReadJournal](
      CassandraReadJournal.Identifier)

    val src: Source[EventEnvelope, NotUsed] =
      queries.eventsByPersistenceId(persistenceId, 0L, Long.MaxValue)

    queries.eventsByTag("sometag", 0)

    val events: Source[Any, NotUsed] = src.map(_.event)
    //events.runForeach { event => println("Event: " + event) }


    var eventTaken = ""
    val takeFuture = events.runForeach { event =>
      eventTaken = event.toString
    }


    Await.result(takeFuture, 10 seconds)

    eventTaken
  }


}
