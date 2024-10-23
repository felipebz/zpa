-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/creating-multivalue-function-based-indexes-json_exists.html
CREATE MULTIVALUE INDEX mvi ON parts_tab t
  (t.jparts.parts.subparts.numberOnly());