-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-INDEX.html
CREATE MULTIVALUE INDEX mvi_1 ON mytable t
      (t.jcol.credit_score.numberOnly());