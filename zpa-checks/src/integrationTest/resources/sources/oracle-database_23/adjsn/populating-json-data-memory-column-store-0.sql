-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/populating-json-data-memory-column-store.html
SELECT COUNT(1) FROM j_purchaseorder
  WHERE json_exists(data,
                    '$.ShippingInstructions?(
                       @.Address.zipCode == 99236)');
ALTER TABLE j_purchaseorder INMEMORY;
SELECT COUNT(1) FROM j_purchaseorder
  WHERE json_exists(data,
                    '$.ShippingInstructions?(
                       @.Address.zipCode == 99236)');