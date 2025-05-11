-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-sort.html
SELECT json_transform(data,
                      SORT '$.LineItems'
                        ORDER BY '$.Part.UnitPrice' DESC,
                                 '$.ItemNumber' DESC
                      RETURNING VARCHAR2(4000))
  FROM j_purchaseorder;