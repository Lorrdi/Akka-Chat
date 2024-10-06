package org.example

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.example.view.Message

import scala.collection.mutable

object Worker {

  val WorkerServiceKey: ServiceKey[Command] = ServiceKey[Command]("Worker")

  def apply(): Behavior[Command] =
    Behaviors.setup { ctx =>
      ctx.log.info("Registering myself with receptionist")
      ctx.system.receptionist ! Receptionist.Register(WorkerServiceKey, ctx.self)

      new WorkerBehavior().behavior
    }

  private class WorkerBehavior {
    private val mapNameMessengers: mutable.Map[String, List[Message]] = mutable.Map.empty
    private var workerName: String = ""

    def behavior: Behavior[Command] = Behaviors.receiveMessage {
      case NewName(name) =>
        workerName = name
        Behaviors.same

      case MyNameAsk(replyTo) =>
        replyTo ! MyName(workerName)
        Behaviors.same

      case SendMessage(message) =>
        val partner = if (message.getTo != workerName) message.getTo else message.getFrom
        mapNameMessengers.updateWith(partner) {
          case Some(messages) => Some(messages :+ message)
          case None => Some(List(message))
        }
        Behaviors.same

      case ChatWithThisParentAsk(name, replyTo) =>
        replyTo ! ChatWithThisParent(mapNameMessengers.getOrElse(name, List(new Message())))
        Behaviors.same

      case Stop() =>
        Behaviors.stopped
    }
  }

  sealed trait Command

  final case class NewName(name: String) extends Command with CborSerializable
  final case class MyNameAsk(replyTo: ActorRef[MyName]) extends Command with CborSerializable
  final case class MyName(name: String) extends CborSerializable
  final case class ChatWithThisParentAsk(name: String, replyTo: ActorRef[ChatWithThisParent]) extends Command with CborSerializable
  final case class ChatWithThisParent(chat: List[Message]) extends CborSerializable
  final case class SendMessage(message: Message) extends Command with CborSerializable
  final case class Stop() extends Command
}
