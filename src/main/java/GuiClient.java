import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class GuiClient extends Application {
	static final long serialVersionUID = 42L;
    private Client client;
    private Stage primaryStage;
    private BorderPane gameBoard;
    private GridPane gameCells;
    private Circle[][] circles;
    private TextArea chatArea;
    private TextField chatInput;
    private Label statusLabel;
    private Button rematchButton;
    private Label turnLabel;
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Connect Four");
        
        // start with login screen
        showLoginScreen();
        
        primaryStage.setOnCloseRequest(e -> {
            if (client != null && client.isConnected()) {
                client.quit();
            }
        });
        
        primaryStage.show();
    }
    
    private void showLoginScreen() {
        VBox loginBox = new VBox(10);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Connect Four");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        
        Label nameLabel = new Label("Enter your username:");
        TextField usernameField = new TextField();
        usernameField.setMaxWidth(200);
        
        Label statusLabel = new Label("");
        statusLabel.setTextFill(Color.RED);
        
        Button connectButton = new Button("Connect");
        connectButton.setDefaultButton(true);
        
        connectButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            if (username.isEmpty()) {
                statusLabel.setText("Username cannot be empty");
                return;
            }
            
            client = new Client(this::handleMessage);
            client.setUsername(username); // set the username first
            client.start(); // start the Client thread 
            
            statusLabel.setText("Connecting...");
            connectButton.setDisable(true);
            
            // wait a to make sure connection is established 
            new Thread(() -> {
                try {
                    Thread.sleep(500); 
                    
                    // once connected send the JOIN message
                    if (client.isConnected()) {
                        client.joinServer();
                    } else {
                        Platform.runLater(() -> {
                            statusLabel.setText("Connection failed");
                            connectButton.setDisable(false);
                        });
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });
        // create the login screen layout
        loginBox.getChildren().addAll(titleLabel, nameLabel, usernameField, connectButton, statusLabel);
        Scene loginScene = new Scene(loginBox, 400, 300);
        primaryStage.setScene(loginScene);
    }
    
    private void handleMessage(Message message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case JOIN_SUCCESS:
                    showWaitingScreen();
                    break;
                case JOIN_FAIL:
                    showLoginFailure(message.getContent());
                    break;
                case GAME_START:
                    showGameScreen(message.getGameState());
                    break;
                case MOVE:
                    updateGameBoard(message.getGameState());
                    break;
                case GAME_END:
                    handleGameEnd(message.getGameState(), message.getContent());
                    break;
                case CHAT:
                    handleChatMessage(message);
                    break;
                case REMATCH:
                    handleRematch(message.getGameState());
                    break;
                case QUIT:
                    handleOpponentQuit(message.getSender(), message.getContent());
                    break;
                default:
                    System.out.println("Unhandled message type: " + message.getType());
            }
        });
    }
    
    private void showLoginFailure(String message) {
        //failure message
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Login Failed");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        
        // return to login screen
        showLoginScreen();
    }
    
    private void showWaitingScreen() {
        // create waiting screen layout
        VBox waitingBox = new VBox(10);
        waitingBox.setAlignment(Pos.CENTER);
        waitingBox.setPadding(new Insets(20));
        
        Label waitingLabel = new Label("Waiting for opponent...");
        waitingLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        ProgressIndicator progress = new ProgressIndicator();
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> {
            client.quit();
            showLoginScreen();
        });
        
        waitingBox.getChildren().addAll(waitingLabel, progress, cancelButton);
        Scene waitingScene = new Scene(waitingBox, 400, 300);
        primaryStage.setScene(waitingScene);
    }
 
    private void showGameScreen(GameSession gameState) {
        gameBoard = new BorderPane();
        gameBoard.setPadding(new Insets(10));
        
        // Create the top status area
        VBox topArea = new VBox(5);
        topArea.setAlignment(Pos.CENTER);
        
        String opponent = client.getUsername().equals(gameState.getPlayer1()) ? 
                          gameState.getPlayer2() : gameState.getPlayer1();
        
        Text titleText = new Text("Playing against: " + opponent);
        titleText.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        statusLabel = new Label("Game in progress");
        statusLabel.setFont(Font.font("Arial", 14));
        
        turnLabel = new Label(getPlayerTurnText(gameState));
        turnLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        topArea.getChildren().addAll(titleText, statusLabel, turnLabel);
        gameBoard.setTop(topArea);
        
        // create game grid
        createGameGrid();
        updateGameBoard(gameState);
        
        // create bottom chat area
        VBox chatBox = new VBox(5);
        chatBox.setPadding(new Insets(10, 0, 0, 0));
        
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setPrefHeight(100);
        chatArea.setWrapText(true);
        
        HBox chatControls = new HBox(5);
        chatInput = new TextField();
        chatInput.setPromptText("Type message here...");
        chatInput.setOnAction(e -> sendChat());
        
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendChat());
        
        rematchButton = new Button("Rematch");
        rematchButton.setDisable(true);
        rematchButton.setOnAction(e -> requestRematch());
        
        Button quitButton = new Button("Quit");
        quitButton.setOnAction(e -> {
            client.quit();
            showLoginScreen();
        });
        
        chatControls.getChildren().addAll(chatInput, sendButton, rematchButton, quitButton);
        chatControls.setAlignment(Pos.CENTER_RIGHT);
        
        chatBox.getChildren().addAll(new Label("Chat:"), chatArea, chatControls);
        gameBoard.setBottom(chatBox);
        
        Scene gameScene = new Scene(gameBoard, 700, 600);
        primaryStage.setScene(gameScene);
        primaryStage.setTitle("Connect Four - " + client.getUsername());
    }
    
    private void createGameGrid() {
        gameCells = new GridPane();
        gameCells.setAlignment(Pos.CENTER);
        gameCells.setPadding(new Insets(10));
        gameCells.setHgap(5);
        gameCells.setVgap(5);
        
        circles = new Circle[GameSession.ROWS][GameSession.COLS];
       
        // create circle for each cell
        for (int row = 0; row < GameSession.ROWS; row++) {
            for (int col = 0; col < GameSession.COLS; col++) {
                Circle circle = new Circle(25); 
                circle.setFill(Color.WHITE);
                circle.setStroke(Color.BLACK);
                circles[row][col] = circle;
       
                StackPane cell = new StackPane(); 
                cell.setPrefSize(70, 70);
                cell.setStyle("-fx-background-color: #0000FF; -fx-border-color: black;");
       
                cell.getChildren().add(circle);
       
                final int column = col;
                cell.setOnMouseClicked(e -> makeMove(column));
       
                gameCells.add(cell, col, row);
            }
        }
       
       
        // create column indicator buttons
        HBox columnButtons = new HBox(5);
        columnButtons.setAlignment(Pos.CENTER);
       
        for (int col = 0; col < GameSession.COLS; col++) {
            final int column = col;
            Button dropButton = new Button("↓");
            dropButton.setPrefWidth(70);
            dropButton.setOnAction(e -> makeMove(column));
            columnButtons.getChildren().add(dropButton);
        }
       
        VBox centerBox = new VBox(5);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().addAll(columnButtons, gameCells);
        gameBoard.setCenter(centerBox);
    }

    
    private void updateGameBoard(GameSession gameState) {
        // update the game board based on the current game state
        int[][] board = gameState.getBoard();
        
        for (int row = 0; row < GameSession.ROWS; row++) {
            for (int col = 0; col < GameSession.COLS; col++) {
                switch (board[row][col]) {
                    case 0:
                        circles[row][col].setFill(Color.WHITE);
                        break;
                    case 1:
                        circles[row][col].setFill(Color.RED);
                        break;
                    case 2:
                        circles[row][col].setFill(Color.YELLOW);
                        break;
                }
            }
        }
        // update turn label
        turnLabel.setText(getPlayerTurnText(gameState));
    }
    
    private String getPlayerTurnText(GameSession gameState) {
        if (gameState.isGameOver()) {
            return "Game Over";
        }
        
        int playerNum = client.getUsername().equals(gameState.getPlayer1()) ? 1 : 2;
        if (gameState.getCurrentPlayer() == playerNum) {
            return "Your turn";
        } else {
            return "Opponent's turn";
        }
    }
    
    private void makeMove(int column) {
        if (client.isInGame()) {
            client.makeMove(column);
        }
    }
    
    private void sendChat() {
        // send chat message to server
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            client.sendChat(message);
            chatArea.appendText("You: " + message + "\n");
            chatInput.clear();
        }
    }
    
    private void requestRematch() {
        // send rematch request to server
        client.requestRematch();
        rematchButton.setDisable(true);
        statusLabel.setText("Rematch requested, waiting for opponent...");
    }
    
    private void handleGameEnd(GameSession gameState, String content) {
        statusLabel.setText(content);
        turnLabel.setText("Game Over");
        rematchButton.setDisable(false);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
    
    private void handleChatMessage(Message message) {
        // display chat message in chat area
        chatArea.appendText(message.getSender() + ": " + message.getContent() + "\n");
    }
    
    private void handleRematch(GameSession gameState) {
        // reset game state
        updateGameBoard(gameState);
        statusLabel.setText("Game restarted!");
        rematchButton.setDisable(true);
    }
    
    private void handleOpponentQuit(String sender, String content) {
        if (client.isInGame()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Opponent Left");
            alert.setHeaderText(null);
            alert.setContentText("Your opponent has left the game");
            alert.show();
            
            statusLabel.setText("Opponent has left. Returning to waiting room...");
            showWaitingScreen();
        } else {
            showLoginScreen();
        }
    }
}