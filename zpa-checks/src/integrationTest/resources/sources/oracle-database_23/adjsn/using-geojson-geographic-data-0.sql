-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-geojson-geographic-data.html
CREATE TABLE j_geo
  (id      VARCHAR2 (32) NOT NULL,
   geo_doc VARCHAR2 (4000) CHECK (geo_doc is json));
INSERT INTO j_geo
  VALUES (1,
          '{"type"     : "FeatureCollection",
            "features" : [{"type"       : "Feature",
                           "geometry"   : {"type" : "Point",
                                           "coordinates" : [-122.236111, 37.482778]},
                           "properties" : {"Name" : "Redwood City"}},
                          {"type"       : "Feature",
                           "geometry"   : {"type" : "LineString",
                                           "coordinates" : [[102.0, 0.0],
                                                            [103.0, 1.0],
                                                            [104.0, 0.0],
                                                            [105.0, 1.0]]},
                           "properties" : {"prop0" : "value0",
                                           "prop1" : 0.0}},
                          {"type"       : "Feature",
                           "geometry"   : {"type" : "Polygon",
                                           "coordinates" : [[[100.0, 0.0],
                                                             [101.0, 0.0],
                                                             [101.0, 1.0],
                                                             [100.0, 1.0],
                                                             [100.0, 0.0]]]},
                           "properties" : {"prop0" : "value0",
                                           "prop1" : {"this" : "that"}}}]}');