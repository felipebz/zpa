-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/sys_row_etag.html
CREATE TABLE foo (c1 NUMBER, c2 NUMBER, c3 NUMBER);

Table created.

INSERT INTO foo VALUES (1, 2, 3);

1 row created.

SELECT SYS_ROW_ETAG(c2, c1) FROM foo;