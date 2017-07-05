public class Edit {

    enum EditOp {
        INSERT, DELETE
    }

    public EditOp op;
    private int cursorRow, cursorCol;
    private char c;

    Edit(EditOp op, char c, int cursorRow, int cursorCol) {
        this.op = op;
        this.c = c;
        this.cursorCol = cursorCol;
        this.cursorRow = cursorRow;
    }

    public int getCursorRow(){
        return cursorRow;
    }

    public int getCursorCol() {
        return cursorCol;
    }

    public char getChar() {
        return c;
    }

}
