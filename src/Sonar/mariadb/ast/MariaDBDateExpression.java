package Sonar.mariadb.ast;

import Sonar.Randomly;
import Sonar.mysql.ast.MySQLConstant;

public class MariaDBDateExpression implements MariaDBExpression  {
    private String dateExp;

    public enum DateFunction {
        DATA_SUB {
            @Override
            public MariaDBDateExpression execute() {
                String date = null;
                if (Randomly.getBoolean()) {
                    date = Randomly.getRandomDate();
                } else {
                    date = Randomly.getRandomDateTime();
                }

                String exp = Randomly.getNotCachedInteger(1, 1000) + "";
                String[] units = {"MICROSECOND", "SECOND", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "YEAR"};
                String unit = Randomly.fromOptions(units);
                return new MariaDBDateExpression("DATE_SUB('" + date + "' ,INTERVAL " + exp + " " + unit + ")");
            }
        },
        DATE_ADD {
            @Override
            public MariaDBDateExpression execute() {
                String date = null;
                if (Randomly.getBoolean()) {
                    date = Randomly.getRandomDate();
                } else {
                    date = Randomly.getRandomDateTime();
                }

                String exp = Randomly.getNotCachedInteger(1, 1000) + "";
                String[] units = {"MICROSECOND", "SECOND", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "YEAR"};
                String unit = Randomly.fromOptions(units);
                return new MariaDBDateExpression("DATE_ADD('" + date + "' ,INTERVAL " + exp + " " + unit + ")");
            }
        },

        ADDDATE {
            @Override
            public MariaDBDateExpression execute() {
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
                    return new MariaDBDateExpression("ADDDATE('" + date + "' ,INTERVAL " + exp + " " + unit + ")");
                } else {
                    String randomDate = Randomly.getRandomDate();
                    String days = Randomly.getNotCachedInteger(1, 1000) + "";
                    return new MariaDBDateExpression("ADDDATE('" + randomDate + "', " + days + ")");
                }
            }
        },

        SUBDATE {
            public MariaDBDateExpression execute() {
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
                    return new MariaDBDateExpression("SUBDATE('" + date + "' ,INTERVAL " + exp + " " + unit + ")");
                } else {
                    String randomDate = Randomly.getRandomDate();
                    String days = Randomly.getNotCachedInteger(1, 1000) + "";
                    return new MariaDBDateExpression("SUBDATE('" + randomDate + "', " + days + ")");
                }
            }
        },

        SUBTIME {
            public MariaDBDateExpression execute() {
                String date = Randomly.getRandomDateTime();
                String time = Randomly.getRandomDAY_MICROSECOND();
                return new MariaDBDateExpression("SUBTIME('" + date + "', '" + time + "')");
            }
        },

        ADDTIME {
            @Override
            public MariaDBDateExpression execute() {
                String date = Randomly.getRandomDateTime();
                String time = Randomly.getRandomDAY_MICROSECOND();
                return new MariaDBDateExpression("ADDTIME('" + date + "', '" + time + "')");
            }
        },
        TIMEDIFF {
            public MariaDBDateExpression execute() {
                String expr1 = Randomly.getRandomDateTime();
                String expr2 = Randomly.getRandomDateTime();
                return new MariaDBDateExpression("TIMEDIFF('" + expr1 + "', '" + expr2 + "')");
            }
        },

        CONVERT_TZ {
            @Override
            public MariaDBDateExpression execute() {
                return new MariaDBDateExpression("CONVERT_TZ('" + Randomly.getRandomDateTime() + "', " + "'GMT','MET')");
            }
        },
        CURDATE {
            @Override
            public MariaDBDateExpression execute() {
                return new MariaDBDateExpression("CURDATE()");
            }
        },






        DATE {
            @Override
            public MariaDBDateExpression execute() {
                return new MariaDBDateExpression(String.format("DATE('%s')", Randomly.getRandomDateTime()));
            }
        },
        DAYOFMONTH {
            @Override
            public MariaDBDateExpression execute() {
                return new MariaDBDateExpression(String.format("DAYOFMONTH('%s')", Randomly.getRandomDate()));
            }
        },
        PERIOD_ADD {
            @Override
            public MariaDBDateExpression execute() {
                return new MariaDBDateExpression(String.format("PERIOD_ADD(%s,%s)", Randomly.getRandomYearMonth(), Randomly.getNotCachedInteger(1, 12)));
            }
        },
        PERIOD_DIFF {
            @Override
            public MariaDBDateExpression execute() {
                return new MariaDBDateExpression(String.format("PERIOD_DIFF(%s,%s)", Randomly.getRandomYearMonth(), Randomly.getRandomYearMonth()));
            }
        },

        TIMESTAMPADD {
            @Override
            public MariaDBDateExpression execute() {
                String[] units = {"MICROSECOND", "SECOND", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "YEAR"};
                return new MariaDBDateExpression(String.format("TIMESTAMPADD(%s, %s , '%s')",
                        Randomly.fromOptions(units),
                        Randomly.getNotCachedInteger(1, 12),
                        Randomly.getRandomDate()));
            }
        },
        UTC_DATE {
            @Override
            public MariaDBDateExpression execute() {
                return new MariaDBDateExpression("UTC_DATE()");
            }
        },
        TIMESTAMP {
            @Override
            public MariaDBDateExpression execute() {
                String data = Randomly.getRandomDate();
                return new MariaDBDateExpression("TIMESTAMP('" + data + "')");
            }
        },
        ;


        public abstract MariaDBDateExpression execute();

    }

    public MariaDBDateExpression(String dateExp) {
        this.dateExp = dateExp;
    }

    public void setDateExp(String dateExp) {
        this.dateExp = dateExp;
    }

    public String getDateExp() {
        return dateExp;
    }

}
