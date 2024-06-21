-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/indexes-for-json-data.html
CREATE TABLE parts_tab (id NUMBER, jparts JSON);
INSERT INTO parts_tab VALUES
  (1, '{"parts" : [{"partno"   : 3, "subparts" : [510, 580, 520]},
                   {"partno"   : 4, "subparts" : 730}]}');
INSERT INTO parts_tab VALUES
  (2, '{"parts" : [{"partno"   : 7, "subparts" : [410, 420, 410]},
                   {"partno"   : 4, "subparts" : [710, 730, 730]}]}');