import java.util.ArrayList;

import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Formula;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

public class Agent {

    private String type;
    private boolean verbose;
    private Game game;
    // Agent's board. which is distinct from the game's one
    private char[][] board;
    // Cells on the board
    private ArrayList<Cell> cells;
    // Unproved cells on the board
    private ArrayList<Cell> unprovedCells;
    // Proved cells on the board
    private ArrayList<Cell> provedCells;
    // Uncovered cells on the board
    private ArrayList<Cell> uncoveredCells;
    // Tornado cells
    private ArrayList<Cell> tornadoCells;
    // The length of the board
    private int boardLength;
    // The number of cells whose hint is 0 and neighbours have not been probed yet.
    private int cellsWithFreeNeighbours;
    private FormulaFactory f = new FormulaFactory();
    private PropositionalParser p = new PropositionalParser(f);

    /**
     * Constructor
     *
     * @param game
     */
    public Agent(String type, boolean verbose, Game game) {
        this.type = type;
        this.verbose = verbose;
        this.game = game;
        this.boardLength = this.game.getBoard().length;
        this.board = new char[boardLength][boardLength];
        this.cells = new ArrayList<>();
        this.unprovedCells = new ArrayList<>();
        this.provedCells = new ArrayList<>();
        this.tornadoCells = new ArrayList<>();
        this.uncoveredCells = new ArrayList<>();
        this.cellsWithFreeNeighbours = 0;
        initBoard();
        initCells();
        proveHintCells();
    }

    /** -------------------------------------------- GENERAL METHODS ------------------------------------------------**/


    /**
     * Method which creates permutations of strings in a list. Used to generate the encoding of the knowledge base
     * as a string.
     *
     * @param list the list containing the strings to be permuted
     * @return a list of the permutations i.e. a list containing the permutations of the list passed as a parameter
     * code adapted from: https://stackoverflow.com/questions/24460480/permutation-of-an-arraylist-of-numbers-using-recursion
     */
    public ArrayList<ArrayList<String>> listPermutations(ArrayList<String> list) {

        if (list.size() == 0) {
            ArrayList<ArrayList<String>> result = new ArrayList<>();
            result.add(new ArrayList<>());
            return result;
        }

        ArrayList<ArrayList<String>> returnMe = new ArrayList<>();

        String firstElement = list.remove(0);

        ArrayList<ArrayList<String>> recursiveReturn = listPermutations(list);
        for (ArrayList<String> li : recursiveReturn) {
            for (int index = 0; index <= li.size(); index++) {
                ArrayList<String> temp = new ArrayList<>(li);
                temp.add(index, firstElement);
                returnMe.add(temp);
            }

        }
        return returnMe;
    }

    /**
     * Initialises the agent board.
     */
    public void initBoard() {
        // Set '?' at first
        for (int i = 0; i < boardLength; i++) {
            for (int j = 0; j < boardLength; j++) {
                board[j][i] = '?';
            }
        }
        if (this.verbose) {
            A3main.printBoard(board);
        }
    }

    /**
     * Initialises the cells and unprovedCells
     */
    public void initCells() {
        for (int i = 0; i < boardLength; i++) {
            for (int j = 0; j < boardLength; j++) {
                Cell cell = new Cell(j, i, '?');
                cells.add(cell);
                unprovedCells.add(cell);
            }
        }
    }

    /**
     * Prove top-left and center
     */
    public void proveHintCells() {
        Cell cell = getCell(0, 0);
        proveCell(cell);
        if (!this.type.equals("P1")) {
            cell = getCell(boardLength / 2, boardLength / 2);
            proveCell(cell);
        }
    }

    /**
     * Return the cell from the unprovedCells with coordinates
     *
     * @param x
     * @param y
     * @return unprovedCell with coordinates x and y
     */
    public Cell findUnprovedCell(int x, int y) {
        for (int i = 0; i < unprovedCells.size(); i++) {
            if (unprovedCells.get(i).x == x && unprovedCells.get(i).y == y) {
                return unprovedCells.get(i);
            }
        }
        return null;
    }

