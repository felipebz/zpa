-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/indexes-for-json-data.html
SELECT count(*) FROM j_purchaseorder 
  WHERE json_exists(po_document, '$.PONumber?(@ > $d)'
                    PASSING 1500 AS "d");