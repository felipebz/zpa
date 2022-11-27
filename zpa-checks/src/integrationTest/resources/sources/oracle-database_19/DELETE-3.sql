-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/DELETE.html
DELETE FROM hr.locations@remote
   WHERE location_id > 3000;