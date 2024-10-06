package org.example.view

import javafx.fxml.FXML
import javafx.scene.control.{Button, ListView, SplitPane, TextArea, TextField}


class Controller {
  var path = "main.fxml"
  @FXML protected var MessageList: ListView[Message] = _
  @FXML protected var ContactList: ListView[Contacts] = _
  @FXML protected var sendBtn: Button = _
  @FXML protected var loginBtn: Button = _
  @FXML protected var windowFX: SplitPane = _
  @FXML protected var postTextArea: TextArea = _
  @FXML protected var nicknameField: TextField = _
  @FXML private[example] def initialize(): Unit = {
    assert(MessageList != null, "fx:id=\"messageList\" was not injected: check your FXML file 'first.fxml'.")
    assert(sendBtn != null, "fx:id=\"sendBtn\" was not injected: check your FXML file 'first.fxml'.")
    assert(postTextArea != null, "fx:id=\"postTextArea\" was not injected: check your FXML file 'first.fxml'.")
  }
}