    /**
     * Return the cell from the cells with coordinates
     *
     * @param x
     * @param y
     * @return cell with coordinates x and y
     */
    public Cell getCell(int x, int y) {
        for (Cell cell : cells) {
            if (cell.x == x && cell.y == y) {
                return cell;
            }
        }
        return null;
    }

    /**
     * Uncovers cell, updates the lists, and prints out the board
     *
     * @param cell
     */
    public void proveCell(Cell cell) {
        Cell targetCell = getCell(cell.x, cell.y);
        Cell uncoveredCell = game.uncoverCell(cell.x, cell.y, this.type);
        cell.setHint(uncoveredCell.getHint(), this.type);
        targetCell.setHint(uncoveredCell.getHint(), this.type);
        unprovedCells.remove(cell);
        provedCells.add(cell);
        uncoveredCells.add(cell);
        board[cell.y][cell.x] = cell.getHint();
        if (cell.getHint() == '0') {
            cellsWithFreeNeighbours++;
        }
    }

    /**
     * Set hint as a danger
     *
     * @param cell which is dangerous
     */
    public void setDanger(Cell cell) {
        Cell targetCell = getCell(cell.x, cell.y);
        cell.setHint('*', this.type);
        targetCell.setHint('*', this.type);
        tornadoCells.add(cell);
        provedCells.add(cell);
        unprovedCells.remove(cell);
        board[cell.y][cell.x] = cell.getHint();
    }

