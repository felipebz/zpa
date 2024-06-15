-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_order.html
CREATE DOMAIN day_of_week AS CHAR(3 CHAR)
  ORDER CASE UPPER(day_of_week)
     WHEN 'MON' THEN 0
     WHEN 'TUE' THEN 1
     WHEN 'WED' THEN 2
     WHEN 'THU' THEN 3
     WHEN 'FRI' THEN 4
     WHEN 'SAT' THEN 5
     WHEN 'SUN' THEN 6
     ELSE 7
  END;