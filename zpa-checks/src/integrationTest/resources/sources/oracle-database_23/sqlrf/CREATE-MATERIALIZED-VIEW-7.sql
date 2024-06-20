-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-MATERIALIZED-VIEW.html
CREATE MATERIALIZED VIEW LOG ON employees
   WITH PRIMARY KEY
   INCLUDING NEW VALUES;
CREATE MATERIALIZED VIEW emp_data 
   PCTFREE 5 PCTUSED 60 
   TABLESPACE example 
   STORAGE (INITIAL 50K)
   REFRESH FAST NEXT sysdate + 7 
   AS SELECT * FROM employees;