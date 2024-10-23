-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/oracle-sql-condition-json_textcontains.html
SELECT po_document FROM j_purchaseorder
  WHERE json_textcontains(po_document,
                          '$.LineItems.Part.Description',
                          'Magic');