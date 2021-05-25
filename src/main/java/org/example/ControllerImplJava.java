package org.example;

import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;

import java.net.URL;
import java.util.ResourceBundle;

public class ControllerImplJava extends Controller implements Initializable{
    /*
    @Override
    public void getPost() {
        super.getPost();
    }

    @Override
    public void sendPost(Messenge messenge) {
        super.sendPost(messenge);
    }
*/

    @Override
    public void initialize(URL location, ResourceBundle resources) {

            ListMessenge.setCellFactory(param -> new ListCell<Messenge>() {
                @Override
                protected void updateItem(Messenge item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null || item.getPostText() == null ) {
                        setText(null);
                    } else {
                        setText(item.getPostText());
                    }
                }
            });


        //ListMessenge.getItems().addAll(addTestStudents());
        Messenge n = new Messenge();
        ListMessenge.getItems().add(n);
        ListMessenge.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
       /* ListMessenge.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                showMessenge(newValue);
            });*/
            Send.setOnAction(event -> Sender());



    }
/*
    private void showMessenge(Messenge newValue) {

        if(newValue!= null){

        }else{

        }
    }*/

    private void Sender() {
        if(PostText.getText()!=""){
        Messenge m = new Messenge(PostText.getText());
        Messenge def = new Messenge();
        if(ListMessenge.getItems().get(0).getPostText()=="\u0412\u0432\u0435\u0434\u0438\u0442\u0435\u0020\u0442\u0435\u043a\u0441\u0442\u0020\u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u044f")
            ListMessenge.getItems().remove(0);
        ListMessenge.getItems().add(m);
        PostText.clear();
        }
        PostText.setText("\u0432\u0432\u0435\u0434\u0438\u0442\u0435 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435!");
    }
}
