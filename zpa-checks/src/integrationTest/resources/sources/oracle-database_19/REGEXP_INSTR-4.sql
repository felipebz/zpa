-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/REGEXP_INSTR.html
SELECT REGEXP_INSTR('1234567890', '(123)(4(56)(78))', 1, 1, 0, 'i', 2) 
"REGEXP_INSTR" FROM DUAL;