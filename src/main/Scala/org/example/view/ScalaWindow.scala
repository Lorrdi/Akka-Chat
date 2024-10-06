package org.example.view

import javafx.application.{Application, Platform}
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.stage.Stage
import org.example.Main

import scala.util.{Failure, Success, Try}

class ScalaWindow extends Application {
  private var primaryStage: Stage = _

  override def start(primaryStage: Stage): Unit = {
    this.primaryStage = primaryStage
    primaryStage.setTitle("Chat")
    showBaseWindow()
  }

  private def showBaseWindow(): Unit = {
    Try {
      val loader = new FXMLLoader(classOf[Controller].getResource("/main.fxml"))
      val rootLayout = loader.load[HBox]()
      val scene = new Scene(rootLayout)
      primaryStage.setScene(scene)
      primaryStage.show()
    } match {
      case Success(_) => // Window loaded successfully
      case Failure(e) => e.printStackTrace()
    }
  }

  override def stop(): Unit = {
    Main.stop()
    Platform.exit()
    System.exit(0)
  }
}
