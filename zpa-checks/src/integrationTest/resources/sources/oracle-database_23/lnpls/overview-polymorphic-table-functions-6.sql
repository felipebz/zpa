-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overview-polymorphic-table-functions.html
SELECT * FROM skip_col_pkg.skip_col(scott.dept, 'number');