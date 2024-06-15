-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGEXP_REPLACE.html
SELECT empName, REGEXP_REPLACE (empName, 'Jane', 'John') "STRING_REPLACE" FROM regexp_temp;