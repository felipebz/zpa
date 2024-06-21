-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-GeoJSON-geographic-data.html
SELECT json_value(geo_doc, '$.features[0].geometry'
                  RETURNING SDO_GEOMETRY 
                  ERROR ON ERROR)
  FROM j_geo;