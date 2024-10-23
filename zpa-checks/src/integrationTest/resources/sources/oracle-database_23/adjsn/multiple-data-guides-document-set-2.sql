-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/multiple-data-guides-document-set.html
SELECT extract(YEAR FROM date_loaded), json_dataguide(po_document)
  FROM j_purchaseorder
  GROUP BY extract(YEAR FROM date_loaded)
  ORDER BY extract(YEAR FROM date_loaded) DESC;