-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SQL-JSON-Conditions.html
INSERT INTO jsontab1(j) VALUES ('["a", "b"]');
INSERT INTO jsontab1(j) VALUES ('["a", "b"]')
*
ERROR at line 1:
ORA-02290: check constraint (SYS.JT1ISJ) violated