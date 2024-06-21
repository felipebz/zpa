-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-TYPE-statement.html
CREATE TYPE demo_typ1 AS OBJECT (a1 NUMBER, a2 NUMBER);
/
CREATE TABLE demo_tab1 (b1 NUMBER, b2 demo_typ1);
INSERT INTO demo_tab1 VALUES (1, demo_typ1(2,3));