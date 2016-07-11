package dbraynard.nidum.httpservice

import scala.concurrent.duration._
import akka.actor.{ActorIdentity, ActorLogging, ActorRef, Identify, Props}
import akka.cluster.sharding.ShardRegion
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer, Update}
import dbraynard.nidum.httpservice.NativeServiceCmdProtocol._
import dbraynard.nidum.httpservice.NativeServiceEvents._

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

//required by the context.system.scheduler
import scala.concurrent.ExecutionContext.Implicits.global


object FibProcessor {

  //props method to help new up an instance of the actor class
  def props(): Props = Props(classOf[FibProcessor])

  //Required by the ClusterSharding system
  //message => entity Id + messageToSend (transformation)
  val idExtractor: ShardRegion.ExtractEntityId = {
    case m: ServiceCmd => (m.serviceId, m)
  }

  //Required by the ClusterSharding system
  //mod id's hashcode by 100 for shard id
  val shardResolver: ShardRegion.ExtractShardId = {
    case m: ServiceCmd => (math.abs(m.serviceId.hashCode) % 100).toString
  }

  //Required by the ClusterSharding system
  val shardName: String = "FibProcessor"

}

class FibProcessor extends PersistentActor with ALogging with Passivation  {

  //enable to short-hand calls to the companion object
  import FibProcessor._

  /** It is thru this persistenceId that this actor is linked to the PersistentActor's event journal */
  override def persistenceId: String = self.path.parent.name + "-" + self.path.name


  context.setReceiveTimeout(1 minute)


  /**
    * This formalizes the effects of this processor
    * Each command results in:
    * maybe AuctionEvt,
    * an AuctionAck,
    * maybe newReceive
    */

  private final case class ProcessedCommand(event: Option[ServiceEvt],
                                            ack: ServiceAck,
                                            newReceive: Option[Receive])


  /** Used only for recovery */
  private var serviceRecoverStateMaybe: Option[FibCalculations] = None

  override def preStart(): Unit = {
    context.actorSelection("akka.tcp://example@127.0.0.1:2552/user/store") ! Identify(1)
  }


  //handles recovery state
  override def receiveRecover: Receive = {

    case evt: FibInitEvt => {

      //assign the local var to a new state (empty array)
      //This is simulating the first event re-occurring.
      serviceRecoverStateMaybe = Some(
        FibCalculations(
          evt.serviceId,
          evt.initTimestamp,
          evt.scheduledEndTimestamp,
          new ArrayBuffer[Long], //run timestamps
          new ArrayBuffer[Long], //completed timestamps
          None,                   //service end timestamp
          new ArrayBuffer[Int], //inputs
          None //Fibonacci sequence
        ))

    }
      //any other service event type should simply be passed to update state
      //where update state can decide how to apply the event to the current state
    case evt: ServiceEvt => {


      //update the local state value used only during recovery
      serviceRecoverStateMaybe = serviceRecoverStateMaybe.map(state => {
        updateState(evt.logInfo("received recover evt: " + _.toString()), state)
      })

    }


    case RecoveryCompleted => postRecoveryBecome(serviceRecoverStateMaybe)

    case SnapshotOffer(_, snapshot) =>
      postRecoveryBecome(snapshot.asInstanceOf[Option[FibCalculations]].logInfo("recovery from snapshot state: " + _.toString))

    case _ => {
      log.error("receiveRecover case not handled!")
    }


  }

  override def receiveCommand: Receive = passivate(initial)


  def postRecoveryBecome(serviceRecoverStateMaybe: Option[FibCalculations]): Unit = {


    //uses fold method to run function over the value
    serviceRecoverStateMaybe.fold[Unit]({}) { state =>
      log.info("postRecoveryBecome")

      //if end timestamp is set, this service is closed
      if (state.serviceEndTimestamp.isDefined) {
        context.become(passivate(serviceClosed(state)).orElse(unknownCommand))
      }
      else {
        launchLifetime(state.scheduledEndTimestamp)
        context.become(passivate(takingRequests(state)).orElse(unknownCommand))
      }
    }

  }

    //possibly offer a lifetime for this instance
    def launchLifetime(time: Long) = {

      //calculate the time remaining till the end of th auction
      //use the logInfo method

      val serviceEnd = (time - System.currentTimeMillis()).logInfo("launchLifetime over in: "
        + _.toString + " ms")


      //if in the future, scheduled an end cmd
      if (serviceEnd > 0) {
        context.system.scheduler.scheduleOnce(serviceEnd.milliseconds, self, CloseServiceCmd("Service End Time Reached."))
      }

    }


  def initial: Receive = {

    case InitFibCmd(serviceId: String, scheduledEndTimestamp: Long) => {

      // This case will new up the internal state object and pass it to the TakingRequests
      //receiver method

      //First, this case should handle any immediate validation
      // checks for starting up a Fibonacci service in this actor instance

      //No validation checks defined for now....


      //Construct an object that contains what is needed to change the state of this actor
      //and communicate acknowledgement back to the sender

      val currentTime = System.currentTimeMillis()

      //event to be journaled
      val fibInitEvt = Some(FibInitEvt(
        serviceId,
        currentTime,
        scheduledEndTimestamp))

      //initialize a state object
      val state = FibCalculations(
        serviceId,
        currentTime,
        scheduledEndTimestamp,
        new ArrayBuffer[Long],
        new ArrayBuffer[Long],
        None,
        new ArrayBuffer[Int],
        None
      )

      //consolidation of objects for framework logistics
      val pc = ProcessedCommand(
        fibInitEvt,                          //event to journal
        InitFibAck(serviceId, persistenceId, currentTime),  //ack to reply to sender/caller
        Some(
          passivate(
            takingRequests(state).orElse(unknownCommand)) //new actor state to become
        )
      )

      //schedule a shutdown of this service
      launchLifetime(scheduledEndTimestamp)

      //call a local helper method for processing the pc object
      handleProcessedCommand(
        sender(),
        pc)
    }

  }

