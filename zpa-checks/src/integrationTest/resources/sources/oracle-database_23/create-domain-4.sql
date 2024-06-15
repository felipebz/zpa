-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE DOMAIN day_of_week AS CHAR(3 CHAR)
   CONSTRAINT day_of_week_c
     CHECK (UPPER(VALUE) IN ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'))
   DEFERRABLE INITIALLY DEFERRED
   DISPLAY SUBSTR(VALUE, 1, 2);