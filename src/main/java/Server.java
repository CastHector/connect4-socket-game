import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Server {
    static final long serialVersionUID = 42L;
    int count = 1;
    ArrayList<ClientThread> clients = new ArrayList<>();
    private List<String> matchmakingPlayers = new ArrayList<>();
    private Map<String, GameSession> gameSessions = new HashMap<>();
    private Map<String, String> matchedPlayers = new HashMap<>();

    private Consumer<String> logger = System.out::println;
    private Consumer<String> onUserJoin = user -> {};
    private Consumer<String> onUserLeave = user -> {};

    public void setLogger(Consumer<String> logger) {
        this.logger = logger;
    }

    public void setOnUserJoin(Consumer<String> onUserJoin) {
        this.onUserJoin = onUserJoin;
    }

    public void setOnUserLeave(Consumer<String> onUserLeave) {
        this.onUserLeave = onUserLeave;
    }

    public void startServer() {
        new TheServer().start();
    }

    private class TheServer extends Thread {
        public void run() {
            try (ServerSocket mysocket = new ServerSocket(5556)) {
                logger.accept("Server is waiting for a client!");
                while (true) {
                    ClientThread c = new ClientThread(mysocket.accept(), count);
                    clients.add(c);
                    c.start();
                    count++;
                }
            } catch (IOException e) {
                logger.accept("[Server] Error starting server: " + e.getMessage());
            }
        }
    }

    class ClientThread extends Thread {
        Socket connection;
        int count;
        ObjectInputStream in;
        ObjectOutputStream out;
        String username;

        ClientThread(Socket s, int count) {
            this.connection = s;
            this.count = count;
        }

        public void updateClients(String message) {
            logger.accept(message);
        }
        
        public void sendMessage(Message message) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                logger.accept("Error sending message to client: " + e.getMessage());
            }
        }

        private synchronized void handleJoin(Message message) {
            username = message.getSender();

            for (ClientThread c : clients) {    //check if username is already taken
                if (c != this && c.username.equals(username)) {
                    sendMessage(new Message(Message.MessageType.JOIN_FAIL, "Server", "Username already taken"));
                    return;
                }
            }
            // send welcome message to the client
            sendMessage(new Message(Message.MessageType.JOIN_SUCCESS, "Server", "Welcome " + username));
            logger.accept("New Client: " + username);
            onUserJoin.accept(username);
            //add player to matchmaking queue
            synchronized (matchmakingPlayers) {
                matchmakingPlayers.add(username);
                matchPlayers();
            }
          
        }

        private synchronized void matchPlayers() {
            synchronized (matchmakingPlayers) {
                if (matchmakingPlayers.size() >= 2) {
                    //pull player from list
                    String player1 = matchmakingPlayers.remove(0);
                    String player2 = matchmakingPlayers.remove(0);
                    //create game session
                    GameSession gameSession = new GameSession(player1, player2);
                    String matchID = getSessionId(player1, player2);
                    gameSessions.put(matchID, gameSession);
                    // add players to matched players map
                    matchedPlayers.put(player1, player2);
                    matchedPlayers.put(player2, player1);
                    // send game start message to both players
                    Message startMessage = new Message(Message.MessageType.GAME_START, "Server", "Game Started!", gameSession);
                    // sendgame start message to both players
                    for (ClientThread c : clients) {
                        if (c.username.equals(player1) || c.username.equals(player2)) {
                            c.sendMessage(startMessage);
                        }
                    }
                    logger.accept("Game started between " + player1 + " and " + player2);
                }
            }
        }

        private synchronized void handleMove(Message message) {
            String player = message.getSender();
            int column = Integer.parseInt(message.getContent());

            if (!matchedPlayers.containsKey(player)) {
                logger.accept("Player not found in matched players");
                return;
            }

            String opponent = matchedPlayers.get(player);
            String matchID = getSessionId(player, opponent);
            GameSession gameSession = gameSessions.get(matchID);

            if (gameSession == null) {
                logger.accept("Game session not found for match ID: " + matchID);
                return;
            }
            //non active players cannot make moves
            int playerNumber = gameSession.getPlayer1().equals(player) ? 1 : 2;
            if (gameSession.getCurrentPlayer() != playerNumber) {
                logger.accept("Not " + player + "'s turn");
                return;
            }

            boolean moveSuccess = gameSession.makeMove(playerNumber, column);
            if (moveSuccess) {
                //
                Message moveMessage = new Message(Message.MessageType.MOVE, player, String.valueOf(column), gameSession);
                // send move message to both players
                for (ClientThread c : clients) {
                    if (c.username.equals(player) || c.username.equals(opponent)) {
                        c.sendMessage(moveMessage);
                    }
                }
                // check if game is over
                if (gameSession.isGameOver()) {
                    String result = (gameSession.getWinner() == 0) ? "It's a draw!" :
                            "Player " + (gameSession.getWinner() == 1 ? gameSession.getPlayer1() : gameSession.getPlayer2()) + " wins!";
                    
                    Message endMessage = new Message(Message.MessageType.GAME_END, "Server", result, gameSession);
                    // send game end message to both players
                    for (ClientThread c : clients) {
                        if (c.username.equals(player) || c.username.equals(opponent)) {
                            c.sendMessage(endMessage);
                        }
                    }
                    logger.accept("Game between " + player + " and " + opponent + " ended.");
                }
            }
        }

        private synchronized void handleChat(Message message) {
            String sender = message.getSender();
            if (!matchedPlayers.containsKey(sender)) return;

            String receiver = matchedPlayers.get(sender);
            // send chat message to the receiver
            for (ClientThread c : clients) {
                if (c.username.equals(receiver)) {
                    c.sendMessage(message);
                    break;
                }
            }
        }

        private synchronized void handleRematch(Message message) {
            String player = message.getSender();

            if (!matchedPlayers.containsKey(player)) return;

            String opponent = matchedPlayers.get(player);
            String sessionId = getSessionId(player, opponent);
            GameSession gameSession = gameSessions.get(sessionId);

            if (gameSession == null) return;

            gameSession.resetGame();

            Message rematchMessage = new Message(Message.MessageType.REMATCH, player, "Rematch requested", gameSession);
            // send rematch message to both players
            for (ClientThread c : clients) {
                if (c.username.equals(player) || c.username.equals(opponent)) {
                    c.sendMessage(rematchMessage);
                }
            }
            logger.accept("Rematch requested between " + player + " and " + opponent);
        }

        private synchronized void handleQuit(Message message) {
            String player = message.getSender();

            if (!matchedPlayers.containsKey(player)) return;

            String opponent = matchedPlayers.get(player);
            String sessionId = getSessionId(player, opponent);
            // remove game and players from maps
            gameSessions.remove(sessionId);
            matchedPlayers.remove(player);
            matchedPlayers.remove(opponent);

            Message quitMessage = new Message(Message.MessageType.QUIT, player, "Opponent disconnected");
            // send quit message to both players
            for (ClientThread c : clients) {
                if (c.username.equals(opponent)) {
                    c.sendMessage(quitMessage);
                    onUserLeave.accept(player);
                    synchronized (matchmakingPlayers) {
                        //add leftover player back to matchmaking
                        matchmakingPlayers.add(opponent);
                    }
                    matchPlayers();
                    break;
                }
            }
        }

        private String getSessionId(String player1, String player2) {
            return player1.compareTo(player2) < 0 ? player1 + "-VS-" + player2 : player2 + "-VS-" + player1;
        }

        public void run() {
            try {
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());
                connection.setTcpNoDelay(true);

                while (true) {
                    try {
                        Object receivedObject = in.readObject();
                        //determine which action to take based on the message type
                        if (receivedObject instanceof Message) {
                            Message message = (Message) receivedObject;
                            logger.accept("Received: " + message);
                            switch (message.getType()) {
                                case JOIN: handleJoin(message); break;
                                case MOVE: handleMove(message); break;
                                case CHAT: handleChat(message); break;
                                case REMATCH: handleRematch(message); break;
                                case QUIT: handleQuit(message); break;
                                default: logger.accept("[Error] Unknown message type: " + message.getType());
                            }
                        }
                    } catch (Exception e) {
                        if (username != null) {
                            handleQuit(new Message(Message.MessageType.QUIT, username, ""));
                        }
                        clients.remove(this);
                        onUserLeave.accept(username);
                        logger.accept(username + " has disconnected.");
                        break;
                    }
                }
            } catch (Exception e) {
                logger.accept("[Error] ClientThread crashed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
