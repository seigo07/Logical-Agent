import java.util.ArrayList;

public class Game {

    private char[][] board;
    private boolean isGameOver;
    private boolean isGameWon;
    private boolean isSatisfiable;
    // Cells on the board
    private ArrayList<Cell> cells;
    // Covered cells on the board
    private ArrayList<Cell> coveredCells;

    /**
     * Constructor
     *
     * @param worldMap
     */
    public Game(char[][] worldMap) {
        this.board = worldMap;
        this.isGameOver = false;
        this.isGameWon = false;
        this.cells = new ArrayList<>();
        this.coveredCells = new ArrayList<>();
        initCells();
    }

    /**
     * Initialises the cells and coveredCells
     */
    private void initCells() {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j++) {
                Cell cell = new Cell(i, j, board[j][i]);
                cells.add(cell);
                coveredCells.add(cell);
            }
        }
    }

    /**
     * Return proved cell.
     *
     * @param x
     * @param y
     * @return proved cell
     */
    public Cell uncoverCell(int x, int y, String type) {
        for (Cell cell: cells) {
            if (cell.x == x && cell.y == y) {
                coveredCells.remove(cell);
                if (cell.getHint() == 't' && type.equals("P1")) {
                    isGameOver = true;
                } else if (checkGameWon()) {
                    if (type.equals("P1")) {
                        isGameOver = true;
                    }
                    isGameWon = true;
                }
                return cell;
            }
        }
        return null;
    }

    /**
     * Check the tornadoes in the remaining cells
     *
     * @return true
     */
    public boolean checkGameWon() {
        if (coveredCells.size() == 0) {
            return true;
        } else {
            for (Cell cell: coveredCells) {
                if (board[cell.y][cell.x] != 't') {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Getter
     *
     * @return board
     */
    public char[][] getBoard() {
        return board;
    }

    /**
     * Getter
     *
     * @return isGameOver
     */
    public boolean isGameOver() {
        return isGameOver;
    }


    /**
     * Getter
     *
     * @return isGameWon
     */
    public boolean isGameWon() {
        return isGameWon;
    }

    /**
     * Getter
     *
     * @return isSatisfiable
     */
    public boolean isSatisfiable() {
        return isSatisfiable;
    }

    /**
     * Setter
     *
     * @param isGameOver
     */
    public void setGameOver(boolean isGameOver) {
        this.isGameOver = isGameOver;
    }

    /**
     * Setter
     *
     * @param isSatisfiable
     */
    public void setSatisfiable(boolean isSatisfiable) {
        this.isSatisfiable = isSatisfiable;
    }
}
