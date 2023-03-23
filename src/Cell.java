public class Cell {

    int x;
    int y;
    char hint;

    /**
     * Constructor
     *
     * @param x
     * @param y
     * @param hint
     */
    public Cell(int x, int y, char hint) {
        this.x = x;
        this.y = y;
        this.hint = hint;
    }

    /**
     * Override toString
     *
     * @return a readable information string for cell
     */
    @Override
    public String toString() {
        return (x + " " + y + " " + "Hint: " + hint);
    }

    /**
     * Getter
     *
     * @return hint
     */
    public char getHint() {
        return hint;
    }

    /**
     * Setter
     *
     * @param hint
     * @param type (agent)
     */
    public void setHint(char hint, String type) {
        this.hint = hint;
        if (type.equals("P1") && hint == 't') {
            this.hint = '-';
        }
    }

}
