-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-VIEW.html
INSERT INTO locations_view VALUES
   (999, 'Entertainment', 87, 'Roma');
INSERT INTO locations_view VALUES
*
ERROR at line 1:
ORA-01776: cannot modify more than one base table through a join view