import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Patrick Stillhart on 25.12.2015.
 * <p>
 * This is the minesweeper adapter, it finds it and
 * translates the values between minesweeper and the solver
 * <p>
 * The images used are 1 pixel high and 16 pixel wide - they have a top offset of 3px
 */
public class Board {

    class BoardException extends Exception {

        public BoardException(String message) {
            super(message);
        }

    }

    static final int BLOCK_SIDE = 16;
    // The values are the RGB code of the pixel row on 3
    static final int[] BLOCK_CLOSED = {-11765043, -11833638, -10386462, -10649116, -10911771, -10649373, -10649372, -10649372, -10714909, -10781215, -10781215, -10847520, -11044645, -11242793, -11570975, -14076059};

    // The value is the RGB value of the first pixel on row 3
    static final int BLOCK_EMPTY = -9206408;
    static final int BLOCK_ONE = -8749954;
    static final int BLOCK_TWO = -8223611;
    static final int BLOCK_THREE = -7565424;
    static final int BLOCK_FOUR = -6578271;
    static final int BLOCK_FIVE = -6051925;
    static final int BLOCK_SIX = -11447725;
    static final int BLOCK_SEVEN = -10987430;
    static final int BLOCK_EIGHT = -10263194;
    static final int BLOCK_FLAG = -11636008;
    static final int BLOCK_MINE_EXPLODED = -12105399;

    private Robot robot;

    private Rectangle boardRect;
    private BufferedImage board;
    private Point clickMultiplier, initialMousePosition;

    private int countColumn, countRow, countMines;
    private State[][] field;

