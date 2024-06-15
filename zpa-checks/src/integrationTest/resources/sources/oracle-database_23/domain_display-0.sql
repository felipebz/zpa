-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_display.html
CREATE DOMAIN day_of_week AS CHAR(3 CHAR)
  DISPLAY INITCAP(day_of_week);