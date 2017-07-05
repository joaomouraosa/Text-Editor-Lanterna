import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class BufferTest {

    private Buffer b;

    @Before
    public void initializeBuffer() {
        b = new Buffer();
        b.insert('a');
        b.newLine();
        b.insert('b');
        b.insert('b');
        b.newLine();
        b.insert('c');
        b.newLine();
        b.newLine();
        b.insert('d');
        b.insert('d');
        b.insert('d');
        b.setCursor(0,0);
    }

    @After
    public void test_invariants() {
        assertTrue(b.testInvariants());
    }

    @Test
    public void test_setCursor(){
        b.insert('a');
        assertTrue(b.setCursor(0,1));
    }

    @Test
    public void test_setCursor_1(){
//Testar sets com posiçoes invalidas
        b.setCursor(0,5);
        assertTrue(b.getCursorCol()==0);
        assertTrue(b.getCursorRow()==0);
    }

    @Test
    public void test_setCursor_2(){
        b.insert('a');
        b.insert('a');
        b.newLine();
        b.insert('a');
        b.insert('a');

        b.setCursor(1,2);
    }

    @Test
    public void testInsert(){
        b.insert('a');

        assertEquals(b.getNumberOfLines(),5);
        assertEquals(b.getCursorRow(),0);
        assertEquals(b.getCursorCol(),1);

        b.insert('a');

        assertEquals(b.getCursorRow(),0);
        assertEquals(b.getCursorCol(),2);
        assertTrue(b.toString().equals("aaa"));

// *Inserir no início da linha
        b.setCursor(0,0);
        b.insert('b');
        assertTrue(b.toString().equals("baaa"));
        assertEquals(b.getCursorRow(),0);
        assertEquals(b.getCursorCol(),1);

// inserir nova linha
        b.newLine();
        assertTrue(b.toString().equals("aaa"));

        assertTrue(b.getCursorCol()==0);
        assertTrue(b.getCursorRow()==1);
        assertTrue(b.getNumberOfLines()==6);

// *Inserir a meio da linha
        b.setCursor(1,2);
        b.insert('c');
        assertTrue(b.toString().equals("aaca"));

// *Inserir no fim da linha
        b.setCursor(1,4);
        b.insert('d');
        assertTrue(b.toString().equals("aacad"));

// *Inserir novas linhas no fim
        b.setCursor(5,3);
        b.newLine();
        b.newLine();
        b.newLine();
        assertTrue(b.getCursorCol()==0);
        assertTrue(b.getCursorRow()==8);

        //Inserir novas linhas a meio
        b.setCursor(1,1);
        b.newLine();
        b.setCursor(1,0);
        assertTrue(b.toString().equals("a"));
        b.setCursor(2,0);
        assertTrue(b.toString().equals("acad"));
        assertEquals(b.getNumberOfLines(),10);
    }

    @Test
    public void testMoveDown() {
// * Com linhas abaixo*

// Mantendo cursorCol
        b.moveDown();
        assertTrue(b.getCursorCol()==0);
        assertTrue(b.getCursorRow()==1);

//Linha mais pequena para linha maior
        b.setCursor(0,1);
        b.moveDown();
        assertTrue(b.getCursorRow()==1);
        assertTrue(b.getCursorCol()==1);

//Linha maior para mais pequena
        b.setCursor(1,2);
        b.insert('b');
        assertTrue(b.toString().equals("bbb"));
        b.moveDown();
        assertTrue(b.getCursorRow()==2);
        assertTrue(b.getCursorCol()==1);

// *Ultima linha*
        b.setCursor(4,0);
        b.moveDown();
        assertTrue(b.getCursorRow()==4);
        assertTrue(b.getCursorCol()==0);
    }

    @Test
    public void testMoveRight() {
//movimentos sem necessidade de mudar de linha
        b.moveRight();
        assertTrue(b.getCursorCol()==1);
        assertTrue(b.getCursorRow()==0);

        b.setCursor(1,0);
        b.moveRight();
        assertTrue(b.getCursorCol()==1);
        assertTrue(b.getCursorRow()==1);

//fim da linha
        b.setCursor(0,1);
        b.moveRight();
        assertTrue(b.getCursorCol()==0);
        assertTrue(b.getCursorRow()==1);

        b.setCursor(4,3);
        b.moveRight();
        assertTrue(b.getCursorCol()==3);
        assertTrue(b.getCursorRow()==4);
    }

    @Test
    public void testMoveUp() {
//sem linhas acima
        b.moveUp();
        assertTrue(b.getCursorCol()==0);
        assertTrue(b.getCursorRow()==0);

//mover para linha acima
        b.setCursor(1,0);
        b.moveUp();
        assertTrue(b.getCursorCol()==0);
        assertTrue(b.getCursorRow()==0);

//move para linha de tamanho menor
        b.setCursor(1,2);
        b.moveUp();
        assertTrue(b.getCursorCol()==1);
        assertTrue(b.getCursorRow()==0);

//move para linha de tamanho maior
        b.setCursor(2,0);
        b.moveUp();
        assertTrue(b.getCursorCol()==0);
        assertTrue(b.getCursorRow()==1);
    }

    @Test
    public void testMoveLeft() {
        //Sem linhas acima
        b.setCursor(0,0);
        b.moveLeft();
        assertTrue(b.getCursorCol()==0);
        assertTrue(b.getCursorRow()==0);

        b.setCursor(4,3);
        b.moveLeft();
        assertEquals(b.getCursorCol(),2);
        assertEquals(b.getCursorRow(),4);

// move para a esquerda a partir do inicio da linha
        b.setCursor(4,3);
        b.newLine();
        b.moveLeft();
        assertTrue(b.getCursorCol()==3);
        assertTrue(b.getCursorRow()==4);
    }

    @Test
    public void testDelete(){
// *Apagar no inicio da linha*

// ->Sem linha anterior
        b.setCursor(0,0);
        b.delete();
        assertEquals(b.getCursorRow(),0);
        assertEquals(b.getCursorCol(),0);

// ->Com linha anterior
        assertTrue(b.getLine(0).equals("a"));
        assertTrue(b.getLine(1).equals("bb"));
        b.setCursor(1,2);

        b.delete();

        assertTrue(b.getLine(0).equals("a"));
        assertTrue(b.getLine(1).equals("b"));


        b.setCursor(1,1);
        b.delete();
        b.delete();
        assertEquals(b.getNumberOfLines(),4);
        assertTrue(b.getLine(1).equals("c"));

// *Apagar a meio da linha*
        assertTrue(b.getLine(3).equals("ddd"));

        b.setCursor(3,2);
        b.delete();

        assertTrue(b.getLine(3).equals("dd"));

// *Apagar no fim da linha*

        b.setCursor(0,1);
        b.delete();
        assertTrue(b.toString().equals(""));
    }
}

