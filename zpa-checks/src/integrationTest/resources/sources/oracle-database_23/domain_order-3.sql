-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_order.html
SELECT day_of_week_abbr, 
       DOMAIN_ORDER(day_of_week_abbr) domain_column, 
       DOMAIN_ORDER(calendar_date) nondomain_column, 
       DOMAIN_ORDER(CAST('MON' AS day_of_week)) domain_value, 
       DOMAIN_ORDER('MON') nondomain_value
  FROM calendar_dates
  ORDER BY DOMAIN_ORDER(day_of_week_abbr);