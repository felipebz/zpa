-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_name.html
CREATE TABLE hr.calendar_dates (
  calendar_date    DATE,
  day_of_week_abbr hr.day_of_week
);