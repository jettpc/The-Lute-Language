/*
  Name: Paul Jett
  Semester: Fall 2016
  Date: 12/2/16
  Lab: Part four of The Lute Language
 */

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class LuteTreeVisitor extends LuteBaseVisitor {

  private static class BreakException extends RuntimeException
  { }

    private static class ReturnException extends RuntimeException
    {
        Object variable;
        ReturnException(Object variable){
            this.variable = variable;
        }

        public Object getVariable(){
            return variable;
        }


    }

  private static class ContinueException extends RuntimeException {}

  // Memory space for variables
  private HashMap<String,Object> globalMemory;

    private HashMap<String,FunctionSymbol> symbolTable;

  //keeping track of functions defined by keeping an array of their names
  public List<String> globalNames = new ArrayList<String>();

  public LuteTreeVisitor() {
    globalMemory = new HashMap<String,Object>();
      symbolTable = new HashMap<String,FunctionSymbol>();
  }

  //stack of maps
  Stack<Map> myStack = new Stack<Map>();

  private static class FunctionSymbol {
    private String name;
    private List<String> parameters;
    private ParseTree stmtsTree;

    //constructor
    public FunctionSymbol(String nameP, List<String> paramListP, ParseTree stmtsP) {
      name = nameP;
      parameters = paramListP;
      stmtsTree =  stmtsP;
    }

    public String getName(){
        return name;
    }

    public List<String> getParam(){
        return parameters;
    }

    public ParseTree getStmts(){
        return stmtsTree;
    }

  }

  @Override
  public Object visitReturnStmt(LuteParser.ReturnStmtContext ctx) {

      if (ctx.getChild(1) != null) {

          throw new ReturnException(visit(ctx.getChild(1)));
      }
      return super.visitReturnStmt(ctx);
  }

  @Override
  public Object visitFctnDef(LuteParser.FctnDefContext ctx) {

      //store the name of the function
      String funcName = ctx.getChild(1).toString();
      globalNames.add(ctx.getChild(1).toString());

      //store the parameters if there are any
      List<String> paramNames = new ArrayList<String>();
      int paramCount = ctx.getChildCount();
      for(int i = 3; i < paramCount-3; i++) {    //range takes only inside params
        if(!ctx.getChild(i).toString().contains(","))
        paramNames.add(ctx.getChild(i).toString());
      }

      //store the Parse tree for the statements of the function
      ParseTree funcStmts = ctx.stmts();

      FunctionSymbol data = new FunctionSymbol(funcName, paramNames, funcStmts);

      symbolTable.put(funcName, data);

    return null;
  }

  @Override
  public Object visitFctnCall(LuteParser.FctnCallContext ctx) {

      //if the stack doesn't contain a previously defined function throw an error
      if(!globalNames.contains(ctx.getChild(0).toString())) {
          throw new RuntimeException(String.format(
                  "[Line %d] Method was not previously defined", ctx.start.getLine()));
      }

      int formalSize = 1;
      Map temp;

      //getting value of FunctionSymbol
      formalSize = symbolTable.get(ctx.getChild(0).toString()).getParam().size();

      //checking actual parameters by looking at the children
      int actualSize = 0;
      for(int i = 2; i < ctx.getChildCount()-1; i++) {    //range takes only inside params ()
          if(!ctx.getChild(i).toString().contains(","))
              actualSize++;
      }

      if(actualSize != formalSize) {
          throw new RuntimeException(String.format(
                  "[Line %d] formal parameter and actual parameter length are not equal", ctx.start.getLine()));
      }


      Object retValue = "";
      Map<String, Object> scope = new HashMap<String, Object>();
      for (int i = 0; i <= formalSize-1; i++) {
      //grabbing the map on top of the stack
      List funcData = (ArrayList) symbolTable.get(ctx.getChild(0).toString()).getParam(); //getting value of FunctionSymbol
      scope.put(symbolTable.get(ctx.getChild(0).toString()).getParam().get(i), visit(ctx.expr().get(i)));

      }
      myStack.push(scope);

      try {
          visit(symbolTable.get(ctx.getChild(0).toString()).getStmts());
      } catch (ReturnException e) {
          retValue = e.getVariable();
      }
      myStack.pop();

    return retValue;
  }

  @Override
  public Object visitUnaryPlusMinus(LuteParser.UnaryPlusMinusContext ctx) {
    Object val = visit(ctx.expr());

    // If it is a +, it is a no-op
    if( ctx.ADD_OP().getText().equals("+") )
      return val;

    if( val instanceof Integer )
    {
      return - ((Integer)val);
    }
    else if(val instanceof Double)
      return - ((Double)val);
    else
      throw new RuntimeException(
              String.format("[Line %d] Can't negate this value", ctx.start.getLine())
      );
  }

  @Override
  public Object visitMult(LuteParser.MultContext ctx)
  {
    List<LuteParser.ExprContext> exprChildren = ctx.expr();
    TerminalNode opNode = ctx.MULT_OP();

    Object leftResult = visit(exprChildren.get(0));
    Object rightResult = visit(exprChildren.get(1));

    if( leftResult instanceof Number && rightResult instanceof Number )
    {
      Number lhs = (Number) leftResult;
      Number rhs = (Number) rightResult;
      if( leftResult instanceof Double || rightResult instanceof Double )
      {
        double leftD = lhs.doubleValue();
        double rightD = rhs.doubleValue();
        if( opNode.getText().equals("*") )
          return leftD * rightD;
        else if( opNode.getText().equals("/"))
          return leftD / rightD;
        else if( opNode.getText().equals("%") )
          return leftD % rightD;
      }
      else
      {
        int leftI = lhs.intValue();
        int rightI = rhs.intValue();
        if( opNode.getText().equals("*"))
          return leftI * rightI;
        if( opNode.getText().equals("/"))
          return leftI / rightI;
        else if( opNode.getText().equals("%") )
          return leftI % rightI;
      }
    }

    // If we get here, the data types were unexpected.
    throw new RuntimeException(String.format(
            "[Line %d] Invalid types for operation: '%s'", ctx.start.getLine(), opNode.getText()));
  }

  @Override 
  public Object visitAdd(LuteParser.AddContext ctx) {

    // Get the list of expr children
    List<LuteParser.ExprContext> exprChildren = ctx.expr();
    // Get the operator (a TerminalNode)
    TerminalNode opNode = ctx.ADD_OP();

    // Evaluate the left and right sides of the operator
    Object leftResult = visit(exprChildren.get(0));
    Object rightResult = visit(exprChildren.get(1));

    // Are the data types numbers?
    if( leftResult instanceof Number && rightResult instanceof Number ) {
      Number lhs = (Number) leftResult;
      Number rhs = (Number) rightResult;

      // If one of the sides is a Double, we want to compute the result as a 
      // Double.  Otherwise, we use integer arithmetic.
      if( leftResult instanceof Double || rightResult instanceof Double ) {
        double leftD = lhs.doubleValue();
        double rightD = rhs.doubleValue();
        if( opNode.getText().equals("+") ) return leftD + rightD;
        else return leftD - rightD;
      } else {
        int leftI = lhs.intValue();
        int rightI = rhs.intValue();
        if( opNode.getText().equals("+")) return leftI + rightI;
        else return leftI - rightI;
      }
    }
    // If one of the sides is a String, we convert the other side to a 
    // String and concatenate.  But only if the operation is '+', if it
    // is '-', it is an invalid operation.
    else if( leftResult instanceof String || rightResult instanceof String ) {
      if( opNode.getText().equals("+") )
        return leftResult.toString() + rightResult.toString();
      throw new RuntimeException("'-' is an Invalid operation for the string type");
    }

    // If we get here the data types were unexpected.
    throw new RuntimeException(
        String.format("[Line %d] Invalid types for operation: '%s'", ctx.start.getLine(), opNode.getText()));
  }

  @Override
  public Object visitParen(LuteParser.ParenContext ctx ) {
    // All we need to do here is visit the middle child (the expr) and
    // return the result
    return visit( ctx.expr() );
  }

  @Override
  public Object visitIntLiteral(LuteParser.IntLiteralContext ctx) {
    // Convert the text of the node to an Integer object
    return new Integer( ctx.getText() );
  }

  @Override
  public Object visitFloatLiteral( LuteParser.FloatLiteralContext ctx ) {
    // Convert the text of the node to a Double object
    return new Double( ctx.getText() );
  }

  @Override
  public Object visitStringLiteral( LuteParser.StringLiteralContext ctx ) {
    // Remove the quotes and convert to a String object
    return ctx.getText().substring(1, ctx.getText().length() - 1);
  }

  @Override
  public Object visitId( LuteParser.IdContext ctx ) {
    // Get the name of the identifier
    String id = ctx.getText();

    // If we have a value for the variable, return that value
      if (myStack.size() == 0) {
          if (globalMemory.containsKey(id)) {
              return globalMemory.get(id);
          }
      }
      else {
          if (myStack.peek().containsKey(id)) {
              return myStack.peek().get(id);
          }
          else if (globalMemory.containsKey(id)) {
              return globalMemory.get(id);
          }
      }
    // If we get here, we don't have a value for the variable, so we have a
    // runtime error.
    throw new RuntimeException(
             String.format("[Line %d] Unrecognized variable: %s", ctx.start.getLine(), id));
  }

  @Override
  public Object visitPrintStmt( LuteParser.PrintStmtContext ctx ) {
    // Evaluate the expr child
    Object result = visit( ctx.expr() );

    // Print the result to the console
    System.out.println(result.toString());
    return null;
  }

  @Override
  public Object visitAssignStmt( LuteParser.AssignStmtContext ctx ) {
    // Get the name of the identifier
    String id = ctx.ID().getText();
    // Evaluate the right hand side
    Object value = visit( ctx.expr() );

    // Place the result into globalMemory
    if (myStack.size() == 0)
        globalMemory.put(id, value);
     else{
        myStack.peek().put(id,value);
    }

    return value;
  }

  @Override
  public Object visitIfStmt( LuteParser.IfStmtContext ctx ) {
    // Evaluate the expression
    Object result = visit(ctx.expr());

    // if it is a Boolean, we visit the stmtList if true
    // otherwise, we have a runtime error.
    if( result instanceof Boolean ) {
      boolean condition = (Boolean)result;
      if( condition )
        visit(ctx.stmts());
      return null;
    } else {
      throw new RuntimeException( String.format(
            "[Line %d] Invalid type for if statement, expected boolean", ctx.start.getLine() ) );
    }
  }

  @Override
  public Object visitIfElseStmt( LuteParser.IfElseStmtContext ctx ) {
    // Evaluate the expression
    Object result = visit(ctx.expr());

    // If it is a Boolean, we visit the first or second stmtList child,
    // otherwise it is a runtime error.
    if( result instanceof Boolean ) {
      boolean condition = (Boolean)result;
      if( condition )
        visit(ctx.stmts(0));
      else
        visit(ctx.stmts(1));
    } else {
      throw new RuntimeException( String.format(
            "[Line %d] Invalid type for if statement, expected boolean", ctx.start.getLine() ) );
    }
    return null;
  }

  @Override
  public Object visitIfElsifStmt(LuteParser.IfElsifStmtContext ctx) {
    List<LuteParser.ExprContext> conditions = ctx.expr();
    List<LuteParser.StmtsContext> stmts = ctx.stmts();

    Object result = visit(conditions.get(0));
    assureBoolean(result, conditions.get(0));

    if( (Boolean) result )
    {
      visit(stmts.get(0));
      return null;
    }

    for( int i = 1; i < conditions.size(); i++ ) {
      result = visit(conditions.get(i));
      assureBoolean(result, conditions.get(i));
      if( (Boolean) result )
      {
        visit(stmts.get(i));
        return null;
      }
    }

    // If we get here, none of the expressions were true, so we do the optional else if it exists
    if( stmts.size() > conditions.size() ) {
      visit(stmts.get(stmts.size() - 1) );
      return null;
    }

    return null;
  }

  @Override
  public Object visitWhile(LuteParser.WhileContext ctx) {

    Object e = visit(ctx.expr());
    assureBoolean(e, ctx.expr());

    try {
      while ((Boolean) e) {

        try {
          visit(ctx.stmts());
        } catch(ContinueException cex) { /* do nothing, loop continues */ }

        e = visit(ctx.expr());
        if (!(e instanceof Boolean)) {
          throw new RuntimeException(String.format(
                  "[Line %d] Invalid type for while, expected boolean", ctx.start.getLine()));
        }
      }
    }
    catch(BreakException ex)
    {
      // Do nothing, loop was broken.
    }

    return null;
  }

  @Override
  public Object visitUnaryNot( LuteParser.UnaryNotContext ctx ) {
    // Evaluate the expr
    Object exprResult = visit(ctx.expr());

    // If it is a boolean, return the opposite, otherwise
    // we have a runtime error
    if( exprResult instanceof Boolean ) {
      return ! (Boolean)exprResult;
    }

    throw new RuntimeException( String.format(
          "[Line %d] Invalid type for 'not' operator", ctx.start.getLine() ) );
  }

  @Override
  public Object visitComparison( LuteParser.ComparisonContext ctx ) {
    // Evaluate the left and right expressions
    Object leftResult = visit(ctx.expr().get(0));
    Object rightResult = visit(ctx.expr().get(1));


    // Get the text of the operator
    String op = ctx.COMP_OP().getText();

    // If the left and right sides are numbers, evaluate as a
    // numerical comparision, with coercion to Double if one
    // of the two is a Double.
    if( leftResult instanceof Number && rightResult instanceof Number ) {
      if( leftResult instanceof Double || rightResult instanceof Double ) {
        double left = ((Number)leftResult).doubleValue();
        double right = ((Number)rightResult).doubleValue();

        if( op.equals(">") ) {
          return left > right;
        } else if (op.equals("<")) {
          return left < right;
        } else if (op.equals(">=")) {
          return left >= right;
        } else if (op.equals("<=")) {
          return left <= right;
        } else {
          throw new RuntimeException("Internal error");
        }
      }
      else {
        int left = ((Number)leftResult).intValue();
        int right = ((Number)rightResult).intValue();

        if( op.equals(">") ) {
          return left > right;
        } else if (op.equals("<")) {
          return left < right;
        } else if (op.equals(">=")) {
          return left >= right;
        } else if (op.equals("<=")) {
          return left <= right;
        } else {
          throw new RuntimeException("Internal error");
        }
      }
    }

    throw new RuntimeException( String.format(
          "[Line %d] Invalid types for operation %s", ctx.start.getLine(), op ) );
  }

  @Override
  public Object visitAnd(LuteParser.AndContext ctx) {
    // Evaluate the left and right sides
    Object leftResult = visit(ctx.expr().get(0));
    Object rightResult = visit(ctx.expr().get(1));

    assureBoolean(leftResult, ctx.expr(0));
    assureBoolean(rightResult, ctx.expr(1));

    return (Boolean)leftResult && (Boolean)rightResult;
  }

  @Override
  public Object visitOr(LuteParser.OrContext ctx) {
    // Evaluate the left and right sides
    Object leftResult = visit(ctx.expr(0));
    Object rightResult = visit(ctx.expr(1));

    assureBoolean(leftResult, ctx.expr(0));
    assureBoolean(rightResult, ctx.expr(1));

    return (Boolean)leftResult || (Boolean)rightResult;
  }

  @Override
  public Object visitBoolLiteral( LuteParser.BoolLiteralContext ctx ) {
    // Convert the text of the node to Boolean
    return new Boolean( ctx.getText() );
  }

  @Override
  public Object visitEquality(LuteParser.EqualityContext ctx) {
    // Evaluate the left and right expressions
    Object leftResult = visit(ctx.expr().get(0));
    Object rightResult = visit(ctx.expr().get(1));

    // Get the text of the operator
    String op = ctx.EQUALITY_OP().getText();

    // If the left and right sides are numbers, evaluate as a
    // numerical comparision, with coercion to Double if one
    // of the two is a Double.
    if (leftResult instanceof Number && rightResult instanceof Number) {
      if (leftResult instanceof Double || rightResult instanceof Double) {
        double left = ((Number) leftResult).doubleValue();
        double right = ((Number) rightResult).doubleValue();

        if (op.equals("==")) {
          return left == right;
        } else {
          return left != right;
        }
      } else {
        int left = ((Number) leftResult).intValue();
        int right = ((Number) rightResult).intValue();

        if (op.equals("==")) {
          return left == right;
        } else {
          return left != right;
        }
      }
    }
    // If the left and right are strings
    else if (leftResult instanceof String && rightResult instanceof String) {
      String left = leftResult.toString();
      String right = rightResult.toString();

      if (op.equals("==")) {
        return left.equals(right);
      } else {
        return !left.equals(right);
      }
    }
    else if( leftResult instanceof Boolean && rightResult instanceof Boolean ) {
      boolean left = (Boolean)leftResult;
      boolean right = (Boolean)rightResult;
      if( op.equals("==") )
        return left == right;
      else
        return left != right;
    }
    else {
      throw new RuntimeException(String.format(
              "[Line %d] Invalid types for operation %s", ctx.start.getLine(), op));
    }
  }

  @Override
  public Object visitBreakStmt(LuteParser.BreakStmtContext ctx) {
    throw new BreakException();
  }

  @Override
  public Object visitContinueStmt(LuteParser.ContinueStmtContext ctx) {
    throw new ContinueException();
  }

  private void assureBoolean(Object result, ParserRuleContext context)
  {
    if(! ( result instanceof Boolean ) )
      throw new RuntimeException(String.format(
              "[Line %d] Expected boolean", context.start.getLine()));
  }
}
