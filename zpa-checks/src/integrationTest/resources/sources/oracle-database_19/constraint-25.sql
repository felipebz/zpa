-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/constraint.html
CREATE TABLE games (scores NUMBER CHECK (scores >= 0));