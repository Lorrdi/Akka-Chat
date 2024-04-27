package org.example.view

import javafx.application.{Application, Platform}
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.stage.Stage
import org.example.Main

import java.io.IOException

class ScalaWindow extends Application {
  private var primaryStage: Stage = _

  def main(): Unit = {
    Application.launch()
  }


  override def start(primaryStage: Stage): Unit = {
    try {
      this.primaryStage = primaryStage
      primaryStage.setTitle("Клиент" + " " + Main.localPort)
      showBaseWindow()
    } catch {
      case e: IOException =>
        e.printStackTrace()
    }
  }

  private def showBaseWindow(): Unit = {
    try {
      val loader: FXMLLoader = new FXMLLoader
      loader.setLocation(classOf[Controller].getResource("/main.fxml"))
      val rootLayout: HBox = loader.load
      val scene: Scene = new Scene(rootLayout)
      primaryStage.setScene(scene)
      primaryStage.show()
    } catch {
      case e: IOException =>
        e.printStackTrace()
    }
  }

  override def stop(): Unit = {
    Main.stop()
    Platform.exit()
    System.exit(0)

    super.stop()
  }
}
