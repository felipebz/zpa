-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Pattern-matching-Conditions.html
CREATE TABLE ducks (f CHAR(6), v VARCHAR2(6));
INSERT INTO ducks VALUES ('DUCK', 'DUCK');
SELECT '*'||f||'*' "char",
   '*'||v||'*' "varchar"
   FROM ducks;