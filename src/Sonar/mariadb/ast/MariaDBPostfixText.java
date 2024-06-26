package Sonar.mariadb.ast;

public class MariaDBPostfixText implements MariaDBExpression {

    
        private final MariaDBExpression expr;
        private final String text;


        public MariaDBPostfixText(MariaDBExpression expr, String text) {
            this.expr = expr;
            this.text = text;
        }

        public MariaDBExpression getExpr() {
            return expr;
        }

        public String getText() {
            return text;
        }
        
}
