-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/creating-multivalue-function-based-indexes-json_exists.html
CREATE MULTIVALUE INDEX cmvi_1 ON parts_tab
  (json_table(jparts, '$.parts[*]'
     ERROR ON ERROR NULL ON EMPTY NULL ON MISMATCH
     COLUMNS (partNum NUMBER(10) PATH '$.partno',
       NESTED PATH '$.subparts[*]'
         COLUMNS (subpartNum NUMBER(20) PATH '$'))));