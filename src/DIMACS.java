import org.logicng.formulas.Formula;
import org.logicng.formulas.Literal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;

public class DIMACS {

    private HashMap<String, Integer> literals;

    /**
     * Build the format of DIMACS
     *
     * @param formula a logic formula
     * @return CNF clauses which is DIMACS format
     */
    public int[][] buildDIMACS(Formula formula) {
        literals = new HashMap<>();
        ArrayList<int[]> clauses = new ArrayList<>();
        // Convert the formula into CNF
        Formula cnf = formula.cnf();
        encodeLiterals(cnf.literals());
        Iterator<Formula> iterator = cnf.iterator();
        while (iterator.hasNext()) {
            clauses.add(getClause(iterator.next()));
        }
        int[][] clausesArray = new int[clauses.size()][];
        for (int i = 0; i < clauses.size(); i++) {
            int[] singleClause = clauses.get(i);
            clausesArray[i] = singleClause;
        }
        return clausesArray;
    }

    /**
     * Get a clause of the formula
     *
     * @param clause
     * @return literals which is formated in DIMACS format
     */
    public int[] getClause(Formula clause) {
        int index = 0;
        int[] literals = new int[clause.variables().size()];
        Iterator<Formula> iterator = clause.iterator();
        if (clause.isAtomicFormula()) {
            literals[index] = this.literals.get(clause.toString());
        } else {
            while (iterator.hasNext()) {
                literals[index] = this.literals.get(iterator.next().toString());
                index++;
            }
        }
        return literals;
    }

    /**
     * Encode Literals
     *
     * @param literals
     */
    public void encodeLiterals(SortedSet<Literal> literals) {
        Iterator<Literal> it = literals.iterator();
        while (it.hasNext()) {
            encodeLiteral(it.next().toString());
        }
    }

    /**
     * Encode the literal into an integer value, for the SAT solver.
     *
     * @param literal which is encoded
     */
    public void encodeLiteral(String literal) {
        // Check negative
        if (literal.startsWith("~")) {
            // Check unencoded
            if (literals.get(literal) == null) {
                String positiveLiteral = literal.replace("~", "");
                if (literals.get(positiveLiteral) != null) {
                    literals.put(literal, literals.get(positiveLiteral) * (-1));
                } else {
                    literals.put(positiveLiteral, literals.size() + 1);
                    literals.put(literal, literals.get(positiveLiteral) * (-1));
                }
            } else {
                String positiveLiteral = literal.replace("~", "");
                if (literals.get(positiveLiteral) == null) {
                    literals.put(literal, literals.get(literal) * (-1));
                }
            }
        } else {
            if (literals.get(literal) == null) {
                String negativeLiteral = "~" + literal;
                if (literals.get(literal) != null) {
                    literals.put(literal, literals.get(negativeLiteral) * (-1));
                } else {
                    literals.put(literal, literals.size() + 1);
                    literals.put(negativeLiteral, literals.get(literal) * (-1));
                }
            } else {
                String negativeLiteral = "~" + literal;
                if (literals.get(negativeLiteral) == null) {
                    literals.put(negativeLiteral, literals.get(literal) * (-1));
                }
            }
        }
    }


    /**
     * Getter
     *
     * @return the literals
     */
    public HashMap<String, Integer> getLiterals() {
        return literals;
    }
}