    public Board(int countMines) throws BoardException {

        try {
            robot = new Robot();
            // Determine where the game is
            // Union together the bounds of each screen for screenshot
            Rectangle screenRect = new Rectangle(0, 0, 0, 0);
            for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
                screenRect = screenRect.union(gd.getDefaultConfiguration().getBounds());
            }
            BufferedImage capture = robot.createScreenCapture(screenRect);
            Point start = findZero(capture);
            if (start == null) throw new BoardException("A Game? ... Computer says no");

            // Find the length and height of the game (in block count)
            countColumn = calculateCountColumn(capture, start) + 1;
            countRow = calculateCountRow(capture, start);
            if (countColumn == 0 || countRow == 0) throw new BoardException("What kind of sorcery is this?");

            // Mirror the game internally
            field = new State[countColumn][countRow];
            for (State[] row : field) Arrays.fill(row, State.BLOCK_CLOSED);

            // Set the amount of mines
            this.countMines = countMines;

            // Future screenshots only need to capture the game itself
            boardRect = new Rectangle(start.x, start.y, countColumn * BLOCK_SIDE, countRow * BLOCK_SIDE);
            board = robot.createScreenCapture(boardRect);

            // Since the original screenshot won't be here anymore we need a multiplier for the access via mouse click
            clickMultiplier = new Point(start.x + BLOCK_SIDE / 2, start.y + BLOCK_SIDE / 2);

            // Get the mouse position when the process starts, to reset it in the end
            initialMousePosition = new Point(MouseInfo.getPointerInfo().getLocation().x, MouseInfo.getPointerInfo().getLocation().y);

        } catch (AWTException e) {
            e.printStackTrace();
        }

    }

    /**
     * Restarts the game
     * @return true if successful
     */
    public boolean restart() {
        robot.mouseMove(clickMultiplier.x, clickMultiplier.y - BLOCK_SIDE / 2);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        robot.keyPress(KeyEvent.VK_F2);
        robot.keyRelease(KeyEvent.VK_F2);

        for (State[] row : field) Arrays.fill(row, State.BLOCK_CLOSED);

        return true;
    }

    /**
     * Mirrors the value from the real minesweeper in the internal array
     *
     * @return true if values have changed
     * @throws BoardException If the game ended
     */
    public boolean refresh() throws BoardException {

        State tmp;
        boolean change = false;

        board = robot.createScreenCapture(boardRect);

        for (int x = 0; x < countColumn; x++) {
            for (int y = 0; y < countRow; y++) {

                // we'll only check the ones who were closed in the last screenshot
                tmp = field[x][y];
                if (tmp == State.BLOCK_CLOSED) {
                    field[x][y] = read(x, y);
                    if (field[x][y] != tmp) change = true;
                } else if (tmp == State.BLOCK_MINE_EXPLODED) throw new BoardException("Well... there was a mine at (" + (x + 1) + "/" + (y + 1) + ")");
            }
        }

        return change;

    }

    /**
     * Open a field
     *
     * @param x why are you reading this?
     * @param y you seriously should understand it
     */
    public void open(int x, int y) {
        robot.mouseMove(clickMultiplier.x + x * BLOCK_SIDE, clickMultiplier.y + y * BLOCK_SIDE);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    /**
     * Opens all fields surrounding a field
     *
     * @param x why are you reading this?
     * @param y you seriously should understand it
     */
    public void openSurrounding(int x, int y) {
        robot.mouseMove(clickMultiplier.x + x * BLOCK_SIDE, clickMultiplier.y + y * BLOCK_SIDE);
        robot.mousePress(InputEvent.BUTTON2_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON2_DOWN_MASK);

    }

    /**
     * Flags a field
     *
     * @param x why are you reading this?
     * @param y you seriously should understand it
     */
    public void flag(int x, int y) {
        if (field[x][y] != State.BLOCK_CLOSED) return;
        field[x][y] = State.BLOCK_FLAG;

        robot.mouseMove(clickMultiplier.x + x * BLOCK_SIDE, clickMultiplier.y + y * BLOCK_SIDE);
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
    }

    /**
     * Flags all fields surrounding a field
     *
     * @param x why are you reading this?
     * @param y you seriously should understand it
     */
    public void flagSurrounding(int x, int y) {
        if (y > 0) {
            if (x > 0) flag(x - 1, y - 1);    // top ■□□
            flag(x, y - 1);    // top □■□
            if (x < field.length - 1) flag(x + 1, y - 1);  // top □□■
        }

        if (x > 0) flag(x - 1, y);  // middle ■□□
        if (x + 1 < field.length - 1) flag(x + 1, y); // middle □□■

        if (y + 1 < field[0].length - 1) {
            if (x > 0) flag(x - 1, y + 1);  // bottom ■□□
            flag(x, y + 1);    // bottom □■□
            if (x < field.length - 1) flag(x + 1, y + 1); // bottom □□■
        }

    }

    /**
     * Returns the cached field array
     *
     * @return the field array
     */
    public State[][] getField() {
        return field;
    }

    public int getCountColumn() {
        return countColumn;
    }

    public int getCountRow() {
        return countRow;
    }

    public int getCountMines() {
        return countMines;
    }

    /**
     * Gives the value from a field read from the screenshot back as state
     *
     * @param x why are you reading this?
     * @param y you seriously should understand it
     * @return the state
     */
    private State read(int x, int y) {
        switch (board.getRGB(x * BLOCK_SIDE, y * BLOCK_SIDE)) {
            case BLOCK_EMPTY:
                return State.BLOCK_EMPTY; // Put BLOCK_EMPTY out of order since it will happen most often
            case BLOCK_ONE:
                return State.BLOCK_ONE;
            case BLOCK_TWO:
                return State.BLOCK_TWO;
            case BLOCK_THREE:
                return State.BLOCK_THREE;
            case BLOCK_FOUR:
                return State.BLOCK_FOUR;
            case BLOCK_FIVE:
                return State.BLOCK_FIVE;
            case BLOCK_SIX:
                return State.BLOCK_SIX;
            case BLOCK_SEVEN:
                return State.BLOCK_SEVEN;
            case BLOCK_EIGHT:
                return State.BLOCK_EIGHT;
            case BLOCK_FLAG:
                return State.BLOCK_FLAG;
            case BLOCK_MINE_EXPLODED:
                return State.BLOCK_MINE_EXPLODED;
        }

        return State.BLOCK_CLOSED;
    }

    public void end() {
        robot.mouseMove(initialMousePosition.x, initialMousePosition.y);
    }

    /**
     * Finds the starting point of the minesweeper game board on the screenshot
     *
     * @param capture the screenshot
     * @return ZeroPoint
     */
    private Point findZero(BufferedImage capture) {
        for (int x = 0; x < capture.getWidth() - BLOCK_SIDE; x++) {
            heightLoop:
            for (int y = 0; y < capture.getHeight() - BLOCK_SIDE; y++) {
                if (capture.getRGB(x, y) == BLOCK_CLOSED[0]) {
                    for (int i = 1; i < BLOCK_SIDE; i++) {
                        if (capture.getRGB(x + i, y) != BLOCK_CLOSED[i]) continue heightLoop;
                    }
                    return new Point(x, y);
                }
            }
        }
        return null;
    }

    /**
     * Calculates how many columns the game has
     *
     * @param capture the screenshot
     * @param start   the starting point for looking
     * @return the amount of columns
     */
    private int calculateCountColumn(BufferedImage capture, Point start) {
        int blocks = 0;
        for (int i = start.x; i < capture.getWidth() - BLOCK_SIDE; i += BLOCK_SIDE) {
            for (int j = 1; j < BLOCK_SIDE; j++) {
                if (capture.getRGB(i + j, start.y) != BLOCK_CLOSED[j]) return blocks;
            }
            blocks++;
        }
        return blocks;
    }

    /**
     * Calculates how many rows the game has
     *
     * @param capture the screenshot
     * @param start   the starting point for looking
     * @return the amount of rows
     */
    private int calculateCountRow(BufferedImage capture, Point start) {
        int blocks = 0;
        for (int i = start.y; i < capture.getHeight() - BLOCK_SIDE; i += BLOCK_SIDE) {
            for (int j = 1; j < BLOCK_SIDE; j++) {
                if (capture.getRGB(start.x + j, i) != BLOCK_CLOSED[j]) return blocks;
            }
            blocks++;
        }
        return blocks;
    }

    /*
    Debugging Methods
     */

    /**
     * Prints the RGB Code used for identification for a field as java code
     */
    static void printRGBCodeForImage(String name) {
        try {
            BufferedImage closedSquare = ImageIO.read(new File("resources/" + name + ".png"));
            StringBuilder sb = new StringBuilder("static final int[] BLOCK_");
            sb.append(name.toUpperCase());
            sb.append(" = {");
            for (int i = 0; i < BLOCK_SIDE; i++) {
                sb.append(closedSquare.getRGB(i, 0));
                sb.append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("};");
            System.out.println(sb);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
