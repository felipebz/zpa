-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/columns-clause-sql-json-function-json_table.html
SELECT jt.ponumb
  FROM j_purchaseorder,
       json_table(po_document, '$'
         COLUMNS (ponumb NUMBER PATH '$.PONumber.numberOnly()')) jt