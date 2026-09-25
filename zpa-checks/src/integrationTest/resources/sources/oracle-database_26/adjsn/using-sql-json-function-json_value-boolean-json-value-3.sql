-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/using-sql-json-function-json_value-boolean-json-value.html
SELECT json_value(data, '$.AllowPartialShipment'
                  RETURNING NUMBER
                  ALLOW BOOLEAN TO NUMBER CONVERSION)
  FROM j_purchaseorder;