  def serviceClosed(state: FibCalculations): Receive = {

    case cmd: ServiceCmd => {
      sender() ! ServiceEndedAck(state.serviceId, state.serviceEndTimestamp.get)
    }

  }

  def takingRequests(state: FibCalculations): Receive = {

    case CloseServiceCmd(msg: String) => {

      val currentTime = System.currentTimeMillis()

      persist(ServiceEndEvt(state.serviceId, currentTime)) {
        evt =>
        {
          //apply the evt to the current state object
          val newState = updateState(evt, state)

          //set actor state to closed
          context.become(
            passivate(
              serviceClosed(newState)
            ).orElse(unknownCommand)
          )
        }
      }

    }

    case RunFibCmd(serviceId: String, input: Int) => {

      val timestamp = System.currentTimeMillis()

      //validate input
      if (input < 0) {

        val ack = RunFibInvalidAck(serviceId, input, timestamp, "Input must not be negative.")

        sender() ! ack

        //Just pass the current state to this method
        val receive = passivate(takingRequests(state))

        //akka's become method
        context.become(receive)

      }

      //assigned from cache or calculated partially from cache to some degree
      var output: ArrayBuffer[BigInt] = new ArrayBuffer(0)

      var updateFibSequence = false

      //if local fib is already calculated, return cached value
      if (state.fibSequence.isDefined &&
        state.fibSequence.get.length >= input) {
        //take the 1st "input" amount from the existing array
        output = state.fibSequence.get.take(input)
      }
      else {
        //use the current fib seq as a hint

        updateFibSequence = true

        val hint = state.fibSequence

        //call local math library (need None call for initial use)
        output = NativeMathLibrary.FibonacciSequence(input, hint)
      }

      //create an calculated event
      val completedTimestamp = System.currentTimeMillis()

      val calculatedEvt = FibCalculatedEvt(serviceId,
        completedTimestamp,
        input,
        if (updateFibSequence) output else state.fibSequence.get )

      //Acknowledgement to send back to sender with the answer
      val ack = RunFibAck(serviceId, input, completedTimestamp, output)

      //Create a new state object using the event
      val newState = updateState(calculatedEvt, state)

      //Determine the new actor state
      val receive = Some(passivate(takingRequests(newState)))

      //Construct object for framework logistics
      val pc = ProcessedCommand(
        Some(calculatedEvt),
        ack,
        receive
      )

      //Call a local helper method for processing the pc object
      handleProcessedCommand(sender(), pc)

    }

    case InitFibCmd(serviceId: String, scheduledEndTimestamp: Long) => {

      //This service was already initialized, just echo back the ack

      val pc = ProcessedCommand(
        None,                          //event to journal
        InitFibAck(serviceId, persistenceId, 0L),  //ack to reply to sender/caller
        Some(
          passivate(
            takingRequests(state).orElse(unknownCommand)) //new actor state to become
        )
      )

      //call a local helper method for processing the pc object
      handleProcessedCommand(
        sender(),
        pc)


    }

  }

  def unknownCommand: Receive = {
    case other => {
      other.logInfo("unknownCommand: " + _.toString)
      sender() ! InvalidServiceAck("", "InvalidAuctionAck")
    }
  }


  private def updateState(evt: ServiceEvt, state: FibCalculations): FibCalculations = {

    evt match {

      case evt:FibBeginCalculationEvt => {

        //re-assign timestamp and input value
        state.copy(
          runTimestamps = state.runTimestamps += evt.timestamp,
          inputs = state.inputs += evt.input
        )
      }

      case evt:FibCalculatedEvt => {

        //new timestamp of completed operation plus the new sequence
        state.copy(
          fibSequence = Option(evt.output),
          runTimestamps = state.runTimestamps += evt.timestamp
        )
      }

      case evt:ServiceEndEvt => {
        state.copy(
          serviceEndTimestamp = Option(evt.timestamp)
        )


      }

    }

  }


  def handleProcessedCommand(sendr: ActorRef, processedCommand: ProcessedCommand): Unit = {

    //"fold" here means if the event is an empty option, then run/return first "(=>B)",
    //otherwise, pass the value of the event into the second function and run it.

    //send acknowledgement to sender of the cmd
    processedCommand.event.fold(sender() ! processedCommand.ack) {
      evt => {
        //persist the event to the underlying journal store
        //and run the provided function
        persist(evt) { persistEvt => {

          //In earlier akka versions, a persistent view needed to be told to update its state
          //This approach has been replaced by calling the Persistent Query extension from
          //a reading/querying component/actor/client
          //readRegion ! Update(await = true) //update read path

          //pass ack to the specified sender
          sendr ! processedCommand.ack

          //change state of this actor (if applicable)
          processedCommand.newReceive.fold()(context.become)

        }

        }
      }
    }


  }


}
