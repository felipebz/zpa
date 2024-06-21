-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/indexes-for-json-data.html
CREATE MULTIVALUE INDEX mvi ON parts_tab t
  (t.jparts.parts.subparts.numberOnly());