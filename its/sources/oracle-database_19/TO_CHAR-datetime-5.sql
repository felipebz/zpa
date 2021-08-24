WITH dates AS (  
  SELECT date'2015-01-01' d FROM dual union  
  SELECT date'2015-01-10' d FROM dual union  
  SELECT date'2015-02-01' d FROM dual union 
  SELECT timestamp'2015-03-03 23:44:32' d FROM dual union  
  SELECT timestamp'2015-04-11 12:34:56' d FROM dual   
)  
SELECT d "Original Date", 
       to_char(d, 'dd-mm-yyyy') "Day-Month-Year",  
       to_char(d, 'hh24:mi') "Time in 24-hr format",  
       to_char(d, 'iw-iyyy') "ISO Year and Week of Year", 
       to_char(d, 'Month') "Month Name", 
       to_char(d, 'Year') "Year"  
FROM dates;