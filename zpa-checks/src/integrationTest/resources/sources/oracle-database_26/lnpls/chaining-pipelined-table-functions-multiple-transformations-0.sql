-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/chaining-pipelined-table-functions-multiple-transformations.html
SELECT * FROM TABLE(table_function_name(parameter_list))