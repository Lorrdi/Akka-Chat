package org.example

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.util.Timeout
import javafx.scene.control.ListView
import org.example.Worker._
import org.example.view.{Contacts, Message}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

//#frontend
object Frontend {

  private var worker: ActorRef[Worker.Command] = _

  private val mapMessagesRef: mutable.Map[String, ActorRef[Command]] = mutable.Map[String, ActorRef[Worker.Command]]()
  private var firstTime = true
  private var mainWorkerName = new String

  def apply(): Behavior[Event] = Behaviors.setup[Event] { ctx =>
    val localPort = Main.localPort.toString

    worker = ctx.spawn(Worker(), "Worker")
    worker ! Worker.NewName(localPort)

    mapMessagesRef(localPort) = worker
    mainWorkerName = localPort


    val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing] {
      case Worker.workerServiceKey.Listing(workers) => WorkersUpdated(workers)
    }
    ctx.system.receptionist ! Receptionist.Subscribe(Worker.workerServiceKey, subscriptionAdapter)

    running(ctx, IndexedSeq.empty, 0)
  }

  private def running(ctx: ActorContext[Event],
                      workers: IndexedSeq[ActorRef[Worker.Command]], jobCounter: Int): Behavior[Event] =

    Behaviors.receiveMessage {

      case WorkersUpdated(newWorkers) =>
        ctx.log.info("List of services registered with the receptionist changed: {}", newWorkers)
        running(ctx, newWorkers.toIndexedSeq, jobCounter)

      case NewNameEvent(newName) =>
        mainWorkerName = newName
        worker ! NewName(newName)
        Behaviors.same

      case Die() =>
        worker ! Stop()
        Behaviors.stopped

      case SendMessageEvent(message) =>
        if (message.getTo != "\u041e\u0431\u0449\u0438\u0439\u0020\u0447\u0430\u0442") {
          if (message.getFrom != message.getTo) {
            mapMessagesRef(message.getTo) ! SendMessage(message)
          } else mapMessagesRef(message.getFrom) ! SendMessage(message)
        }
        else {
          for (msg <- mapMessagesRef) {
            msg._2.tell(SendMessage(message))
          }
        }
        Behaviors.same

      case UpdateInformation(replyTo) =>
        import scala.concurrent.ExecutionContext.Implicits.global
        implicit val scheduler: Scheduler = schedulerFromActorSystem(ctx.system)
        implicit val timeout: Timeout = 5.seconds

        val futures = workers.map { ref =>
          (ref ? MyNameAsk).mapTo[MyName].map { response =>
            val name = response.name
            (name, ref, ref == worker)
          }
        }

        val allResults = Future.sequence(futures)

        allResults.onComplete {
          case Success(results) =>
            val newMapMessagesRef: Map[String, ActorRef[Command]] =
              results.map { case (name, actorRef, _) => name -> actorRef }.toMap
            val vecView = results.map { case (name, actorRef, priority) =>
              new Contacts(actorRef.path.toString, name, priority)
            }.toVector.sortWith(_.compare(_))

            if (newMapMessagesRef != mapMessagesRef || firstTime) {
              mapMessagesRef.clear()
              mapMessagesRef ++= newMapMessagesRef
              firstTime = false

              val buf = new ListView[Contacts]()
              vecView.foreach(buf.getItems.add)
              replyTo ! AnswerUpdateInformation(buf)
            } else {
              replyTo ! AnswerUpdateInformation(new ListView[Contacts]())
            }

          case Failure(exception) =>
            exception.printStackTrace()
        }

        Behaviors.same



      case UpdateMessage(name, replyTo) =>
        implicit val scheduler: Scheduler = ctx.system.scheduler
        implicit val timeout: Timeout = 5.seconds
        val result: Future[ChatWithThisParent] = worker.ask(ChatWithThisParentAsk(name, _))
        result.foreach {
          case response: ChatWithThisParent =>
            replyTo ! AnswerUpdateMessage(response.chat, upd = true)
          case _ =>
            throw new RuntimeException("Unexpected response type")
        }

        Behaviors.same
    }

  sealed trait Event

  final case class UpdateInformation(replyTo: ActorRef[AnswerUpdateInformation]) extends Event with CborSerializable

  final case class AnswerUpdateInformation(workersName: ListView[Contacts]) extends CborSerializable

  final case class UpdateMessage(name: String, replyTo: ActorRef[AnswerUpdateMessage]) extends Event with CborSerializable

  final case class AnswerUpdateMessage(chat: List[Message], upd: Boolean) extends CborSerializable

  final case class NewNameEvent(newName: String) extends Event

  final case class Die() extends Event

  final case class SendMessageEvent(message: Message) extends Event

  private final case class WorkersUpdated(newWorkers: Set[ActorRef[Worker.Command]]) extends Event
}


