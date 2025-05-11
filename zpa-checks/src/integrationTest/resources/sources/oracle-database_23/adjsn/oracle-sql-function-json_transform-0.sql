-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/oracle-sql-function-json_transform.html
UPDATE j_purchaseorder po
  SET data = json_transform(data,
                            SET '$.costCenter' = 'Z20')
  WHERE po.data.User = 'ABULL';