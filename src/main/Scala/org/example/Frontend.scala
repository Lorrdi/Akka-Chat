package org.example

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.util.Timeout
import javafx.scene.control.ListView
import org.example.Worker._
import org.example.view.{Contacts, Message}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Frontend {

  def apply(): Behavior[Event] = Behaviors.setup { ctx =>
    val localPort =
      ctx.system.settings.config
        .getInt("myPort")
        .toString // Get port from config
    val worker = ctx.spawn(Worker(), "Worker")
    worker ! Worker.NewName(localPort)

    val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing] {
      case Worker.WorkerServiceKey.Listing(workers) => WorkersUpdated(workers)
    }
    ctx.system.receptionist ! Receptionist.Subscribe(
      Worker.WorkerServiceKey,
      subscriptionAdapter
    )

    new FrontendBehavior(ctx, worker, localPort).behavior
  }

  private class FrontendBehavior(
      ctx: ActorContext[Event],
      worker: ActorRef[Worker.Command],
      initialWorkerName: String
  ) {
    private var mainWorkerName = initialWorkerName
    private val mapMessagesRef = scala.collection.mutable
      .Map[String, ActorRef[Worker.Command]](initialWorkerName -> worker)
    private var firstTime = true

    def behavior: Behavior[Event] = Behaviors.receiveMessage {
      case WorkersUpdated(newWorkers) =>
        ctx.log.info(
          "List of services registered with the receptionist changed: {}",
          newWorkers
        )
        Behaviors.same

      case NewNameEvent(newName) =>
        mainWorkerName = newName
        worker ! NewName(newName)
        Behaviors.same

      case Die() =>
        worker ! Stop()
        Behaviors.stopped

      case SendMessageEvent(message) =>
        sendMessage(message)
        Behaviors.same

      case UpdateInformation(replyTo) =>
        updateInformation(replyTo)(ctx.system.scheduler, Timeout(5.seconds))
        Behaviors.same

      case UpdateMessage(name, replyTo) =>
        updateMessage(name, replyTo)
        Behaviors.same
    }

    private def sendMessage(message: Message): Unit = {
      if (message.getTo != "Общий чат") {
        if (message.getFrom != message.getTo) {
          mapMessagesRef.get(message.getTo).foreach(_ ! SendMessage(message))
          mapMessagesRef.get(message.getFrom).foreach(_ ! SendMessage(message))
        } else {
          mapMessagesRef.get(message.getFrom).foreach(_ ! SendMessage(message))
        }
      } else {
        mapMessagesRef.values.foreach(_ ! SendMessage(message))
      }
    }

    private def updateInformation(
        replyTo: ActorRef[AnswerUpdateInformation]
    )(implicit scheduler: Scheduler, timeout: Timeout): Unit = {
      implicit val scheduler: Scheduler = ctx.system.scheduler
      implicit val timeout: Timeout = 5.seconds

      val futures = mapMessagesRef.map { case (name, ref) =>
        ref
          .ask(MyNameAsk)
          .mapTo[MyName]
          .map(response => (name, ref, response.name, ref == worker))
      }

      Future.sequence(futures).onComplete {
        case Success(results) =>
          val newMapMessagesRef = results.map { case (_, ref, name, _) =>
            name -> ref
          }.toMap
          val vecView = results
            .map { case (_, ref, name, priority) =>
              new Contacts(ref.path.toString, name, priority)
            }
            .toVector
            .sortWith(_.compare(_) < 0)

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
          ctx.log.error("Failed to update information", exception)
      }
    }

    private def updateMessage(
        name: String,
        replyTo: ActorRef[AnswerUpdateMessage]
    ): Unit = {
      implicit val scheduler: Scheduler = ctx.system.scheduler
      implicit val timeout: Timeout = 5.seconds

      worker
        .ask(ChatWithThisParentAsk(name, _))
        .mapTo[ChatWithThisParent]
        .onComplete {
          case Success(response) =>
            replyTo ! AnswerUpdateMessage(response.chat, upd = true)
          case Failure(exception) =>
            ctx.log.error("Failed to update message", exception)
        }
    }
  }

  sealed trait Event
  final case class UpdateInformation(replyTo: ActorRef[AnswerUpdateInformation])
      extends Event
      with CborSerializable
  final case class AnswerUpdateInformation(workersName: ListView[Contacts])
      extends CborSerializable
  final case class UpdateMessage(
      name: String,
      replyTo: ActorRef[AnswerUpdateMessage]
  ) extends Event
      with CborSerializable
  final case class AnswerUpdateMessage(chat: List[Message], upd: Boolean)
      extends CborSerializable
  final case class NewNameEvent(newName: String) extends Event
  final case class Die() extends Event
  final case class SendMessageEvent(message: Message) extends Event
  private final case class WorkersUpdated(
      newWorkers: Set[ActorRef[Worker.Command]]
  ) extends Event
}
