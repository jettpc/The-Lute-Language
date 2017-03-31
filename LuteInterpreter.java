/*
  Name: Paul Jett
  Semester: Fall 2016
  Date: 12/2/16
  Lab: Part four of The Lute Language
 */

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class LuteInterpreter
{
  public static void main( String[] args ) throws IOException
  {
    // Open the input file as a ANTLRInputStream
    String fName = "lute.txt";
    ANTLRInputStream input = null;
    try
    {
      input = new ANTLRInputStream( new FileInputStream( fName ) );
    }
    catch( FileNotFoundException e )
    {
      System.err.println("File: " + fName + " not found.");
      System.exit(1);
    }

    // At this point, the file has been opened and we need to create
    // a lexer and a parser
    LuteLexer lex = new LuteLexer( input );
    LuteParser parser = new LuteParser(new CommonTokenStream(lex));

    // Generate the parse tree. Call the method corresponding to
    // the start symbol in the grammar here!
    ParseTree tree = parser.start();

    // Start the process of traversing the tree by visiting the root
    // node.
    LuteTreeVisitor visitor = new LuteTreeVisitor();
    visitor.visit(tree);
  }
}
