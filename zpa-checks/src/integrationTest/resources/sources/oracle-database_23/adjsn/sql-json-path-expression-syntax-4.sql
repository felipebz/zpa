-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-path-expression-syntax.html
SELECT po.data FROM j_purchaseorder po
  WHERE json_exists(po.data,                    
                    '$.LineItems.Part?(@.UPCCode == $v1)'
                    PASSING '85391628927' AS "v1");