-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/floor-datetime.html
SELECT FLOOR(TO_TIMESTAMP ('28-FEB-2023 14:10:10','DD-MON-YYYY HH24:MI:SS'),'HH24') AS hour_floor;