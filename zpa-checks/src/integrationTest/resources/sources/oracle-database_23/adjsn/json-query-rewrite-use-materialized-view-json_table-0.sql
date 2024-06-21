-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-query-rewrite-use-materialized-view-json_table.html
CREATE MATERIALIZED VIEW mv_for_query_rewrite
  BUILD IMMEDIATE
  REFRESH FAST ON STATEMENT WITH PRIMARY KEY
  AS SELECT po.id, jt.*
       FROM j_purchaseorder po,
            json_table(po.po_document, '$' ERROR ON ERROR NULL ON EMPTY
              COLUMNS (
                po_number       NUMBER         PATH '$.PONumber',
                userid          VARCHAR2(10)   PATH '$.User',
                NESTED PATH '$.LineItems[*]'
                  COLUMNS (
                    itemno      NUMBER         PATH '$.ItemNumber',
                    description VARCHAR2(256)  PATH '$.Part.Description',
                    upc_code    NUMBER         PATH '$.Part.UPCCode',
                    quantity    NUMBER         PATH '$.Quantity',
                    unitprice   NUMBER         PATH '$.Part.UnitPrice'))) jt;