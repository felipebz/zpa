-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/APPROX_PERCENTILE_DETAIL.html
SET LINESIZE 300
SET PAGESIZE 0
COLUMN plan_table_output FORMAT A150

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(format=>'BASIC'));