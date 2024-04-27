package org.example.view

import akka.util.Timeout
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.control.{ListCell, ListView, SelectionMode}
import javafx.scene.text.TextAlignment
import org.example.Main

import java.net.URL
import java.util.{ResourceBundle, Timer, TimerTask}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt


class ControllerScala extends Controller with Initializable {

  private val timerTask = new MyTimerTask()
  private val timer = new Timer(true)
  timerTask.upd(this)

  implicit val timeout: Timeout = Timeout(3.seconds)
  private val inputHint = "\u0412\u0432\u0435\u0434\u0438\u0442\u0435\u0020\u0442\u0435\u043a\u0441\u0442\u0020\u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u044f"
  var partner: String = _
  var name: String = _
  private var counter = 0

  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    import javafx.beans.value.{ChangeListener, ObservableValue}
    if (counter == 0) {
      timer.scheduleAtFixedRate(timerTask, 1, 1_000)
    }

    ContactList.getSelectionModel.setSelectionMode(SelectionMode.SINGLE)
    ContactList.getSelectionModel.selectedItemProperty.addListener(new ChangeListener[Contacts]() {
      override def changed(observable: ObservableValue[_ <: Contacts], oldValue: Contacts, newValue: Contacts): Unit = {
        if (newValue != null)
          partner = newValue.name
        postTextArea.setPromptText(partner + " : " + name)
      }
    })

    ContactList.setCellFactory((_: ListView[Contacts]) => new ListCell[Contacts]() {
      override protected def updateItem(item: Contacts, empty: Boolean): Unit = {
        super.updateItem(item, empty)
        if (empty || item == null || item.getName == null) setText(null)
        else setText(item.getName)
      }
    })

    MessageList.setCellFactory((_: ListView[Message]) => new ListCell[Message] {
      override protected def updateItem(item: Message, empty: Boolean): Unit = {
        super.updateItem(item, empty)
        if (empty || item == null || item.getPostText == null) {
          setText(null)
        } else {
          if (item.getFrom != null) {
            setText(item.getFrom + ": " + item.getPostText)
          } else {
            setText(item.getPostText)
          }
          setTextAlignment(if (item.getFrom == name) TextAlignment.LEFT else TextAlignment.RIGHT)
          setAlignment(
            if (item.getFrom == null) Pos.CENTER
            else if (item.getFrom == name) Pos.CENTER_RIGHT
            else Pos.CENTER_LEFT
          )
        }
      }
    })


    initViews()
  }

  private def initViews(): Unit = {

    //    MessageList.getItems.add(new Message)
    MessageList.getSelectionModel.setSelectionMode(SelectionMode.MULTIPLE)
    loginBtn.setOnAction((_: ActionEvent) => Login())
    sendBtn.setOnAction((_: ActionEvent) => Sender())

    val promptText = "\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435!"
    postTextArea.setPromptText(promptText)
  }

  private def Sender(): Unit = {
    if (!(postTextArea.getText == "")) {
      val msg = new Message(postTextArea.getText, nicknameField.getText, partner)
      if (partner != null && name != null) {

        Main.sendMessage(msg)
        MessageList.getItems.removeIf {
          it =>
            it.getPostText == inputHint
        }
        MessageList.getItems.add(msg)
        MessageList.refresh()

        postTextArea.clear()
      }
    }
  }

  private def Login(): Unit = {
    if (!(nicknameField.getText == "")) {
      Main.rename(nicknameField.getText)
      name = nicknameField.getText
      timerTask.run()
      nicknameField.setEditable(false)
    }
  }

  def startThread(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    new Thread() {
      updateViewMessageList()
      updateViewUserList()
    }.start()

    counter += 1
    Thread.sleep(10)
  }

  private def updateViewMessageList()(implicit ec: ExecutionContext): Unit = {
    if (partner != null) {
      val newMessage = Main.updateMessageList(partner)
      newMessage.map { msg =>
        println(msg.getItems.toString)
        if (!msg.getItems.isEmpty)
          if (!MessageList.getItems.containsAll(msg.getItems)) {
            Platform.runLater { () =>
              MessageList.getItems.setAll(msg.getItems)
              MessageList.refresh()
            }
          }
      }
    }
  }

  private def updateViewUserList()(implicit ec: ExecutionContext): Unit = {
    Main.updateListUser().foreach { newList =>
      if (!newList.getItems.isEmpty) {
        val sortedList = newList.getItems.sorted
        Platform.runLater(() => {
          ContactList.getItems.clear()
          ContactList.getItems.addAll(sortedList)
          ContactList.getItems.add(
            new Contacts("",
              "\u041e\u0431\u0449\u0438\u0439\u0020\u0447\u0430\u0442",
              false))
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