package Sonar.mysql.ast;



public class MySQLPostfixText implements MySQLExpression{
    private final MySQLExpression expr;
    private final String text;


    public MySQLPostfixText(MySQLExpression expr, String text) {
        this.expr = expr;
        this.text = text;
    }

    public MySQLExpression getExpr() {
        return expr;
    }

    public String getText() {
        return text;
    }

}