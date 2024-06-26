package Sonar.tidb.ast;

import Sonar.Randomly;

public class TiDBDateExpression implements TiDBExpression{

    private String dateExp;

    public enum DateFunction {
        DATA_SUB {
            @Override
            public TiDBDateExpression execute() {
                String date = null;
                if (Randomly.getBoolean()) {
                    date = Randomly.getRandomDate();
                } else {
                    date = Randomly.getRandomDateTime();
                }

                String exp = Randomly.getNotCachedInteger(1, 1000) + "";
                String[] units = {"MICROSECOND", "SECOND", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "YEAR"};
                String unit = Randomly.fromOptions(units);
                return new TiDBDateExpression("DATE_SUB('" + date + "' ,INTERVAL " + exp + " " + unit + ")");
            }
        },
        DATE_ADD {
            @Override
            public TiDBDateExpression execute() {
                String date = null;
                if (Randomly.getBoolean()) {
                    date = Randomly.getRandomDate();
                } else {
                    date = Randomly.getRandomDateTime();
                }

                String exp = Randomly.getNotCachedInteger(1, 1000) + "";
                String[] units = {"MICROSECOND", "SECOND", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "YEAR"};
                String unit = Randomly.fromOptions(units);
                return new TiDBDateExpression("DATE_ADD('" + date + "' ,INTERVAL " + exp + " " + unit + ")");
            }
        },

        ADDDATE {
            @Override
            public TiDBDateExpression execute() {
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
                    return new TiDBDateExpression("ADDDATE('" + date + "' ,INTERVAL " + exp + " " + unit + ")");
                } else {
                    String randomDate = Randomly.getRandomDate();
                    String days = Randomly.getNotCachedInteger(1, 1000) + "";
                    return new TiDBDateExpression("ADDDATE('" + randomDate + "', " + days + ")");
                }
            }
        },

        SUBDATE {
            public TiDBDateExpression execute() {
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
                    return new TiDBDateExpression("SUBDATE('" + date + "' ,INTERVAL " + exp + " " + unit + ")");
                } else {
                    String randomDate = Randomly.getRandomDate();
                    String days = Randomly.getNotCachedInteger(1, 1000) + "";
                    return new TiDBDateExpression("SUBDATE('" + randomDate + "', " + days + ")");
                }
            }
        },

        SUBTIME {
            public TiDBDateExpression execute() {
                String date = Randomly.getRandomDateTime();
                String time = Randomly.getRandomDAY_MICROSECOND();
                return new TiDBDateExpression("SUBTIME('" + date + "', '" + time + "')");
            }
        },

        ADDTIME {
            @Override
            public TiDBDateExpression execute() {
                String date = Randomly.getRandomDateTime();
                String time = Randomly.getRandomDAY_MICROSECOND();
                return new TiDBDateExpression("ADDTIME('" + date + "', '" + time + "')");
            }
        },
        TIMEDIFF {
            public TiDBDateExpression execute() {
                String expr1 = Randomly.getRandomDateTime();
                String expr2 = Randomly.getRandomDateTime();
                return new TiDBDateExpression("TIMEDIFF('" + expr1 + "', '" + expr2 + "')");
            }
        },

        CONVERT_TZ {
            @Override
            public TiDBDateExpression execute() {
                return new TiDBDateExpression("CONVERT_TZ('" + Randomly.getRandomDateTime() + "', " + "'GMT','MET')");
            }
        },
        CURDATE {
            @Override
            public TiDBDateExpression execute() {
                return new TiDBDateExpression("CURDATE()");
            }
        },






        DATE {
            @Override
            public TiDBDateExpression execute() {
                return new TiDBDateExpression(String.format("DATE('%s')", Randomly.getRandomDateTime()));
            }
        },
        DAYOFMONTH {
            @Override
            public TiDBDateExpression execute() {
                return new TiDBDateExpression(String.format("DAYOFMONTH('%s')", Randomly.getRandomDate()));
            }
        },
        PERIOD_ADD {
            @Override
            public TiDBDateExpression execute() {
                return new TiDBDateExpression(String.format("PERIOD_ADD(%s,%s)", Randomly.getRandomYearMonth(), Randomly.getNotCachedInteger(1, 12)));
            }
        },
        PERIOD_DIFF {
            @Override
            public TiDBDateExpression execute() {
                return new TiDBDateExpression(String.format("PERIOD_DIFF(%s,%s)", Randomly.getRandomYearMonth(), Randomly.getRandomYearMonth()));
            }
        },

        TIMESTAMPADD {
            @Override
            public TiDBDateExpression execute() {
                String[] units = {"MICROSECOND", "SECOND", "MINUTE", "HOUR", "DAY", "WEEK", "MONTH", "QUARTER", "YEAR"};
                return new TiDBDateExpression(String.format("TIMESTAMPADD(%s, %s , '%s')",
                        Randomly.fromOptions(units),
                        Randomly.getNotCachedInteger(1, 12),
                        Randomly.getRandomDate()));
            }
        },
        UTC_DATE {
            @Override
            public TiDBDateExpression execute() {
                return new TiDBDateExpression("UTC_DATE()");
            }
        },
        TIMESTAMP {
            @Override
            public TiDBDateExpression execute() {
                String data = Randomly.getRandomDate();
                return new TiDBDateExpression("TIMESTAMP('" + data + "')");
            }
        },
        ;


        public abstract TiDBDateExpression execute();

    }

    public TiDBDateExpression(String dateExp) {
        this.dateExp = dateExp;
    }

    public void setDateExp(String dateExp) {
        this.dateExp = dateExp;
    }

    public String getDateExp() {
        return dateExp;
    }

    
}
