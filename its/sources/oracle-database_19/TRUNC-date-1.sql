-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TRUNC-date.html
WITH dates AS (   
  SELECT date'2015-01-01' d FROM dual union   
  SELECT date'2015-01-10' d FROM dual union   
  SELECT date'2015-02-01' d FROM dual union   
  SELECT timestamp'2015-03-03 23:45:00' d FROM dual union   
  SELECT timestamp'2015-04-11 12:34:56' d FROM dual    
)   
SELECT d "Original Date",   
       trunc(d) "Nearest Day, Time Removed",   
       trunc(d, 'ww') "Nearest Week", 
       trunc(d, 'iw') "Start of Week",   
       trunc(d, 'mm') "Start of Month",   
       trunc(d, 'year') "Start of Year"   
FROM dates;