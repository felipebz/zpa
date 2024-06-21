-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/in-memory-json-data.html
SELECT COUNT(1) FROM j_purchaseorder
  WHERE json_exists(po_document,
                    '$.ShippingInstructions?(
                       @.Address.zipCode == 99236)');
ALTER TABLE j_purchaseorder INMEMORY;
SELECT COUNT(1) FROM j_purchaseorder
  WHERE json_exists(po_document,
                    '$.ShippingInstructions?(
                       @.Address.zipCode == 99236)');