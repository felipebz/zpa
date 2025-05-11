-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/oracle-sql-function-json_transform.html
SELECT json_transform(data,
                      REMOVE '$."Special Instructions"'
                      RETURNING CLOB PRETTY)
  FROM j_purchaseorder;