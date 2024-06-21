-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/function-JSON_TABLE.html
SELECT jt.*
  FROM j_purchaseorder,
       json_table(po_document, '$'
         COLUMNS (requestor VARCHAR2(32 CHAR) PATH '$.Requestor',
                  ph_arr    VARCHAR2(100 CHAR) FORMAT JSON
                            PATH '$.ShippingInstructions.Phone')
                 ) AS "JT";