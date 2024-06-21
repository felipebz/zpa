-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/function-JSON_TABLE.html
SELECT jt.*
  FROM j_purchaseorder po,
       json_table(po.po_document
         COLUMNS ("Special Instructions",
                  NESTED LineItems[*]
                    COLUMNS (ItemNumber NUMBER,
                             Description PATH Part.Description))
       ) AS "JT";