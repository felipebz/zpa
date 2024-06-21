-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-GeoJSON-geographic-data.html
SELECT jt.*
  FROM j_geo,
       json_table(geo_doc, '$.features[*]'
         COLUMNS (sdo_val SDO_GEOMETRY PATH '$.geometry')) jt;