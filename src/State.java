/**
 * Created by Patrick Stillhart on 26.12.2015.
 * <p>
 * The different states a block can have,
 * basically what you see.. like the numbers, flags, etc.
 */
public enum State {

    BLOCK_MINE_EXPLODED(-3),
    BLOCK_CLOSED(-2),
    BLOCK_FLAG(-1),
    BLOCK_EMPTY(0),
    BLOCK_ONE(1),
    BLOCK_TWO(2),
    BLOCK_THREE(3),
    BLOCK_FOUR(4),
    BLOCK_FIVE(5),
    BLOCK_SIX(6),
    BLOCK_SEVEN(7),
    BLOCK_EIGHT(8);

    private int val;

    State(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }

}
