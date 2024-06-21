-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
SELECT * FROM skip_col_pkg.skip_col(scott.dept, 'number');