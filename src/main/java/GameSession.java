import java.io.Serializable;

public class GameSession implements Serializable {
    static final long serialVersionUID = 42L;

    public static final int ROWS = 6;
    public static final int COLS = 7;

    private String player1;
    private String player2;
    private int currentPlayer;
    private int[][] board;
    private int winner; // 0: no winner, 1: player1, 2: player2
    private boolean isGameOver;

    public GameSession(String player1, String player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = 1;
        this.board = new int[ROWS][COLS];
        this.isGameOver = false;
        this.winner = 0;
    }

    public boolean makeMove(int playerNum, int column) {
        //make sure column exists
        if (isGameOver || column < 0 || column >= COLS) {
            return false;
        }

        int row = -1;
        //find lowest point in column
        for( int r = ROWS - 1; r >= 0; r--) {
            if (board[r][column] == 0) {
                row = r;
                break;
            }
        }
        //if column is full, return false
        if (row == -1) {
            return false;
        }

        board[row][column] = currentPlayer;
        if(checkWin(row, column)){
            isGameOver = true;
            winner = currentPlayer;
        }
        else if (isBoardFull()) {
            isGameOver = true;
            winner = 0; 
        }
        else{
            currentPlayer = (currentPlayer == 1) ? 2 : 1; //switch players
        }
        
        return true;
    }

    private boolean checkWin(int row, int col) {
        int player = board[row][col];

        // horizontal win check
        int count = 0;
        for (int c = 0; c < COLS; c++) {
            if (board[row][c] == player) {
                count++;
                if (count == 4) {
                    return true;
                }
            } else {
                count = 0;
            }
        }

        // vertical win check
        count = 0;
        for (int r = 0; r < ROWS; r++) {
            if (board[r][col] == player) {
                count++;
                if (count == 4) {
                    return true;
                }
            } else {
                count = 0;
            }
        }

        // diagonal win check 
        for (int r = 3; r < ROWS; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (board[r][c] == player &&
                    board[r-1][c+1] == player &&
                    board[r-2][c+2] == player &&
                    board[r-3][c+3] == player) {
                    return true;
                }
            }
        }

        // diagonal win check
        for (int r = 0; r <= ROWS - 4; r++) {
            for (int c = 0; c <= COLS - 4; c++) {
                if (board[r][c] == player &&
                    board[r+1][c+1] == player &&
                    board[r+2][c+2] == player &&
                    board[r+3][c+3] == player) {
                    return true;
                }
            }
        }

        return false;
    }
    
    private boolean isBoardFull() {
        for (int c = 0; c < COLS; c++) {
            if (board[0][c] == 0) {
                return false;
            }
        }
        return true;
    }

    public int[][] getBoard() {
        return board;
    }
    public int getCurrentPlayer() {
        return currentPlayer;
    }   

    public boolean isGameOver() {
        return isGameOver;
    }

    public int getWinner() {
        return winner;
    }

    public void resetGame() {
        this.board = new int[ROWS][COLS];
        this.currentPlayer = 1;
        this.isGameOver = false;
        this.winner = 0;
    }

    public void setPlayer1(String player1) {
        this.player1 = player1;
    }
    
    public void setPlayer2(String player2) {
        this.player2 = player2;
    }

    public String getPlayer1() {
        return player1;
    }
    
    public String getPlayer2() {
        return player2;
    }
}