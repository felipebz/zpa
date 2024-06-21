-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
SELECT * FROM TABLE(tf2(CURSOR(SELECT * FROM TABLE(tf1()))));