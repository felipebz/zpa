-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/REGEXP_INSTR.html
SELECT
  REGEXP_INSTR('500 Oracle Parkway, Redwood Shores, CA',
               '[^ ]+', 1, 6) "REGEXP_INSTR"
  FROM DUAL;