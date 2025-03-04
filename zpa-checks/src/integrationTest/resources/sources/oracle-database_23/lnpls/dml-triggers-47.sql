-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/dml-triggers.html
DROP TABLE Library_table;
CREATE TABLE Library_table (Section VARCHAR2(20));
INSERT INTO Library_table (Section)
VALUES ('Novel');
INSERT INTO Library_table (Section)
VALUES ('Classic');
SELECT * FROM Library_table ORDER BY Section;