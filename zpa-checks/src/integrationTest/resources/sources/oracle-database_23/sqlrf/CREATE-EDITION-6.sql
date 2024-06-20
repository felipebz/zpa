-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-EDITION.html
ALTER SESSION SET EDITION = TEST_ED;
DROP VIEW e_view;
ALTER SESSION SET EDITION = ORA$BASE;
DESCRIBE e_view;