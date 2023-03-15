
import java.util.ArrayList;


public class A3main {

	public static void main(String[] args) {
		
		boolean verbose=false; //prints the formulas for SAT if true
		if (args.length>2 && args[2].equals("verbose") ){
			verbose=true; //prints the formulas for SAT if true
		}

		// read input from command line
		// Agent type
		System.out.println("-------------------------------------------\n");
		System.out.println("Agent " + args[0] + " plays " + args[1] + "\n");

		// World

		World world = World.valueOf(args[1]);

		char[][] p = world.map;
		printBoard(p);
		System.out.println("Start!");

		switch (args[0]) {
		case "P1":
			//TODO: Part 1
		case "P2":
			//TODO: Part 2
		case "P3":
			//TODO: Part 3
		case "P4":
			//TODO: Part 4
		case "P5":
			//TODO: Part 5

		}


		//templates to print results - copy to appropriate places
		//System.out.println("\nResult: Agent alive: all solved\n");
		//System.out.println("\nResult: Agent dead: found mine\n");
		//System.out.println("\nResult: Agent not terminated\n");

	}

	
	//prints the board in the required format - PLEASE DO NOT MODIFY
	public static void printBoard(char[][] board) {
		System.out.println();
		// first line
		for (int l = 0; l < board.length + 5; l++) {
			System.out.print(" ");// shift to start
		}
		for (int j = 0; j < board[0].length; j++) {
			System.out.print(j);// x indexes
			if (j < 10) {
				System.out.print(" ");
			}
		}
		System.out.println();
		// second line
		for (int l = 0; l < board.length + 3; l++) {
			System.out.print(" ");
		}
		for (int j = 0; j < board[0].length; j++) {
			System.out.print(" -");// separator
		}
		System.out.println();
		// the board
		for (int i = 0; i < board.length; i++) {
			for (int l = i; l < board.length - 1; l++) {
				System.out.print(" ");// fill with left-hand spaces
			}
			if (i < 10) {
				System.out.print(" ");
			}

			System.out.print(i + "/ ");// index+separator
			for (int j = 0; j < board[0].length; j++) {
				System.out.print(board[i][j] + " ");// value in the board
			}
			System.out.println();
		}
		System.out.println();
	}

}
