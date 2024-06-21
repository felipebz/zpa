-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-name-resolution.html
SELECT * FROM hr.tab1 p1
WHERE EXISTS (SELECT * FROM hr.tab2 p2 WHERE p2.x = p1.tab2.a);