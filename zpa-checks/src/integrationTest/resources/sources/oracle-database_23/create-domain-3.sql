-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CASE 
  WHEN UPPER(DOMAIN_DISPLAY(day_of_week)) IN ('SAT','SUN')
  THEN 'weekend' 
  ELSE 'week day' 
END