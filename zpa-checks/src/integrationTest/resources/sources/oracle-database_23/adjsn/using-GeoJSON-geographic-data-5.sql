-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-GeoJSON-geographic-data.html
CREATE INDEX geo_first_feature_idx
  ON j_geo (json_value(geo_doc, '$.features[0].geometry'
                       RETURNING SDO_GEOMETRY))
  INDEXTYPE IS MDSYS.SPATIAL_INDEX;