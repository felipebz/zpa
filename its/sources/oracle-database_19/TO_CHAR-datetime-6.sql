WITH dates AS (   
  SELECT date'2015-01-01' d FROM dual union   
  SELECT date'2015-01-10' d FROM dual union   
  SELECT date'2015-02-01' d FROM dual union   
  SELECT timestamp'2015-03-03 23:44:32' d FROM dual union   
  SELECT timestamp'2015-04-11 12:34:56' d FROM dual    
)   
SELECT extract(minute from d) minutes,  
       extract(hour from d) hours,  
       extract(day from d) days,  
       extract(month from d) months,  
       extract(year from d) years  
FROM dates;