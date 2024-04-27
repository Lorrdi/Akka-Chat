package org.example

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.example.view.Message

import scala.collection.mutable

object Worker {

  val workerServiceKey: ServiceKey[Command] = ServiceKey[Worker.Command]("Worker")
  private var workerName = new String
  private val mapNameMessengers: mutable.Map[String, List[Message]] = mutable.Map[String, List[Message]]()

  def apply(): Behavior[Command] =
    Behaviors.setup { ctx =>
      ctx.log.info("Registering myself with receptionist")
      ctx.system.receptionist ! Receptionist.Register(workerServiceKey, ctx.self)

      Behaviors.receiveMessage {

        case NewName(name) =>
          workerName = name
          Behaviors.same
        case MyNameAsk(replyTo) =>
          replyTo ! MyName(workerName)
          Behaviors.same
        case SendMessage(message) =>
          val partner: String = if (message.getTo != workerName) {
            message.getTo // Message is intended for someone else
          } else {
            message.getFrom // Message is received from someone else
          }
          val updatedMessages = mapNameMessengers.getOrElse(partner, List.empty) :+ message
          mapNameMessengers += (partner -> updatedMessages)
          Behaviors.same



        case ChatWithThisParentAsk(name, replyTo) =>
          if (mapNameMessengers.contains(name)) {
            replyTo ! ChatWithThisParent(mapNameMessengers(name))
            Behaviors.same
          }
          else {
            replyTo ! ChatWithThisParent(List(new Message()))
            Behaviors.same
          }

        case Stop() =>
          Behaviors.stopped
      }
    }

  sealed trait Command

  final case class NewName(name: String) extends CborSerializable with Command

  final case class MyNameAsk(replyTo: ActorRef[MyName]) extends Command with CborSerializable

  final case class MyName(name: String) extends CborSerializable

  final case class ChatWithThisParentAsk(name: String, replyTo: ActorRef[ChatWithThisParent]) extends Command with CborSerializable

  final case class ChatWithThisParent(chat: List[Message]) extends CborSerializable

  final case class SendMessage(message: Message) extends Command with CborSerializable

  final case class Stop() extends Command

}
