import java.util.ArrayList;

import org.logicng.datastructures.Tristate;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Formula;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;
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

    /**
     * Generate the strings of permutations
     *
     * @param list
     * @return permutations
     */
    public ArrayList<ArrayList<String>> getPermutations(ArrayList<String> list) {

        if (list.size() == 0) {
            ArrayList<ArrayList<String>> result = new ArrayList<>();
            result.add(new ArrayList<>());
            return result;
        }

        ArrayList<ArrayList<String>> permutations = new ArrayList<>();
        String firstElement = list.remove(0);
        ArrayList<ArrayList<String>> recursivePermutations = getPermutations(list);

        for (ArrayList<String> rp : recursivePermutations) {
            for (int index = 0; index <= rp.size(); index++) {
                ArrayList<String> p = new ArrayList<>(rp);
                p.add(index, firstElement);
                permutations.add(p);
            }

        }
        return permutations;
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

    /**
     * Build clause based on the surroundings of given cell
     *
     * @param cell
     * @return a logical formula
     */
    public String buildClause(Cell cell) {

        ArrayList<Cell> neighbours = getNeighbours(cell);
        ArrayList<Cell> unknowns = new ArrayList<>();
        ArrayList<Cell> dangerousNeighbours = new ArrayList<>();
        ArrayList<String> dangerousLiterals = new ArrayList<>();

        // Initialise the markedNeighbours and unknowns
        for (Cell c : neighbours) {
            if (c.getHint() == '*') {
                dangerousNeighbours.add(c);
            } else if (c.getHint() == '?') {
                unknowns.add(c);
            }
        }

        // Generate the literals
        ArrayList<String> literals = new ArrayList<>();
        for (Cell unknown: unknowns) {
            literals.add("T" + unknown.x + unknown.y);
        }
        for (Cell dangerousNeighbour : dangerousNeighbours) {
            dangerousLiterals.add("T" + dangerousNeighbour.x + dangerousNeighbour.y);
        }

        int nTornadoes = Character.getNumericValue(cell.getHint());
        int nUnknowns = unknowns.size();
        int nDangers = getTheNumberOfDangers(cell);

        // Get permutations
        ArrayList<ArrayList<String>> permutedClauses = getPermutations(literals);
        for (ArrayList<String> permutedClause : permutedClauses) {
            for (int j = 0; j < nUnknowns - nTornadoes - nDangers; j++) {
                String clause = permutedClause.get(j);
                permutedClause.remove(clause);
                clause = "~" + clause;
                permutedClause.add(0, clause);
            }
        }

        // Build a logical formula
        StringBuilder builder = new StringBuilder();
        for (ArrayList<String> permutedClause : permutedClauses) {
            builder.append("(");
            for (int j = 0; j < permutedClause.size(); j++) {
                String clause = permutedClause.get(j);
                builder.append(clause);
                builder.append("&");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append(")");
            builder.append("|");
        }

        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    /**
     * Build a logical formulas
     *
     * @return kb (String)
     */
    public String buildKB() {

        StringBuilder builder = new StringBuilder();
        for (Cell cell: uncoveredCells) {
            if (getTheNumberOfUnknown(cell) > 0) {
                String clause = buildClause(cell);
                if (clause != "") {
                    builder.append("(");
                    builder.append(clause);
                    builder.append(")");
                    builder.append("&");
                }
            }
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    /**
     * Method for SAT with CNF encoding.
     */
    public void SATWithDNF() {

        Cell targetCell = null;
        boolean isSatisfiable = false;
        try {
            // Build KB based on the uncoveredCells
            String kbString = buildKB();
            // Convert the KB into a logical formula
            for (Cell cell : unprovedCells) {
                String clause = "&T" + cell.x + cell.y;
                Formula formula = p.parse(kbString+clause);
                // Convert a logical formula to a CNF encoding
                SATSolver miniSat= MiniSat.miniSat(f);
                miniSat.add(formula);
                Tristate result = miniSat.sat();
                if (result.toString().equals("FALSE")) {
                    targetCell = cell;
                    isSatisfiable = true;
                    break;
                }
            }
            if (isSatisfiable) {
                proveCell(targetCell);
            } else {
                game.setSatisfiable(false);
            }
        } catch (ParserException e) {
            System.out.println("ParserException: " + e.getMessage());
        }
    }

    /**
     * Method for SAT with CNF encoding.
     */
    public void SATWithCNF() {

        ISolver solver;
        Cell targetCell = null;
        boolean isSatisfiable = false;
        try {
            // Build KB based on the uncoveredCells
            String kbString = buildKB();
            DIMACS dimacs = new DIMACS();
            // Convert the KB into a logical formula
            Formula formula = p.parse(kbString);
            // Convert a logical formula to a CNF encoding
            int[][] dimacsClauses = dimacs.buildDIMACS(formula);
            solver = SolverFactory.newDefault();
            solver.newVar(1000);
            solver.setExpectedNumberOfClauses(50000);
            for (int j = 0; j < dimacsClauses.length; j++) {
                VecInt vecInt = new VecInt(dimacsClauses[j]);
                solver.addClause(vecInt);
            }
            // Check the satisfiability of including a tornado
            for (Cell cell : unprovedCells) {
                String clause = "T" + cell.x + cell.y;
                if (dimacs.getLiterals().containsKey(clause)) {
                    int literal = dimacs.getLiterals().get(clause);
                    int[] literals = new int[]{literal};
                    if (!solver.isSatisfiable(new VecInt(literals))) {
                        targetCell = cell;
                        isSatisfiable = true;
                        break;
                    }
                }
            }
            if (isSatisfiable) {
                proveCell(targetCell);
            } else {
                game.setSatisfiable(false);
            }
        } catch (ParserException e) {
            System.out.println("ParserException: " + e.getMessage());
        } catch (ContradictionException e) {
            System.out.println("ContradictionException: " + e.getMessage());
        } catch (TimeoutException e) {
            System.out.println("TimeoutException: " + e.getMessage());
        }
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
        game.setSatisfiable(true);
        while (game.isSatisfiable()) {
            uncoverNeighbours();
            SATWithDNF();
        }
        while (unprovedCells.size() > 0 && !game.isGameOver()) {
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
     * Play Intermediate Tornado Sweeper Agent with CNF encoding
     */
    public void playIntermediateCNF() {
        game.setSatisfiable(true);
        while (game.isSatisfiable()) {
            uncoverNeighbours();
            SATWithCNF();
        }
        while (unprovedCells.size() > 0 && !game.isGameOver()) {
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
}
