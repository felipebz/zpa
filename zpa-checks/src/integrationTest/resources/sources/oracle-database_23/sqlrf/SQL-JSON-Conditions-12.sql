-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SQL-JSON-Conditions.html
SQL> INSERT INTO jsontab1(jschd) VALUES (json('3.1415'));
INSERT INTO jsontab1(jschd) VALUES (json('3.1415'))
*
ERROR at line 1:
ORA-02290: check constraint (SYS.JSCHDSV) violated