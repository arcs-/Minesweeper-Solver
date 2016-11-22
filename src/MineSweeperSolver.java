/*
The MIT License (MIT)

Copyright (c) 2015 Patrick Stillhart

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Minesweeper Solver
 * Created by Patrick Stillhart on 25.12.2015.
 */
public class MineSweeperSolver {

    public static void main(String[] args) {
        new MineSweeperSolver();
    }

    private Board board;
    private State[][] field;

    public MineSweeperSolver() {

        try {

            // Get the number of mines from the user
            int countMines = getUserInput("Enter the amount of mines");

            // Initialize the board
            board = new Board(countMines);
            field = board.getField();

            do {

                // Just open some fields
                board.open(0, 0);
                board.open(0, board.getCountRow() - 1);
                board.open(board.getCountColumn() - 1, 0);
                board.open(board.getCountColumn() - 1, board.getCountRow() - 1);

                // Solve the game
                solver();

                // move mouse back
                board.end();

            } while (getUserInput("\nType 1 for another game") == 1 && board.restart());

        } catch (Board.BoardException e) {
            System.err.println(e.getMessage());
        }

    }

    /**
     * Asks the user to enter a number via keyboard
     *
     * @param msg the message that should be shown
     * @return the number the user entered
     */
    private int getUserInput(String msg) {
        boolean ok = false;
        int input = -1;

        System.out.println(msg);

        while (!ok) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                input = Integer.parseInt(br.readLine());
                ok = true;
            } catch (IOException | NumberFormatException e) {
                System.err.println("Not a good number, try again!");
            }
        }

        return input;
    }

    /**
     * Main Task
     * Solves a game
     */
    private void solver() {

        try {

            int reruns = 0;
            boolean triedTank = false;

            for (int times = 1; times <= 300; times++) {

                // Code for running at the end twice, just to be sure we really can't do anything more
                if (board.refresh()) {
                    reruns = 0;
                    triedTank = false; // something changed -> everything okay
                } else { // there was no change

                    Thread.sleep(200);
                    board.refresh();

                    if (reruns < 3) reruns++;
                    else if (checkSolved()) throw board.new BoardException("\nWOW... I rate this game a solid 5 / 7");
                    else {

                        if (!triedTank) {
                            tankSolver();
                            triedTank = true;
                        } else throw board.new BoardException("\nThe computer didn't do anything no more");

                    }
                }

                System.out.println("\nRound: " + times);

                for (int x = 0; x < board.getCountColumn(); x++) {
                    for (int y = 0; y < board.getCountRow(); y++) {

                        // Only solve fields with "numbers"
                        if (field[x][y].getVal() > 0) solveSingle(x, y);

                    }
                }

            }

        } catch (Board.BoardException | InterruptedException e) {
            System.err.println(e.getMessage());
        }

    }

    /**
     * Solves a single field the easy way ..
     * if the number of blocks around equals the number on this block, flag them otherwise open them
     *
     * @param x why are you reading this?
     * @param y you seriously should understand it
     */
    private void solveSingle(int x, int y) {
        int countClosed = getSurroundingByType(x, y, State.BLOCK_CLOSED);
        if (countClosed == 0) return;

        int countAlreadyFlagged = getSurroundingByType(x, y, State.BLOCK_FLAG);
        int countMinesAround = field[x][y].getVal();

        // First: flag as much as we can
        if (countMinesAround == countClosed + countAlreadyFlagged) {
            System.out.println("  Flag: " + field[x][y].getVal() + " at (" + (x + 1) + "/" + (y + 1) + ")");
            board.flagSurrounding(x, y);
            countAlreadyFlagged = getSurroundingByType(x, y, State.BLOCK_FLAG);
        }

        // Second: open the ones around
        if (countMinesAround == countAlreadyFlagged) {
            System.out.println("  Open: " + field[x][y].getVal() + " at (" + (x + 1) + "/" + (y + 1) + ")");
            board.openSurrounding(x, y);
        }

    }

    /**
     * Discovers all the fields around that match the parameter
     *
     * @param x why are you reading this?
     * @param y you seriously should understand it
     * @param type type to compare
     * @return the amount of fields around that match type
     */
    private int getSurroundingByType(int x, int y, State type) {
        int hits = 0;

        if (y > 0) {
            if (x > 0 && field[x - 1][y - 1] == type) hits++; // top ■□□
            if (field[x][y - 1] == type) hits++;   // top □■□
            if (x < board.getCountColumn() - 1 && field[x + 1][y - 1] == type) hits++; // top □□■
        }

        if (x > 0 && field[x - 1][y] == type) hits++; // middle ■□□
        if (x < board.getCountColumn() - 1 && field[x + 1][y] == type) hits++; // middle □□■

        if (y < board.getCountRow() - 1) {
            if (x > 0 && field[x - 1][y + 1] == type) hits++; // bottom ■□□
            if (field[x][y + 1] == type) hits++;   // bottom □■□
            if (x < board.getCountColumn() - 1 && field[x + 1][y + 1] == type) hits++; // bottom □□■
        }

        return hits;

    }

    /**
     * Discovers all boundary blocks around
     * A boundary block is an unopened block with opened blocks next to it.
     *
     * @param x why are you reading this?
     * @param y you seriously should understand it
     * @return true if it is a boundary block
     */
    private boolean isBoundary(int x, int y) {
        if (field[x][y] != State.BLOCK_CLOSED) return false;

        if (y > 0) {
            if (x > 0 && field[x - 1][y - 1].getVal() >= 0) return true; // top ■□□
            if (field[x][y - 1].getVal() >= 0) return true;   // top □■□
            if (x < board.getCountColumn() - 1 && field[x + 1][y - 1].getVal() >= 0) return true; // top □□■
        }

        if (x > 0 && field[x - 1][y].getVal() >= 0) return true; // middle ■□□
        if (x < board.getCountColumn() - 1 && field[x + 1][y].getVal() >= 0) return true; // middle □□■

        if (y < board.getCountRow() - 1) {
            if (x > 0 && field[x - 1][y + 1].getVal() >= 0) return true; // bottom ■□□
            if (field[x][y + 1].getVal() >= 0) return true;   // bottom □■□
            if (x < board.getCountColumn() - 1 && field[x + 1][y + 1].getVal() >= 0) return true; // bottom □□■
        }

        return false;
    }

    /**
     * Checks if we already won the game
     *
     * @return true if we won
     */
    private boolean checkSolved() {
        for (int x = 0; x < board.getCountColumn(); x++) {
            for (int y = 0; y < board.getCountRow(); y++) {

                if (field[x][y] == State.BLOCK_CLOSED) return false;

            }
        }

        return true;
    }

    /**
     * How many flags exist around this block?
     *
     * @param array the array to check in
     * @param x why are you reading this?
     * @param y you seriously should understand it
     * @return amount of flags around
     */
    private int countFlagsAround(boolean[][] array, int x, int y) {
        int mines = 0;

        if (y > 0) {
            if (x > 0 && array[x - 1][y - 1]) mines++; // top ■□□
            if (array[x][y - 1]) mines++;   // top □■□
            if (x < array.length - 1 && array[x + 1][y - 1]) mines++; // top □□■
        }

        if (x > 0 && array[x - 1][y]) mines++; // middle ■□□
        if (x < array.length - 1 && array[x + 1][y]) mines++; // middle □□■

        if (y < array[0].length - 1) {
            if (x > 0 && array[x - 1][y + 1]) mines++; // bottom ■□□
            if (array[x][y + 1]) mines++;   // bottom □■□
            if (x < array.length - 1 && array[x + 1][y + 1]) mines++; // bottom □□■
        }

        return mines;
    }

    /**
     * Tank solver
     * By LuckyToilet: https://luckytoilet.wordpress.com/2012/12/23/2125/
     *
     * TANK solver: slow and heavyweight backtrack solver designed to
     * solve any conceivable position!
     */
    private void tankSolver() {

        // Timing
        long tankTime = System.currentTimeMillis();

        ArrayList<Point> borderBlocks = new ArrayList<>();
        ArrayList<Point> allEmptyBlocks = new ArrayList<>();

        // Endgame case: if there are few enough tiles, don't bother with border tiles.
        borderOptimization = false;
        for (int x = 0; x < board.getCountColumn(); x++)
            for (int y = 0; y < board.getCountRow(); y++)
                if (field[x][y] == State.BLOCK_CLOSED && field[x][y] != State.BLOCK_FLAG) allEmptyBlocks.add(new Point(x, y));

        // Determine all border tiles
        for (int x = 0; x < board.getCountColumn(); x++)
            for (int y = 0; y < board.getCountRow(); y++)
                if (isBoundary(x, y) && field[x][y] != State.BLOCK_FLAG) borderBlocks.add(new Point(x, y));

        // Count how many blocks outside the knowable range
        int countBlocksOutOfRange = allEmptyBlocks.size() - borderBlocks.size();
        if (countBlocksOutOfRange > 8) { // 8 = brute force limit
            borderOptimization = true;
        } else borderBlocks = allEmptyBlocks;


        // Something went wrong
        if (borderBlocks.size() == 0) return;


        // Run the segregation routine before recursing one by one
        // Don't bother if it's endgame as doing so might make it miss some cases
        ArrayList<ArrayList<Point>> segregated;
        if (!borderOptimization) {
            segregated = new ArrayList<>();
            segregated.add(borderBlocks);
        } else segregated = tankSegregate(borderBlocks);

        boolean success = false;
        double propBest = 0; // Store information about the best probability
        int totalMultiCases = 1,
                propBestBlock = -1,
                probBestS = -1;
        for (int currentBlockId = 0; currentBlockId < segregated.size(); currentBlockId++) {

            // Copy everything into temporary constructs
            tankSolutions = new ArrayList<>();
            tankBoard = field.clone();

            knownMine = new boolean[board.getCountColumn()][board.getCountRow()];
            for (int x = 0; x < board.getCountColumn(); x++) {
                for (int y = 0; y < board.getCountRow(); y++) {
                    knownMine[x][y] = field[x][y] == State.BLOCK_FLAG;
                }
            }

            knownEmpty = new boolean[board.getCountColumn()][board.getCountRow()];
            for (int x = 0; x < board.getCountColumn(); x++) {
                for (int y = 0; y < board.getCountRow(); y++) {
                    knownEmpty[x][y] = tankBoard[x][y].getVal() >= 0;
                }
            }

            // Compute solutions -- here's the time consuming step
            tankRecurse(segregated.get(currentBlockId), 0);

            // Something screwed up
            if (tankSolutions.size() == 0) return;


            // Check for solved squares
            for (int i = 0; i < segregated.get(currentBlockId).size(); i++) {
                boolean allMine = true,
                        allEmpty = true;
                for (boolean[] sln : tankSolutions) {
                    if (!sln[i]) allMine = false;
                    if (sln[i]) allEmpty = false;
                }

                Point block = segregated.get(currentBlockId).get(i);

                if (allMine) board.flag(block.x, block.y);
                else if (allEmpty) {
                    success = true;
                    board.open(block.x, block.y);
                }
            }

            totalMultiCases *= tankSolutions.size();

            // Calculate probabilities, in case we need it
            if (success) continue;
            int maxEmpty = -10000;
            int iEmpty = -1;
            for (int i = 0; i < segregated.get(currentBlockId).size(); i++) {
                int nEmpty = 0;
                for (boolean[] sln : tankSolutions) {
                    if (!sln[i]) nEmpty++;
                }
                if (nEmpty > maxEmpty) {
                    maxEmpty = nEmpty;
                    iEmpty = i;
                }
            }

            double probability = (double) maxEmpty / (double) tankSolutions.size();

            if (probability > propBest) {
                propBest = probability;
                propBestBlock = iEmpty;
                probBestS = currentBlockId;
            }

        }

        tankTime = System.currentTimeMillis() - tankTime;
        if (success) {
            System.out.printf("  Tank successfully invoked (%dms, %d cases)\n", tankTime, totalMultiCases);
            return;
        }

        // Take the guess, since we can't deduce anything useful
        System.out.printf("  Tank guessing with probability %1.2f (%dms, %d cases)\n", propBest, tankTime, totalMultiCases);
        Point q = segregated.get(probBestS).get(propBestBlock);
        board.open(q.x, q.y);

    }

    /**
     * Segregation routine: if two regions are independent then consider them as separate regions
     *
     * @param borderBlocks the blocks to check
     * @return the separated regions
     */
    private ArrayList<ArrayList<Point>> tankSegregate(ArrayList<Point> borderBlocks) {

        ArrayList<ArrayList<Point>> allRegions = new ArrayList<>();
        ArrayList<Point> covered = new ArrayList<>();

        while (true) {

            LinkedList<Point> queue = new LinkedList<>();
            ArrayList<Point> finishedRegion = new ArrayList<>();

            // Find a suitable starting point
            for (Point firstB : borderBlocks) {
                if (!covered.contains(firstB)) {
                    queue.add(firstB);
                    break;
                }
            }

            if (queue.isEmpty()) break;

            while (!queue.isEmpty()) {

                Point block = queue.poll();
                finishedRegion.add(block);
                covered.add(block);

                // Find all connecting blocks
                for (Point compareBlock : borderBlocks) {

                    boolean isConnected = false;

                    if (finishedRegion.contains(compareBlock)) continue;

                    if (Math.abs(block.x - compareBlock.x) > 2 || Math.abs(block.y - compareBlock.y) > 2) isConnected = false;
                    else {
                        // Perform a search on all the blocks
                        blockSearch: for (int x = 0; x < board.getCountColumn(); x++) {
                            for (int y = 0; y < board.getCountRow(); y++) {
                                if (field[x][y].getVal() > 0) {
                                    if (Math.abs(block.x - x) <= 1 && Math.abs(block.y - y) <= 1 && Math.abs(compareBlock.x - x) <= 1 && Math.abs(compareBlock.y - y) <= 1) {
                                        isConnected = true;
                                        break blockSearch;
                                    }
                                }
                            }
                        }
                    }

                    if (!isConnected) continue;
                    if (!queue.contains(compareBlock)) queue.add(compareBlock);

                }
            }

            allRegions.add(finishedRegion);

        }

        return allRegions;

    }


    private State[][] tankBoard = null;
    private boolean[][] knownMine = null;
    private boolean[][] knownEmpty = null;
    private ArrayList<boolean[]> tankSolutions;

    // Should be true -- if false, we're brute forcing the endgame
    boolean borderOptimization;

    /**
     * Recurse from depth (0 is root)
     * Assumes the tank variables are already set; puts solutions in the arraylist.
     * @param borderTiles the region to analyze
     * @param depth which depth lvl we're in
     */
    void tankRecurse(ArrayList<Point> borderTiles, int depth) {

        // Return if at this point, it's already inconsistent
        int flagCount = 0;
        for (int x = 0; x < board.getCountColumn(); x++)
            for (int y = 0; y < board.getCountRow(); y++) {

                // Count flags for endgame cases
                if (knownMine[x][y]) flagCount++;

                int currentBlockValue = tankBoard[x][y].getVal();
                if (currentBlockValue < 0) continue;

                // Scenario 1: too many mines
                if (countFlagsAround(knownMine, x, y) > currentBlockValue) return;

                // Total bordering blocks
                int countBorderingBlocks;
                if ((x == 0 && y == 0) || (x == board.getCountColumn() - 1 && y == board.getCountRow() - 1)) countBorderingBlocks = 3;
                else if (x == 0 || y == 0 || x == board.getCountColumn() - 1 || y == board.getCountRow() - 1) countBorderingBlocks = 5;
                else countBorderingBlocks = 8;

                // Scenario 2: too many empty
                if (countBorderingBlocks - countFlagsAround(knownEmpty, x, y) < currentBlockValue) return;
            }

        // We have too many flags
        if (flagCount > board.getCountMines()) return;


        // Solution found!
        if (depth == borderTiles.size()) {

            // We don't have the exact mine count, so no
            if (!borderOptimization && flagCount < board.getCountMines()) return;

            boolean[] solution = new boolean[borderTiles.size()];
            for (int i = 0; i < borderTiles.size(); i++) {
                Point block = borderTiles.get(i);
                solution[i] = knownMine[block.x][block.y];
            }
            tankSolutions.add(solution);
            return;
        }

        Point block = borderTiles.get(depth);

        // Recurse two positions: mine and no mine
        knownMine[block.x][block.y] = true;
        tankRecurse(borderTiles, depth + 1);
        knownMine[block.x][block.y] = false;

        knownEmpty[block.x][block.y] = true;
        tankRecurse(borderTiles, depth + 1);
        knownEmpty[block.x][block.y] = false;

    }

    /*
    Debugging Methods
     */

    /**
     * Prints out the board in console
     */
    private void printBoard() {

        for (int x = 0; x < board.getCountRow(); x++) {
            for (int y = 0; y < board.getCountColumn(); y++) {

                switch (field[y][x]) {
                    case BLOCK_EMPTY:System.out.print("□");break;
                    case BLOCK_CLOSED:System.out.print("■");break;
                    case BLOCK_ONE:System.out.print("1");break;
                    case BLOCK_TWO:System.out.print("2");break;
                    case BLOCK_THREE:System.out.print("3");break;
                    case BLOCK_FOUR:System.out.print("4");break;
                    case BLOCK_FIVE:System.out.print("5");break;
                    case BLOCK_SIX:System.out.print("6");break;
                    case BLOCK_SEVEN:System.out.print("7");break;
                    case BLOCK_EIGHT:System.out.print("8");break;
                    case BLOCK_FLAG:System.out.print("P");break;
                    case BLOCK_MINE_EXPLODED:System.out.print("X");break;
                }

            }
            System.out.println();
        }
    }


}
