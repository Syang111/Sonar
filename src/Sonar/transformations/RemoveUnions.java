package Sonar.transformations;

import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;

import java.util.List;

/**
 * try removing sub selects of a union statement.
 *
 * e.g. select 1 union select 2 -> select 1
 */

public class RemoveUnions extends JSQLParserBasedTransformation {

    private final SelectDeParser remover = new SelectDeParser() {
        @Override
        public void visit(SetOperationList list) {
            List<SelectBody> selectBodyList = list.getSelects();
            tryRemoveElms(list, selectBodyList, SetOperationList::setSelects);
            super.visit(list);
        }
    };

    public RemoveUnions() {
        super("remove union selects");
    }

    @Override
    public boolean init(String sql) {

        boolean baseSuc = super.init(sql);
        if (!baseSuc) {
            return false;
        }

        this.remover.setExpressionVisitor(new ExpressionDeParser(remover, new StringBuilder()));
        return true;
    }

    @Override
    public void apply() {
        super.apply();
        if (statement instanceof Select) {
            Select select = (Select) statement;
            select.getSelectBody().accept(remover);
        }
    }
}
