-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-MATERIALIZED-VIEW.html
ALTER MATERIALIZED VIEW emp_data
   REFRESH COMPLETE   
   START WITH TRUNC(SYSDATE+1) + 9/24  
   NEXT SYSDATE+7;