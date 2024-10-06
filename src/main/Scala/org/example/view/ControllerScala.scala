package org.example.view

import akka.util.Timeout
import javafx.application.Platform
import javafx.fxml.{FXML, Initializable}
import javafx.geometry.Pos
import javafx.scene.control.{
  Button,
  ListCell,
  ListView,
  SelectionMode,
  TextArea,
  TextField
}
import javafx.scene.text.TextAlignment
import org.example.Main

import java.net.URL
import java.util.{ResourceBundle, Timer, TimerTask}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

class ControllerScala extends Controller with Initializable {

  private val timer = new Timer(true)
  implicit val timeout: Timeout = Timeout(3.seconds)
  private val inputHint = "Введите текст сообщения"
  private var partner: String = _
  private var name: String = _

  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    initializeListViews()
    initializeButtons()
    initializeTimer()
  }

  private def initializeListViews(): Unit = {
    ContactList.getSelectionModel.setSelectionMode(SelectionMode.SINGLE)
    ContactList.getSelectionModel.selectedItemProperty.addListener {
      (_, _, newValue) =>
        Option(newValue).foreach { contact =>
          partner = contact.name
          postTextArea.setPromptText(s"$partner : $name")
        }
    }

    ContactList.setCellFactory(_ =>
      new ListCell[Contacts] {
        override def updateItem(item: Contacts, empty: Boolean): Unit = {
          super.updateItem(item, empty)
          setText(if (empty || item == null) null else item.name)
        }
      }
    )

    MessageList.setCellFactory(_ =>
      new ListCell[Message] {
        override def updateItem(item: Message, empty: Boolean): Unit = {
          super.updateItem(item, empty)
          if (empty || item == null) {
            setText(null)
          } else {
            setText(
              Option(item.getFrom)
                .map(from => s"$from: ${item.getPostText}")
                .getOrElse(item.getPostText)
            )
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

    MessageList.getItems.add(new Message)
    MessageList.getSelectionModel.setSelectionMode(SelectionMode.MULTIPLE)
  }

  private def initializeButtons(): Unit = {
    loginBtn.setOnAction(_ => login())
    sendBtn.setOnAction(_ => sendMessage())
    postTextArea.setPromptText("Введите сообщение!")
  }

  private def initializeTimer(): Unit = {
    val timerTask = new TimerTask {
      def run(): Unit = updateViews()
    }
    timer.scheduleAtFixedRate(timerTask, 1000, 1000)
  }

  private def sendMessage(): Unit = {
    val messageText = postTextArea.getText
    if (messageText.nonEmpty && partner != null && name != null) {
      val msg = new Message(messageText, name, partner)
      Main.sendMessage(msg)
      MessageList.getItems.removeIf(_.getPostText == inputHint)
      MessageList.getItems.add(msg)
      postTextArea.clear()
    }
  }

  private def login(): Unit = {
    val nickname = nicknameField.getText
    if (nickname.nonEmpty) {
      Main.rename(nickname)
      name = nickname
      nicknameField.setEditable(false)
    }
  }

  private def updateViews(): Future[Unit] = {
    for {
      _ <- updateMessageList()
      _ <- updateUserList()
    } yield ()
  }

  private def updateMessageList(): Future[Unit] = Future {
    Option(partner).foreach { p =>
      Main.updateMessageList(p).foreach { msg =>
        if (
          !msg.getItems.isEmpty && !MessageList.getItems
            .containsAll(msg.getItems)
        ) {
          Platform.runLater { () =>
            MessageList.getItems.setAll(msg.getItems.sorted)
            MessageList.refresh()
          }
        }
      }
    }
  }

  private def updateUserList(): Future[Unit] = Future {
    Main.updateListUser().foreach { newList =>
      if (!newList.getItems.isEmpty) {
        val sortedList = newList.getItems.asScala.toList.sorted
        Platform.runLater { () =>
          {
            ContactList.getItems.setAll(sortedList.asJava)
            ContactList.getItems.add(new Contacts("", "General", false))
            ContactList.refresh()
          }
        }
      }
    }
  }
}
