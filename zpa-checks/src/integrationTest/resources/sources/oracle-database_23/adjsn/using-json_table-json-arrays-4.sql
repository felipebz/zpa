-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-json_table-json-arrays.html
SELECT jt.*
  FROM j_purchaseorder po,
       json_table(po.po_document, '$'
         COLUMNS (Requestor VARCHAR2(4000) PATH '$.Requestor',
                  NESTED
                    PATH '$.ShippingInstructions.Phone[*]'
                    COLUMNS (type     VARCHAR2(4000) PATH '$.type',
                             "number" VARCHAR2(4000) PATH '$.number'))
       ) AS "JT";