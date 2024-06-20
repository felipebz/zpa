-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ceil-datetime.html
SELECT CEIL(TO_DATE ('28-FEB-2023','DD-MON-YYYY'), 'MM') AS month_ceiling;