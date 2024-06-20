-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/COLLATION.html
CREATE TABLE id_table
  (name VARCHAR2(64) COLLATE BINARY_AI,
   id VARCHAR2(8) COLLATE BINARY_CI);
INSERT INTO id_table VALUES('Christopher', 'ABCD1234');
SELECT COLLATION(name), COLLATION(id)
  FROM id_table;