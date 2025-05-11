-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-data-type.html
SELECT json_serialize(data PRETTY ORDERED)
  FROM j_purchaseorder po
  WHERE po.data.PONumber = 1600;