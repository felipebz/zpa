-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-path-expressions.html
SELECT po.po_document FROM j_purchaseorder po
  WHERE json_exists(po.po_document,                    
                    '$.LineItems.Part?(@.UPCCode == $v1)'
                    PASSING '85391628927' AS "v1");