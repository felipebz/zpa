-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-GeoJSON-geographic-data.html
CREATE INDEX geo_all_features_idx ON geo_doc_view(sdo_val)
  INDEXTYPE IS MDSYS.SPATIAL_INDEX V2;