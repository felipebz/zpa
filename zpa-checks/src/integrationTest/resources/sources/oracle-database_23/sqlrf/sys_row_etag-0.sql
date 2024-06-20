-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/sys_row_etag.html
CREATE TABLE foo (c1 NUMBER, c2 NUMBER, c3 NUMBER);
INSERT INTO foo VALUES (1, 2, 3);
SELECT SYS_ROW_ETAG(c2, c1) FROM foo;