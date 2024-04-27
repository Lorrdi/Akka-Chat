package org.example

import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import javafx.application.Application
import javafx.scene.control.ListView
import org.example.Frontend._
import org.example.view.{Contacts, Message, ScalaWindow}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object Main extends App {
  implicit val timeout: Timeout = 5.seconds

  val localPort = ConfigFactory.load().getInt("myPort")
  private var context: scaladsl.ActorContext[Nothing] = _
  private var frontend: ActorRef[Frontend.Event] = _

  startUp(localPort)
  Application.launch(classOf[ScalaWindow])

  def stop(): Unit = {
    frontend ! Die()
  }

  def updateListUser(): Future[ListView[Contacts]] = {
    implicit val scheduler: Scheduler = schedulerFromActorSystem(context.system)
    val futureResult: Future[AnswerUpdateInformation] = frontend ? UpdateInformation

    futureResult.map { result =>
      result.workersName
    }
  }

  def updateMessageList(name: String): Future[ListView[Message]] = {
    implicit val scheduler: Scheduler = schedulerFromActorSystem(context.system)
    val updateList: Future[AnswerUpdateMessage] =
      frontend ? (UpdateMessage(name, _))

    updateList.map { updateMessage =>
      val listMessage = new ListView[Message]
      if (updateMessage.upd) {
        updateMessage.chat.foreach { message =>
          listMessage.getItems.add(message)
        }
      } else {
        println("Not new")
      }
      listMessage
    }
  }

  def rename(newName: String): Unit = {
    frontend ! NewNameEvent(newName)
  }

  def sendMessage(message: Message): Unit = {
    frontend ! SendMessageEvent(message)
  }


  private def startUp(port: Int): Unit = {

    val config = ConfigFactory
      .parseString(
        s"""
        akka.remote.artery.canonical.port=$port
        akka.cluster.roles = [frontend]
        """)
      .withFallback(ConfigFactory.load(ConfigFactory.load()))
    ActorSystem[Nothing](RootBehavior(), "system", config)

  }

  private object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>
      val cluster = Cluster(ctx.system)
      context = ctx

      if (cluster.selfMember.hasRole("frontend")) {
        frontend = ctx.spawn(Frontend(), "Frontend")
        Behaviors.empty
      } else {
        Behaviors.stopped
      }
    }
  }

}
