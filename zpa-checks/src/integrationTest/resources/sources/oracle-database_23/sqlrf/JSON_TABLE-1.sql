-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_TABLE.html
SELECT t.*
FROM j_purchaseOrder LEFT OUTER JOIN
JSON_TABLE(po_document COLUMNS(PONumber, Reference, Requestor)) t ON 1=1;