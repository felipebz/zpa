-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-function-json_table.html
SELECT jt.*
  FROM j_purchaseorder po,
       json_table(po.po_document, 
         '$'
         COLUMNS (
           "Special Instructions" VARCHAR2(4000)
                                  PATH '$."Special Instructions"',
           NESTED PATH '$.LineItems[*]'
             COLUMNS (
               ItemNumber  NUMBER        PATH '$.ItemNumber',
               Description VARCHAR(4000) PATH '$.Part.Description'))
       ) AS "JT";