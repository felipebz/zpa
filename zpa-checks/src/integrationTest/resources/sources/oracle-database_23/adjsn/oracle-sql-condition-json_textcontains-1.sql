-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/oracle-sql-condition-json_textcontains.html
SELECT po.data.PONumber, SCORE(1)
  FROM  j_purchaseorder po
  WHERE json_textcontains (po.data,
                           '$.LineItems.Part.Description',
                           'run',
                           1)
  ORDER BY SCORE(1) DESC;