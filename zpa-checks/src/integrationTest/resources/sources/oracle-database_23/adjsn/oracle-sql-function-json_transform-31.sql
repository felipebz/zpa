-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/oracle-sql-function-json_transform.html
SELECT json_transform(po_document,
                      SORT '$.LineItems'
                        ORDER BY '$.Part.UnitPrice' DESC,
                                 '$.ItemNumber' DESC
                      RETURNING VARCHAR2(4000))
  FROM j_purchaseorder;