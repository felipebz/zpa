-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overview-polymorphic-table-functions.html
SELECT * FROM skip_col_by_type(scott.dept, 'number');