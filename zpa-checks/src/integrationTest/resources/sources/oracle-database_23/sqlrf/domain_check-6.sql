-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_check.html
CREATE DOMAIN day_of_week AS CHAR(3 CHAR);
CREATE TABLE calendar_dates (
  calendar_date    DATE,
  day_of_week_abbr day_of_week
);
INSERT INTO calendar_dates 
VALUES(DATE'2023-05-01', 'MON'), 
      (DATE'2023-05-02', 'tue'), 
      (DATE'2023-05-05', 'fRI');
SELECT day_of_week_abbr, 
       DOMAIN_CHECK(day_of_week, day_of_week_abbr) domain_column, 
       DOMAIN_CHECK(day_of_week, calendar_date) nondomain_column, 
       DOMAIN_CHECK(day_of_week, CAST('MON' AS day_of_week)) domain_value, 
       DOMAIN_CHECK(day_of_week, 'mon') nondomain_value
  FROM calendar_dates;