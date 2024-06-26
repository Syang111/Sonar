package Sonar.mysql.ast;

import io.questdb.cairo.pool.ReaderPool;
import Sonar.Randomly;

public class MySQLDataExpression implements MySQLExpression {

    private String dateExp;

    public enum DateFunction {
        DATA_SUB {
            @Override
            public MySQLDataExpression execute() {
                String date = null;
                if (Randomly.getBoolean()) {
                    date = Randomly.getRandomDate();
                } else {
                    date = Randomly.getRandomDateTime();
                }

                String exp = Randomly.getNotCachedInteger(1, 1000) + "";
                String[] units = {"MICROSECOND", "SECOND", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "YEAR"};
                String unit = Randomly.fromOptions(units);
                return new MySQLDataExpression("DATE_SUB('" + date + "' ,INTERVAL " + exp + " " + unit + ")");
            }
        },
        DATE_ADD {
            @Override
            public MySQLDataExpression execute() {
                String date = null;
                if (Randomly.getBoolean()) {
                    date = Randomly.getRandomDate();
                } else {
                    date = Randomly.getRandomDateTime();
                }

                String exp = Randomly.getNotCachedInteger(1, 1000) + "";
                String[] units = {"MICROSECOND", "SECOND", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "YEAR"};
                String unit = Randomly.fromOptions(units);
                return new MySQLDataExpression("DATE_ADD('" + date + "' ,INTERVAL " + exp + " " + unit + ")");
            }
        },

        ADDDATE {
            @Override
            public MySQLDataExpression execute() {
                if (Randomly.getBoolean()) {
                    String date = null;
                    if (Randomly.getBoolean()) {
                        date = Randomly.getRandomDate();
                    } else {
                        date = Randomly.getRandomDateTime();
                    }

                    String exp = Randomly.getNotCachedInteger(1, 1000) + "";
                    String[] units = {"MICROSECOND", "SECOND", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "YEAR"};
                    String unit = Randomly.fromOptions(units);
                    return new MySQLDataExpression("ADDDATE('" + date + "' ,INTERVAL " + exp + " " + unit + ")");
                } else {
                    String randomDate = Randomly.getRandomDate();
                    String days = Randomly.getNotCachedInteger(1, 1000) + "";
                    return new MySQLDataExpression("ADDDATE('" + randomDate + "', " + days + ")");
                }
            }
        },

        SUBDATE {
            public MySQLDataExpression execute() {
                if (Randomly.getBoolean()) {
                    String date = null;
                    if (Randomly.getBoolean()) {
                        date = Randomly.getRandomDate();
                    } else {
                        date = Randomly.getRandomDateTime();
                    }

                    String exp = Randomly.getNotCachedInteger(1, 1000) + "";
                    String[] units = {"MICROSECOND", "SECOND", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "YEAR"};
                    String unit = Randomly.fromOptions(units);
                    return new MySQLDataExpression("SUBDATE('" + date + "' ,INTERVAL " + exp + " " + unit + ")");
                } else {
                    String randomDate = Randomly.getRandomDate();
                    String days = Randomly.getNotCachedInteger(1, 1000) + "";
                    return new MySQLDataExpression("SUBDATE('" + randomDate + "', " + days + ")");
                }
            }
        },

        SUBTIME {
            public MySQLDataExpression execute() {
                String date = Randomly.getRandomDateTime();
                String time = Randomly.getRandomDAY_MICROSECOND();
                return new MySQLDataExpression("SUBTIME('" + date + "', '" + time + "')");
            }
        },

        ADDTIME {
            @Override
            public MySQLDataExpression execute() {
                String date = Randomly.getRandomDateTime();
                String time = Randomly.getRandomDAY_MICROSECOND();
                return new MySQLDataExpression("ADDTIME('" + date + "', '" + time + "')");
            }
        },
        TIMEDIFF {
            public MySQLDataExpression execute() {
                String expr1 = Randomly.getRandomDateTime();
                String expr2 = Randomly.getRandomDateTime();
                return new MySQLDataExpression("TIMEDIFF('" + expr1 + "', '" + expr2 + "')");
            }
        },

        CONVERT_TZ {
            @Override
            public MySQLDataExpression execute() {
                return new MySQLDataExpression("CONVERT_TZ('" + Randomly.getRandomDateTime() + "', " +"'GMT','MET')");
            }
        },
        CURDATE {
            @Override
            public MySQLDataExpression execute() {
                return new MySQLDataExpression("CURDATE()");
            }
        },






        DATE{
            @Override
            public MySQLDataExpression execute() {
                return new MySQLDataExpression(String.format("DATE('%s')", Randomly.getRandomDateTime()));
            }
        },
        DAYOFMONTH {
            @Override
            public MySQLDataExpression execute() {
                return new MySQLDataExpression(String.format("DAYOFMONTH('%s')", Randomly.getRandomDate()));
            }
        },
        PERIOD_ADD {
            @Override
            public MySQLDataExpression execute() {
                return new MySQLDataExpression(String.format("PERIOD_ADD(%s,%s)", Randomly.getRandomYearMonth(), Randomly.getNotCachedInteger(1, 12)));
            }
        },
        PERIOD_DIFF {
            @Override
            public MySQLDataExpression execute() {
                return new MySQLDataExpression(String.format("PERIOD_DIFF(%s,%s)", Randomly.getRandomYearMonth(), Randomly.getRandomYearMonth()));
            }
        },











        UTC_DATE {
            @Override
            public MySQLDataExpression execute() {
                return new MySQLDataExpression("UTC_DATE()");
            }
        },
        TIMESTAMP {
            @Override
            public MySQLDataExpression execute() {
                String data = Randomly.getRandomDate();
                return new MySQLDataExpression("TIMESTAMP('" + data + "')");
            }
        },
        ;


        public abstract MySQLDataExpression execute();

    }

    public MySQLDataExpression(String dateExp) {
        this.dateExp = dateExp;
    }

    public void setDateExp(String dateExp) {
        this.dateExp = dateExp;
    }

    public String getDateExp() {
        return dateExp;
    }

    @Override
    public MySQLConstant getExpectedValue() {
        return null;
    }
}
