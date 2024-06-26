package Sonar.common.gen;

import java.util.List;

import Sonar.common.query.ExpectedErrors;
import Sonar.common.schema.AbstractTableColumn;

public abstract class AbstractUpdateGenerator<C extends AbstractTableColumn<?, ?>> {

    protected final ExpectedErrors errors = new ExpectedErrors();
    protected StringBuilder sb = new StringBuilder();

    protected void updateColumns(List<C> columns) {
        for (int nrColumn = 0; nrColumn < columns.size(); nrColumn++) {
            if (nrColumn != 0) {
                sb.append(", ");
            }
            sb.append(columns.get(nrColumn).getName());
            sb.append("=");
            updateValue(columns.get(nrColumn));
        }
    }

    protected abstract void updateValue(C column);

}
