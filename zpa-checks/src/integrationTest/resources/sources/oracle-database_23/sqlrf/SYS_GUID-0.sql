-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SYS_GUID.html
ALTER TABLE locations ADD (uid_col RAW(16));
UPDATE locations SET uid_col = SYS_GUID();
SELECT location_id, uid_col FROM locations
   ORDER BY location_id, uid_col;