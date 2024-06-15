-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_display.html
SELECT day_of_week_abbr, 
       DOMAIN_DISPLAY(day_of_week_abbr) domain_column, 
       DOMAIN_DISPLAY(calendar_date) nondomain_column,
       DOMAIN_DISPLAY(CAST('MON' AS day_of_week)) domain_value, 
       DOMAIN_DISPLAY('MON') nondomain_value
  FROM calendar_dates;