-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/REGEXP_INSTR.html
SELECT
  REGEXP_INSTR('500 Oracle Parkway, Redwood Shores, CA',
               '[s|r|p][[:alpha:]]{6}', 3, 2, 1, 'i') "REGEXP_INSTR"
  FROM DUAL;