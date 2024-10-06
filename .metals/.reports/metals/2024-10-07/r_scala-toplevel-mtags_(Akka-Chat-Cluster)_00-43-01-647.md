error id: file:///F:/projects/scala/Akka-Chat/src/main/Scala/org/example/view/ControllerScala.scala:[850..854) in Input.VirtualFile("file:///F:/projects/scala/Akka-Chat/src/main/Scala/org/example/view/ControllerScala.scala", "package org.example.view

import akka.util.Timeout
import akka.actor.typed.ActorSystem
import akka.actor.typed.Scheduler
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.control.{ListCell, ListView, SelectionMode}
import javafx.scene.text.TextAlignment
import org.example.Main
import scala.concurrent.Future
import java.net.URL
import java.util.{ResourceBundle, Timer, TimerTask}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Success, Failure}

class ControllerScala(implicit val system: ActorSystem[Nothing])
    extends Controller
    with Initializable {
  def this() = {
    this(ActorSystem("defaultSystem"))
  }

  // Your existing auxiliary constructor
  def this(system: ActorSystem[Nothing]) = {
    this()
    this.system = system
  }
  private val timerTask = new MyTimerTask()
  private val timer = new Timer(true)
  timerTask.upd(this)

  implicit val timeout: Timeout = Timeout(3.seconds)
  import akka.actor.typed.Scheduler
  implicit val scheduler: Scheduler = system.scheduler

  private val inputHint =
    "\u0412\u0432\u0435\u0434\u0438\u0442\u0435\u0020\u0442\u0435\u043a\u0441\u0442\u0020\u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u044f"
  var partner: String = _
  var name: String = _
  private var counter = 0

  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    import javafx.beans.value.{ChangeListener, ObservableValue}
    if (counter == 0) {
      timer.scheduleAtFixedRate(timerTask, 1, 1_000)
    }

    ContactList.getSelectionModel.setSelectionMode(SelectionMode.SINGLE)
    ContactList.getSelectionModel.selectedItemProperty.addListener(
      new ChangeListener[Contacts]() {
        override def changed(
            observable: ObservableValue[_ <: Contacts],
            oldValue: Contacts,
            newValue: Contacts
        ): Unit = {
          if (newValue != null)
            partner = newValue.name
          postTextArea.setPromptText(partner + " : " + name)
        }
      }
    )

    ContactList.setCellFactory((_: ListView[Contacts]) =>
      new ListCell[Contacts]() {
        override protected def updateItem(
            item: Contacts,
            empty: Boolean
        ): Unit = {
          super.updateItem(item, empty)
          if (empty || item == null || item.getName == null) setText(null)
          else setText(item.getName)
        }
      }
    )

    MessageList.setCellFactory((_: ListView[Message]) =>
      new ListCell[Message] {
        override protected def updateItem(
            item: Message,
            empty: Boolean
        ): Unit = {
          super.updateItem(item, empty)
          if (empty || item == null || item.getPostText == null) {
            setText(null)
          } else {
            if (item.getFrom != null) {
              setText(item.getFrom + ": " + item.getPostText)
            } else {
              setText(item.getPostText)
            }
            setTextAlignment(
              if (item.getFrom == name) TextAlignment.LEFT
              else TextAlignment.RIGHT
            )
            setAlignment(
              if (item.getFrom == null) Pos.CENTER
              else if (item.getFrom == name) Pos.CENTER_RIGHT
              else Pos.CENTER_LEFT
            )
          }
        }
      }
    )

    initViews()
  }

  private def initViews(): Unit = {
    MessageList.getItems.add(new Message)
    MessageList.getSelectionModel.setSelectionMode(SelectionMode.MULTIPLE)
    loginBtn.setOnAction((_: ActionEvent) => Login())
    sendBtn.setOnAction((_: ActionEvent) => Sender())

    val promptText =
      "\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435!"
    postTextArea.setPromptText(promptText)
  }

  private def Sender(): Unit = {
    if (postTextArea.getText.nonEmpty && partner != null && name != null) {
      val msg = new Message(postTextArea.getText, name, partner)
      Main.sendMessage(msg)
      MessageList.getItems.removeIf(_.getPostText == inputHint)
      MessageList.getItems.add(msg)
      postTextArea.clear()
    }
  }

  private def Login(): Unit = {
    import system.executionContext
    if (nicknameField.getText.nonEmpty && portField.getText.nonEmpty) {
      try {
        val port = portField.getText.toInt
        val systemFuture: Future[ActorSystem[Nothing]] = Main.startUp(port)

        systemFuture.onComplete {
          case Success(newSystem) =>
            Platform.runLater(() => {
              nicknameField.setEditable(false)
              portField.setEditable(false)
              name = nicknameField.getText
              Main.rename(name)
            })
          case Failure(exception) =>
            Platform.runLater(() => {
              println(s"Failed to start actor system: ${exception.getMessage}")
            })
        }
      } catch {
        case _: NumberFormatException =>
          Platform.runLater(() => {
            println("Invalid port number. Please enter a valid integer.")
          })
      }
    }
  }

  def startThread(): Unit = {
    import system.executionContext
    scala.concurrent.Future {
      updateViewMessageList()
      updateViewUserList()
    }
    counter += 1
  }

  private def updateViewMessageList(): Unit = {
    import system.executionContext
    Option(partner).foreach { p =>
      Main.updateMessageList(p).foreach { msg =>
        if (
          !msg.getItems.isEmpty && !MessageList.getItems
            .containsAll(msg.getItems)
        ) {
          Platform.runLater { () =>
            MessageList.getItems.setAll(msg.getItems)
            MessageList.refresh()
          }
        }
      }
    }
  }

  private def updateViewUserList(): Unit = {
    import system.executionContext
    Main.updateListUser().foreach { newList =>
      if (!newList.getItems.isEmpty) {
        val sortedList = newList.getItems.sorted
        Platform.runLater(() => {
          ContactList.getItems.clear()
          ContactList.getItems.addAll(sortedList)
          ContactList.getItems.add(
            new Contacts(
              "",
              "\u041e\u0431\u0449\u0438\u0439\u0020\u0447\u0430\u0442",
              false
            )
          )
        })
      }
    }
  }
}

class MyTimerTask extends TimerTask {
  private var controller: ControllerScala = _

  def upd(controller: ControllerScala): Unit = {
    this.controller = controller
  }

  override def run(): Unit = {
    controller.startThread()
  }
}
")
file:///F:/projects/scala/Akka-Chat/src/main/Scala/org/example/view/ControllerScala.scala
file:///F:/projects/scala/Akka-Chat/src/main/Scala/org/example/view/ControllerScala.scala:28: error: expected identifier; obtained this
  def this(system: ActorSystem[Nothing]) = {
      ^
#### Short summary: 

expected identifier; obtained this