-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/simple-dot-notation-access-json-data.html
SELECT po.data.PONumber FROM j_purchaseorder po;
SELECT json_value(data, '$.PONumber') FROM j_purchaseorder;