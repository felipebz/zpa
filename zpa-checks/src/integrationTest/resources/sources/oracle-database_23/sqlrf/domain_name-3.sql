-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_name.html
SELECT day_of_week_abbr, 
       DOMAIN_NAME(day_of_week_abbr) domain_column, 
       DOMAIN_NAME(calendar_date) nondomain_column, 
       DOMAIN_NAME(CAST('MON' AS hr.day_of_week)) domain_value,
       DOMAIN_NAME('MON') nondomain_value
  FROM hr.calendar_dates;