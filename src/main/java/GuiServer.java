
import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiServer extends Application{
	
	
	public static void main(String[] args) {
		Server serv = new Server();
		launch(args);

	}

	@Override
public void start(Stage primaryStage) {
    //area for server actions
    TextArea logArea = new TextArea();
    logArea.setEditable(false);
    logArea.setWrapText(true);
    // list of users
    ListView<String> userList = new ListView<>();
    userList.setPrefWidth(150);

    BorderPane layout = new BorderPane();
    layout.setCenter(logArea);
    layout.setRight(userList);

    Scene scene = new Scene(layout, 600, 400);
    primaryStage.setScene(scene);
    primaryStage.setTitle("Connect Four Server");
    primaryStage.show();

    // start the  server 
    new Thread(() -> {
        Server server = new Server();
        server.setLogger(msg -> Platform.runLater(() -> logArea.appendText(msg + "\n")));
        server.setOnUserJoin(user -> Platform.runLater(() -> userList.getItems().add(user)));
        server.setOnUserLeave(user -> Platform.runLater(() -> userList.getItems().remove(user)));
        server.startServer();  
    }).start();
}




}
