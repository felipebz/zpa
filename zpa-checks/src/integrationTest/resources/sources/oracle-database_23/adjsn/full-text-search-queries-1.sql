-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/full-text-search-queries.html
SELECT po.po_document.PONumber, SCORE(1)
  FROM  j_purchaseorder po
  WHERE json_textcontains (po.po_document,
                           '$.LineItems.Part.Description',
                           'run',
                           1)
  ORDER BY SCORE(1) DESC;