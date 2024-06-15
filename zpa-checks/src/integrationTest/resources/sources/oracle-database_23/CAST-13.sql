-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CAST.html
CREATE DOMAIN day_of_week AS VARCHAR2(3 CHAR)
  CONSTRAINT CHECK (day_of_week IN('MON','TUE','WED','THU','FRI','SAT','SUN'))
  DISABLE;