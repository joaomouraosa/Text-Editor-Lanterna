import com.googlecode.lanterna.TerminalFacade;
import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.terminal.Terminal;
import javafx.geometry.Pos;

import java.io.IOException;
import java.util.ArrayList;


public class BufferView
{
    private Terminal term;
    private int width;
    private int height;
    private Position origin=new Position(0,0);
    private Position cursorPos = new Position(0,0);
    private int startRow=0,startCol=0;
    private ArrayList<Integer> modifiedLines;
    private FileBuffer buffer;

    public BufferView(FileBuffer b) throws IOException{

        buffer = b;
        term = TerminalFacade.createTerminal();
        term.enterPrivateMode();
        height = term.getTerminalSize().getRows();
        width = term.getTerminalSize().getColumns();

        modifiedLines = new ArrayList<Integer>();
        startBufferView();
    }

    public BufferView() throws IOException
    {
        term = TerminalFacade.createTerminal();
        term.enterPrivateMode();
        height = term.getTerminalSize().getRows();
        width  = term.getTerminalSize().getColumns();
        buffer = new FileBuffer();
        modifiedLines = new ArrayList<Integer>();
        startBufferView();
    }

    private void startBufferView() throws IOException {

        term.clearScreen();
        term.applySGR(Terminal.SGR.ENTER_BOLD);
        term.applyForegroundColor(Terminal.Color.WHITE);
        term.flush();

        showBuffer();
        Position logical = new Position(buffer.getCursorRow(), buffer.getCursorCol());
        Position visualCursor = viewPos(logical);
        cursorPos.row = visualCursor.row;
        cursorPos.col = visualCursor.col;
        term.moveCursor(cursorPos.col, cursorPos.row);

        while (true)
        {
            Key k= term.readInput();
            if (k!=null) {

                if (k.getKind() == Key.Kind.Escape) {
                    term.exitPrivateMode();
                    return;
                }

                editBuffer(term,k);
            }
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException ie)
            {
                ie.printStackTrace();
            }
        }
    }

    private Position viewPos(Position logical) {
        Position visualPos = new Position(0, 0);

        for (int i = startRow; i < logical.row; i++) {
            if (buffer.getLine(i).length() != 0) { //linha nao vazia
                if (i == startRow) {
                    //se a primeira linha nao for vazia
                    // apenas considerar o espaço que os carateres ocupam a partir do startCol
                    visualPos.row += (int) Math.ceil((double) (buffer.getLine(i).length() - startCol) / (double) width);
                } else {
                    visualPos.row += (int) Math.ceil((double) buffer.getLine(i).length() / (double) width);
                }
            } else {
                visualPos.row += 1;
            }
        }

        if (logical.row == startRow && startCol > 0) {
            visualPos.row += ((logical.col - startCol) / width);
        } else {
            visualPos.row += logical.col / width;
        }

        if (visualPos.row >= height) {
            return null;
        }

        visualPos.col = logical.col % width;

        visualPos.row += origin.row;
        visualPos.col += origin.col;
        return visualPos;
    }

    private void showBuffer() {
        Position visualPos;
        Position logical;

        int index;

        for (int i = startRow; i < buffer.getNumberOfLines(); i++) {
            String str = buffer.getLine(i);
            if (i == startRow) {
                index = startCol;
            } else index = 0;
            for (int j = index; j < str.length(); j++) {
                logical = new Position(i, j);
                visualPos = viewPos(logical);
                if (visualPos == null) {
                    buffer.setCursor(logical.row, logical.col - 1);
                    return;
                }
                buffer.setCursor(logical.row, logical.col + 1); //atualizar cursor logico
                term.moveCursor(visualPos.col, visualPos.row);
                term.putCharacter(str.charAt(j));
            }
        }
    }

    private void showBufferLine(int line) {

        String str = buffer.getLine(line);
        Position logical = new Position(line,0);
        Position visualPos = viewPos(logical);

        int col = visualPos.col;
        int row = visualPos.row;

        term.moveCursor(col,row);

        for (int i=0;i<=str.length();i++) {
            if (i%width==0)
                term.moveCursor(0, visualPos.row++);
            term.putCharacter(' ');
        }

        term.moveCursor(col,row);
        for (int i=0;i<str.length();i++) {
            if (i%width==0)
            term.putCharacter(str.charAt(i));
        }
    }

    private void moveUp() {
        if ( !(buffer.getCursorRow()==0 && cursorPos.row==0 && buffer.getCursorCol()>=0 && buffer.getCursorCol()<width)) {
            if (buffer.getCursorCol() >= width) { //linha de cima pertence a mesma linha logica
                buffer.setCursor(buffer.getCursorRow(), buffer.getCursorCol() - width);
            } else {
                float upLine_complete_size = buffer.getLine(buffer.getCursorRow() - 1).length();
                float n = upLine_complete_size / width; //linhas visuais totalmente preenchidas
                float fractional_part = n - (int) n;
                float upLineSize = (width * fractional_part); //carateres na ultima linha visual
                if (upLineSize >= buffer.getCursorCol()) { //a linha tem pelo menos o mesmo tamanho que a atual
                    int logical_col = (int) ((upLine_complete_size - upLineSize) + buffer.getCursorCol());
                    buffer.setCursor(buffer.getCursorRow() - 1, logical_col);
                } else {
                    buffer.setCursor(buffer.getCursorRow() - 1, (int) upLine_complete_size);
                }
            }
        }

        if ( (startRow>0 || (startRow==0 && startCol>0)) && cursorPos.row==0) {
            if(buffer.getCursorRow() == startRow){
                startCol-=width;
            }
            else {
                float upLine_size=buffer.getLine(startRow-1).length();
                float n=upLine_size/width; //linhas visuais que a linha logica acima da startRow ocupa
                float fractional_part = n - (int) n;
                float last_visual_line_size = fractional_part * width; //carateres na ultima linha visual
                float coluna_inicio_ultima_linha = buffer.getLine(startRow-1).length() - last_visual_line_size;

                startRow--;
                startCol = (int) coluna_inicio_ultima_linha;
            }

            term.clearScreen();
            Position backup = new Position(buffer.getCursorRow(), buffer.getCursorCol());
            showBuffer();
            buffer.setCursor(backup.row, backup.col);
        }
    }

    private void moveDown() {
        int row = buffer.getCursorRow();
        int col = buffer.getCursorCol();
        int lineLength = buffer.getLine(row).length();
        Position logical1 = new Position(buffer.getCursorRow(),buffer.getCursorCol());
        Position visual1  = viewPos(logical1);

        if (visual1.row==height-1 ){
            startRow++;
        }
        if ((col+width) < lineLength) {
            buffer.setCursor(row,col+width);
        }
        else {

            Position logical2 = new Position(buffer.getCursorRow(),lineLength);
            Position visual2  = viewPos(logical2);


            if ((lineLength-col < width) && (visual1.row < visual2.row)) { //vai para o final da linha logica
                col = lineLength;
                buffer.setCursor(row, col);
            }
            else if (buffer.getLineList().size()-1>buffer.getCursorRow()){ //muda de linha logica
                while (col >= width) {
                    col -= width;
                }
                if (buffer.getLine(row+1).length()<col)
                    col = buffer.getLine(row+1).length();
                buffer.setCursor(row + 1, col);
            }
        }
        if (visual1.row==height-1 ){
            int tempRow = buffer.getCursorRow();
            int tempCol = buffer.getCursorCol();
            term.clearScreen();
            showBuffer();
            buffer.setCursor(tempRow,tempCol);
        }

    }

    private void moveRight() {
        Position initialPos=new Position(buffer.getCursorRow(),buffer.getCursorCol());
        int tamanho_linha_logica=buffer.getLine(buffer.getCursorRow()).length();
        buffer.moveRight();

        if (startRow!=buffer.getNumberOfLines()-1 && cursorPos.row==height-1 && (initialPos.col==tamanho_linha_logica || cursorPos.col==width-1)) {
            double startRow_size = buffer.getLine(startRow).length();
            double n = startRow_size / width; //linhas visuais que a linha logica startRow ocupa

            if (n > 1) { //linha logica startRow ocupa mais do que uma linha visual
                double fractional_part = n - (int) n;
                double last_visual_line_size = fractional_part * width; //carateres na ultima linha visual
                double coluna_inicio_ultima_linha = buffer.getLine(startRow).length() - last_visual_line_size;

                if (startCol >= coluna_inicio_ultima_linha) { //startCol corresponde a ultima linha visual
                    //inicio da proxima linha logica
                    startRow++;
                    startCol = 0;
                } else {
                    startCol += width; //inicio da proxima linha visual
                }
            } else { //linha logica startRow apenas ocupa uma linha visual
                startRow++;
                startCol = 0;
            }

            term.clearScreen();
            Position backup = new Position(buffer.getCursorRow(), buffer.getCursorCol());
            showBuffer();
            buffer.setCursor(backup.row, backup.col);
        }
    }

    private void moveLeft() {
        buffer.moveLeft();

        if ( (startRow>0 || (startRow==0 && startCol>0)) && cursorPos.row==0 && cursorPos.col==0) {
            if( startRow==buffer.getCursorRow() ) {
                startCol-= width;
            }
            else {
                double upLine_size=buffer.getLine(startRow-1).length();
                double n=upLine_size/width; //linhas visuais que a linha logica acima da startRow ocupa
                double fractional_part = n - (int) n;
                double last_visual_line_size = fractional_part * width; //carateres na ultima linha visual
                double coluna_inicio_ultima_linha = buffer.getLine(startRow-1).length() - last_visual_line_size;

                startRow--;
                startCol = (int) coluna_inicio_ultima_linha;
            }
            term.clearScreen();
            Position backup=new Position(buffer.getCursorRow(),buffer.getCursorCol());
            showBuffer();
            buffer.setCursor(backup.row,backup.col);
        }
    }

    private void editBuffer(Terminal term, Key k)  throws IOException{

        if (k.getKind()==Key.Kind.ArrowLeft) {
          moveLeft();
        }
        else if (k.getKind()==Key.Kind.ArrowRight) {
            moveRight();
        }
        else if (k.getKind()==Key.Kind.ArrowUp) {
            moveUp();
        }
        else if (k.getKind()==Key.Kind.ArrowDown) {
            moveDown();
        }

        else if (k.getKind()==Key.Kind.Enter) {
            buffer.newLine();
            term.clearScreen();
            int row = buffer.getCursorRow();
            int col = buffer.getCursorCol();
            Position pos = new Position(row,col);
            Position visual = viewPos(pos);
            if (visual.row==height-1)
                startRow++;
            showBuffer();
            buffer.setCursor(row,col);
        }

        else if (k.getKind()==Key.Kind.Backspace) { //backspace

            buffer.delete();
            int row = buffer.getCursorRow();
            int col = buffer.getCursorCol();
            term.clearScreen();
            showBuffer();
            buffer.setCursor(row,col);
        }
        else if (k.getKind()==Key.Kind.Escape) { //backspace
            term.exitPrivateMode();
        }

        else if (k.isCtrlPressed() && k.getCharacter()=='s')  { //save
            buffer.save();
        }

        else if (k.isCtrlPressed() && k.getCharacter()=='z') { //undo
            if (buffer.getUndoSize() > 0) {
                buffer.undo();
                term.clearScreen();
                showBuffer();
                buffer.setCursor(buffer.getUndoRow(), buffer.getUndoCol());
            }
        }
        else if(k.isCtrlPressed() && k.getCharacter()=='r'){ //marcar inicio texto
            buffer.setMark(buffer.getCursorRow(),buffer.getCursorCol());
        }
        else if(k.isCtrlPressed() && k.getCharacter()=='c'){ //copy
            buffer.copy();
        }
        else if(k.isCtrlPressed() && k.getCharacter()=='x') { //cut
            Position previousPos=new Position(buffer.getCursorRow(),buffer.getCursorCol());
            buffer.cut();
            term.clearScreen();
            showBuffer();
            buffer.setCursor(previousPos.row,previousPos.col);
        }
        else if(k.isCtrlPressed() && k.getCharacter()=='v'){ //paste
            Position previousPos=new Position(buffer.getCursorRow(),buffer.getCursorCol());
            buffer.paste();
            term.clearScreen();
            showBuffer();
            buffer.setCursor(previousPos.row,previousPos.col);
        }
        else {
            if (!modifiedLines.contains(buffer.getCursorRow()))
                modifiedLines.add(buffer.getCursorRow());
            buffer.insert(k.getCharacter());
            int row = buffer.getCursorRow();
            if (row==startRow) {
                int col = buffer.getCursorCol();
                term.clearScreen();
                showBuffer();
                buffer.setCursor(row, col);
            }
            else
            redraw();
        }

        Position logical=new Position(buffer.getCursorRow(),buffer.getCursorCol());
        Position visualCursor=viewPos(logical);
            cursorPos.row = visualCursor.row;
            cursorPos.col = visualCursor.col;
            term.moveCursor(cursorPos.col,cursorPos.row);
    }

    private void redraw() {
        for (Integer line : modifiedLines) {
            showBufferLine(line);

        }
        modifiedLines.clear();
    }
}

class Position
{
    int col;
    int row;

    Position() {
        row=0;
        col=0;
    }

    Position(int row, int col) {
        this.row=row;
        this.col=col;
    }
}