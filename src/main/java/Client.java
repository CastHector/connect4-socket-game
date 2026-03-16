import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;




public class Client extends Thread{
	static final long serialVersionUID = 42L;
	
	Socket socketClient;
	private String username;

	ObjectOutputStream out;
	ObjectInputStream in;
	private Consumer<Message> messageHandler;
	private boolean connected = false;
	private boolean inGame = false;
	private String opponent;
	private GameSession currentGame;
	private Thread listenThread;


	
	public void run() {
		
		try {
			socketClient = new Socket("127.0.0.1", 5556);
        
			// Create output stream FIRST, then input stream
	    	out = new ObjectOutputStream(socketClient.getOutputStream());
	    	out.flush(); // Make sure to flush after creating the output stream
	    	in = new ObjectInputStream(socketClient.getInputStream());
			socketClient.setTcpNoDelay(true);
			connected = true;

			listenThread = new Thread(this::listenForMessages);
            listenThread.setDaemon(true);
            listenThread.start();
            

		}
		catch(IOException e) {
			System.out.println("Connection error: " + e.getMessage());
		}
		
    }
	
	public void send(Message message) {
		
		try {
			out.writeObject(message);
			out.flush();
		} catch (IOException e) {
			
			System.out.println("Error sending message: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public Client(Consumer<Message> messageHandler) {
		this.messageHandler = messageHandler;
	}

    // New method to join the server once we're connected
    public void joinServer() {
        if (connected && username != null && !username.isEmpty()) {
            send(new Message(Message.MessageType.JOIN, username, username));
        }
    }

	private void listenForMessages() {
		try {
			while (connected) {
				try {
					Object receivedObj = in.readObject();
					
					if (receivedObj instanceof Message) {
						Message message = (Message) receivedObj;
						processMessage(message);
						messageHandler.accept(message);
					} else {
						System.out.println("Received non-Message object: " + receivedObj.getClass().getName());
					}
				} catch (ClassNotFoundException e) {
					System.out.println("Unknown object type received: " + e.getMessage());
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			if (connected) {
				System.out.println("Error receiving message: " + e.getMessage());
				connected = false;
				messageHandler.accept(new Message(Message.MessageType.QUIT,"Server","Connection lost"));
			}
		}
	}

	private void processMessage(Message message) {
		// handle messages based on type
		switch (message.getType()) {
			case GAME_START:
				inGame = true;
				currentGame = message.getGameState();
				if (currentGame.getPlayer1().equals(username)) {
					opponent = currentGame.getPlayer2();
				} else {
					opponent = currentGame.getPlayer1();
				}
				break;

			case MOVE:
				currentGame = message.getGameState();
				break;

			case GAME_END:
				currentGame = message.getGameState();
				break;

			case REMATCH:
				currentGame = message.getGameState();
				break;

			case QUIT:
				if (message.getSender().equals(opponent)) {
					inGame = false;
					opponent = null;
				}
				break;

		}
	}


	public boolean isConnected() {
		return connected;
	}

	public void closeConnection() { 
		// send quit message to server
		if(username != null) {
			send(new Message(Message.MessageType.QUIT, username, ""));
		}

		connected = false;
		try {
			// close the socket
			if (socketClient != null && !socketClient.isClosed()) {
				socketClient.close();
			}
		} catch (IOException e) {
			System.out.println("Error closing connection: " + e.getMessage());
		}
	}

	public void makeMove(int column) {
		// send move message to server
		if (inGame) {
			Message message = new Message(Message.MessageType.MOVE, username, String.valueOf(column));
			send(message);
		}
	}

	public void sendChat(String message) {
		// send chat message to server
		if (inGame) {
			Message chatMessage = new Message(Message.MessageType.CHAT, username, message);
			send(chatMessage);
		}
	}
	

	public void requestRematch() {
		// send rematch request to server
		if (inGame) {
			Message rematchMessage = new Message(Message.MessageType.REMATCH, username, "");
			send(rematchMessage);
		}
	}

	public void quit() {
		// send quit message to server
		Message quitMessage = new Message(Message.MessageType.QUIT, username, "");
		send(quitMessage);
		closeConnection();
	}

	public String getUsername() {
		return username;
	}

	public boolean isInGame() {
		return inGame;
	}

	public String getOpponent() {
		return opponent;
	}
	
	public GameSession getCurrentGame() {
		return currentGame;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
}