    /**
     * Method which returns whether a Cell object has been examined before
     *
     * @param adjacentCell
     * @return
     */
    public boolean hasBeenExamined(Cell adjacentCell) {
        for (Cell cell : provedCells) {
            if (cell.x == adjacentCell.x && cell.y == adjacentCell.y) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return neighbouring cells
     *
     * @param cell
     * @return neighbouring cells
     */
    public ArrayList<Cell> getNeighbours(Cell cell) {

        ArrayList<Cell> neighbours = new ArrayList<>();

        if (cell.x > 0 && cell.y > 0) {
            Cell neighbourCell = getCell(cell.x - 1, cell.y - 1);
            if (neighbourCell != null) {
                neighbours.add(neighbourCell);
            }
        }
        if (cell.x > 0) {
            Cell neighbourCell = getCell(cell.x - 1, cell.y);
            if (neighbourCell != null) {
                neighbours.add(neighbourCell);
            }
        }
        if (cell.y > 0) {
            Cell neighbourCell = getCell(cell.x, cell.y - 1);
            if (neighbourCell != null) {
                neighbours.add(neighbourCell);
            }
        }
        if (cell.x < boardLength - 1 && cell.y < boardLength - 1) {
            Cell neighbourCell = getCell(cell.x + 1, cell.y + 1);
            if (neighbourCell != null) {
                neighbours.add(neighbourCell);
            }
        }
        if (cell.x < boardLength - 1) {
            Cell neighbourCell = getCell(cell.x + 1, cell.y);
            if (neighbourCell != null) {
                neighbours.add(neighbourCell);
            }
        }
        if (cell.y < boardLength - 1) {
            Cell neighbourCell = getCell(cell.x, cell.y + 1);
            if (neighbourCell != null) {
                neighbours.add(neighbourCell);
            }
        }

        return neighbours;
    }

    /**
     * Uncovers neighbours whose hint is 0 with no tornadoes around them
     */
    public void uncoverNeighbours() {
        while (cellsWithFreeNeighbours != 0 && !game.isGameWon()) {
            ArrayList<Cell> adjacentCells = new ArrayList<>();
            for (Cell cell : provedCells) {
                if (cell.getHint() == '0') {
                    if (cell.x > 0 && cell.y > 0) {
                        Cell adjacentCell = findUnprovedCell(cell.x - 1, cell.y - 1);
                        if (adjacentCell != null) {
                            adjacentCells.add(adjacentCell);
                        }
                    }
                    if (cell.x > 0) {
                        Cell adjacentCell = findUnprovedCell(cell.x - 1, cell.y);
                        if (adjacentCell != null) {
                            adjacentCells.add(adjacentCell);
                        }
                    }
                    if (cell.y > 0) {
                        Cell adjacentCell = findUnprovedCell(cell.x, cell.y - 1);
                        if (adjacentCell != null) {
                            adjacentCells.add(adjacentCell);
                        }
                    }
                    if (cell.x < boardLength - 1 && cell.y < boardLength - 1) {
                        Cell adjacentCell = findUnprovedCell(cell.x + 1, cell.y + 1);
                        if (adjacentCell != null) {
                            adjacentCells.add(adjacentCell);
                        }
                    }
                    if (cell.x < boardLength - 1) {
                        Cell adjacentCell = findUnprovedCell(cell.x + 1, cell.y);
                        if (adjacentCell != null) {
                            adjacentCells.add(adjacentCell);
                        }
                    }
                    if (cell.y < boardLength - 1) {
                        Cell adjacentCell = findUnprovedCell(cell.x, cell.y + 1);
                        if (adjacentCell != null) {
                            adjacentCells.add(adjacentCell);
                        }
                    }

                }
            }
            for (Cell adjacentCell : adjacentCells) {
                if (!hasBeenExamined(adjacentCell)) {
                    proveCell(adjacentCell);
                }
            }
            cellsWithFreeNeighbours--;
        }
    }

    /**
     * Return the number of dangered
     *
     * @param cell
     * @return the number of dangered around the passed cell
     */
    public int getTheNumberOfDangers(Cell cell) {
        int nDangers = 0;
        ArrayList<Cell> neighbours = getNeighbours(cell);
        for (Cell neighbour : neighbours) {
            if (neighbour.getHint() == '*') {
                nDangers++;
            }
        }
        return nDangers;
    }

    /**
     * Return the number of unproved cells
     *
     * @param cell
     * @return the number of unproved cells
     */
    public int getTheNumberOfUnknown(Cell cell) {
        int nUnknowns = 0;
        ArrayList<Cell> neighbours = getNeighbours(cell);
        for (Cell neighbour : neighbours) {
            if (neighbour.getHint() == '?') {
                nUnknowns++;
            }
        }
        return nUnknowns;
    }

    /**
     * Check whether the cell is in an AFN situation.
     *
     * @param cell
     * @return true if the cell is in an AFN situation
     */
    public boolean isAFN(Cell cell) {
        ArrayList<Cell> neighbours = getNeighbours(cell);
        for (Cell neighbour : neighbours) {
            if (neighbour.getHint() != '?' && neighbour.getHint() != '*') {
                if (getTheNumberOfDangers(neighbour) == Character.getNumericValue(neighbour.getHint())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check whether the cell is in an AMN situation
     *
     * @param cell
     * @return true if the cells is in an AMN situation
     */
    public boolean isAMN(Cell cell) {
        ArrayList<Cell> neighbours = getNeighbours(cell);
        for (Cell neighbour : neighbours) {
            if (neighbour.getHint() != '?' && neighbour.getHint() != '*') {
                if (getTheNumberOfUnknown(neighbour) == (Character.getNumericValue(neighbour.getHint() - getTheNumberOfDangers(neighbour)))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method for single point strategy
     */
    public void SPS() {
        // Check AFN or AMN
        boolean isAFNorAMN = false;
        for (Cell cell : unprovedCells) {
            if (isAFN(cell)) {
                isAFNorAMN = true;
                proveCell(cell);
                break;
            } else if (isAMN(cell)) {
                isAFNorAMN = true;
                setDanger(cell);
                break;
            }
        }
        if (!isAFNorAMN) {
            game.setGameOver(true);
        }
    }

    /** -------------------------------------------- SAT METHODS ------------------------------------------------**/


    /**
     * Method which takes an uncovered Cell object as a parameter and it evaluates its surroundings in order to construct
     * a logical formula.
     *
     * @param cell
     * @return a String representing a logical formula with information about the parameter's surrounding cells.
     */
    public String createClause(Cell cell) {

        // get all the neighbours of the cell
        ArrayList<Cell> neighbours = getNeighbours(cell);
        // contains unknown neighbours of parameter cell
        ArrayList<Cell> unknownCells = new ArrayList<>();
        // contains marked neighbours of parameter cell
        ArrayList<Cell> markedNeighbours = new ArrayList<>();
        ArrayList<String> markedLiterals = new ArrayList<>();

        // populate the markedNeighbours and unknownCells lists
        for (Cell myCell : neighbours) {
            if (myCell.getHint() == '*') {
                markedNeighbours.add(myCell);
            } else if (myCell.getHint() == '?') {
                unknownCells.add(myCell);
            }
        }

        // create the literals of each cell
        ArrayList<String> literals = new ArrayList<>();
        for (Cell unknownCell : unknownCells) {
            literals.add("T" + unknownCell.x + unknownCell.y);
        }
        for (Cell markedCell : markedNeighbours) {
            markedLiterals.add("T" + markedCell.x + markedCell.y);
        }

        // number of neighbouring tornado cells
        int nTornadoes = Character.getNumericValue(cell.getHint());
        // number of neighbouring cells that are unknown
        int nUnknowns = unknownCells.size();
        // number of neihbouring cells marked as dangers i.e. flagged
        int nMarked = getTheNumberOfDangers(cell);

        // get all the permutations, to be used when adding the negation
        ArrayList<ArrayList<String>> permutedClauses = listPermutations(literals);
        for (int i = 0; i < permutedClauses.size(); i++) {
            ArrayList<String> currentClause = permutedClauses.get(i);
            // nUnknowns - nTornados - nMarked is the number of free/safe cells around cell
            // used to get all possible scenarios
            for (int j = 0; j < nUnknowns - nTornadoes - nMarked; j++) {
                String clause = currentClause.get(j);
                currentClause.remove(clause);
                clause = "~" + clause;
                currentClause.add(0, clause);
            }
        }

        // build the logical formula string
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < permutedClauses.size(); i++) {
            ArrayList<String> currentClause = permutedClauses.get(i);
            stringBuilder.append("(");
            for (int j = 0; j < currentClause.size(); j++) {
                String clause = currentClause.get(j);
                stringBuilder.append(clause);
                stringBuilder.append("&");
            }
            // delete trailing &
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            stringBuilder.append(")");
            stringBuilder.append("|");
        }

        // delete trailing |
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();

    }

    /**
     * Method which taked all uncoveredCells as a parameter, and created a logical formula with all the possibilities
     * of where tornados could possibly be.
     *
     * @param uncoveredCells cells that have been uncovered i.e. probed
     * @return a String representation of the knowledge base.
     */
    public String convertKB(ArrayList<Cell> uncoveredCells) {

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < uncoveredCells.size(); i++) {
            Cell cell = uncoveredCells.get(i);
            if (getTheNumberOfUnknown(cell) > 0) {
                // for each cell, get a single clause
                String clause = createClause(cell);
                if (clause != "") {
                    stringBuilder.append("(");
                    stringBuilder.append(clause);
                    stringBuilder.append(")");
                    stringBuilder.append("&");
                }
            }
        }
        if (stringBuilder.length() > 0) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        return stringBuilder.toString();
    }


    /**
     * Method which carries out the SAT move strategy. It calls the methods required to turn the knowledge base into
     * a logical formula string, then calls the methods that encode this into CNF DIMACS and then uses the SAT4J solver
     * to assess whether a Cell is safe to be probed.
     */
    public boolean makeSATMove() {

        ISolver solver;
        Cell myCell = null;
        String action = "R";
        try {
            // Create the KB from the probed Cells
            String kbString = convertKB(uncoveredCells);
            DIMACSGenerator dimacsGenerator = new DIMACSGenerator();
            // parse the String representing the knowledge base into a logical formula
            Formula formula = p.parse(kbString);
            // convert the formula to a CNF DIMACS encoding
            int[][] dimacsClauses = dimacsGenerator.convertToDIMACS(formula);
            // instantiate the solver
            solver = SolverFactory.newDefault();
            solver.newVar(1000);
            solver.setExpectedNumberOfClauses(50000);
            for (int j = 0; j < dimacsClauses.length; j++) {
                VecInt vecInt = new VecInt(dimacsClauses[j]);
                // add clause to solved
                solver.addClause(vecInt);
            }
            // for every unexamined cells check whether the possibility of it containing a tornado is satisfiable.
            // if not then it means that the cell can be probed safely.
            for (Cell cell : unprovedCells) {
                String clause = "T" + Integer.toString(cell.x) + Integer.toString(cell.y);
                if (dimacsGenerator.getLiteralsHashMap().containsKey(clause)) {
                    int literal = dimacsGenerator.getLiteralsHashMap().get(clause);
                    int[] literalArray = new int[]{literal};
                    if (!solver.isSatisfiable(new VecInt(literalArray))) {
                        myCell = cell;
                        action = "P";
                        break;
                    }
                }
            }
            if (action == "P") {
                proveCell(myCell);
            } else if (action == "R") {
//                System.out.println("SAT could not determine, going Random");
//                makeRandomMove();
                game.setGameOver(true);
            }
        } catch (ParserException e) {
//            System.out.println("Parser Exception: " + e.getMessage());
        } catch (ContradictionException e) {
//            System.out.println("Contradiction Exception: " + e.getMessage());
        } catch (TimeoutException e) {
//            System.out.println("Exception: " + e.getMessage());
        }

        return true;
    }

    /**
     * Play game based on the agent type
     */
    public void playGame() {
        switch (type) {
            case "P1":
                playBasic();
                break;
            case "P2":
                playBeginner();
                break;
            case "P3":
                playIntermediateDNF();
                break;
            case "P4":
                playIntermediateCNF();
                break;
            case "P5":
                //TODO: Part 5
                break;
        }
    }

    /**
     * Play Basic Tornado Sweeper Agent
     */
    public void playBasic() {
        while (!game.isGameOver()) {
            uncoverNeighbours();
            if (!game.isGameWon()) {
                if (this.verbose) {
                    A3main.printBoard(board);
                }
                proveCell(unprovedCells.get(0));
            }
        }
        System.out.println("Final map");
        A3main.printBoard(board);
        if (game.isGameWon()) {
            System.out.println("Result: Agent alive: all solved");
        } else {
            System.out.println("Result: Agent dead: found mine");
        }
    }

    /**
     * Play Beginner Tornado Sweeper Agent
     */
    public void playBeginner() {
        while (!game.isGameOver()) {
            uncoverNeighbours();
            SPS();
        }
        System.out.println("Final map");
        A3main.printBoard(board);
        if (game.isGameWon()) {
            System.out.println("Result: Agent alive: all solved");
        } else if (game.isGameOver()) {
            System.out.println("Result: Agent not terminated");
        } else {
            System.out.println("Result: Agent dead: found mine");
        }
    }

    /**
     * Play Intermediate Tornado Sweeper Agent with DNF encoding
     */
    public void playIntermediateDNF() {
        while (!game.isGameOver()) {
            uncoverNeighbours();
            makeSATMove();
        }
        if (game.isGameWon()) {
            while (unprovedCells.size() > 0) {
                Cell myCell = null;
                for (Cell cell : unprovedCells) {
                    if (cell.getHint() == '?') {
                        myCell = cell;
                    }
                }
                setDanger(myCell);
            }
        }
        System.out.println("Final map");
        A3main.printBoard(board);
        if (game.isGameWon()) {
            System.out.println("Result: Agent alive: all solved");
        } else if (game.isGameOver()) {
            System.out.println("Result: Agent not terminated");
        } else {
            System.out.println("Result: Agent dead: found mine");
        }
    }

    /**
     * Play Intermediate Tornado Sweeper Agent with CNF encoding
     */
    public void playIntermediateCNF() {
        while (!game.isGameOver()) {
            uncoverNeighbours();
            makeSATMove();
        }
        if (game.isGameWon()) {
            while (unprovedCells.size() > 0) {
                Cell myCell = null;
                for (Cell cell : unprovedCells) {
                    if (cell.getHint() == '?') {
                        myCell = cell;
                    }
                }
                setDanger(myCell);
            }
        }
        System.out.println("Final map");
        A3main.printBoard(board);
        if (game.isGameWon()) {
            System.out.println("Result: Agent alive: all solved");
        } else if (game.isGameOver()) {
            System.out.println("Result: Agent not terminated");
        } else {
            System.out.println("Result: Agent dead: found mine");
        }
    }
}
