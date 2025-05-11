-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/oracle-sql-condition-json_textcontains.html
SELECT data FROM j_purchaseorder
  WHERE json_textcontains(data,
                          '$.LineItems.Part.Description',
                          'Magic');