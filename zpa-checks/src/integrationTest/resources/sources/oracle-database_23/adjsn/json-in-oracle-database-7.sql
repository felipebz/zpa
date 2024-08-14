-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-in-oracle-database.html
SELECT json_serialize(po_document PRETTY ORDERED)
  FROM j_purchaseorder po
  WHERE po.po_document.PONumber = 1600;