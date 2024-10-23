-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-function-json_arrayagg.html
SELECT json_object(
         'region'    : region_name,
         'countries' :
         (SELECT json_arrayagg(json_object('id'   : country_id,
                                           'name' : country_name))
            FROM countries c
            WHERE c.region_id = r.region_id))
  FROM regions r;