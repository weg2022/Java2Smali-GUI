package weg.ide.tools.java2smali.common;

public class HighlightSpace {

    public int[] myTypes;
    public int[] myStartLines;
    public int[] myStartColumns;
    public int[] myEndLines;
    public int[] myEndColumns;
    public int mySize = 0;

    public HighlightSpace(int initSize) {
        myTypes = new int[initSize];
        myStartLines = new int[initSize];
        myStartColumns = new int[initSize];
        myEndLines = new int[initSize];
        myEndColumns = new int[initSize];
        mySize = 0;
    }

    public void reset() {
        mySize = 0;
    }

    public void highlight(int type, int startLine, int startColumn, int endLine, int endColumn) {
        resize(mySize);
        myTypes[mySize] = type;
        myStartLines[mySize] = startLine;
        myStartColumns[mySize] = startColumn;
        myEndLines[mySize] = endLine;
        myEndColumns[mySize] = endColumn;
        mySize++;
    }

    private void resize(int size) {
        if (myTypes.length <= size) {
            int[] types = new int[size * 5 / 4];
            System.arraycopy(myTypes, 0, types, 0, myTypes.length);
            myTypes = types;

            int[] startLines = new int[size * 5 / 4];
            System.arraycopy(myStartLines, 0, startLines, 0, myStartLines.length);
            myStartLines = startLines;

            int[] startColumns = new int[size * 5 / 4];
            System.arraycopy(myStartColumns, 0, startColumns, 0, myStartColumns.length);
            myStartColumns = startColumns;

            int[] endLines = new int[size * 5 / 4];
            System.arraycopy(myEndLines, 0, endLines, 0, myEndLines.length);
            myEndLines = endLines;

            int[] endColumns = new int[size * 5 / 4];
            System.arraycopy(myEndColumns, 0, endColumns, 0, myEndColumns.length);
            myEndColumns = endColumns;
        }
    }
}
