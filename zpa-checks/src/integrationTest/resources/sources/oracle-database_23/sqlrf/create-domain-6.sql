-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE DOMAIN day_of_week AS CHAR(3 CHAR)
   CONSTRAINT day_of_week_c
     CHECK (day_of_week IN ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'))   
   INITIALLY DEFERRED
   ORDER CASE day_of_week
      WHEN 'MON' THEN 0
      WHEN 'TUE' THEN 1
      WHEN 'WED' THEN 2
      WHEN 'THU' THEN 3
      WHEN 'FRI' THEN 4
      WHEN 'SAT' THEN 5
      WHEN 'SUN' THEN 6
      ELSE 7
  END;