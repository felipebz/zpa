-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_TABLE.html
SELECT *
FROM j_purchaseOrder
NESTED po_document.LineItems[*]
COLUMNS(ItemNumber, Quantity NUMBER);