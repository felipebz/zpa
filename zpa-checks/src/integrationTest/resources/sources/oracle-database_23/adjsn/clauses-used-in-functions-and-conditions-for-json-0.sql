-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/clauses-used-in-functions-and-conditions-for-json.html
SELECT count(*) FROM j_purchaseorder
WHERE json_exists(po_document, '$.PONumber?(@.numberOnly() > $d)'
PASSING to_number(:1) AS "d");