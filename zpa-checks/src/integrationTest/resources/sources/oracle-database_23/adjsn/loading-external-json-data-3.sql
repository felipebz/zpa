-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/loading-external-json-data.html
INSERT INTO j_purchaseorder (id, date_loaded, po_document)
  SELECT SYS_GUID(), SYSTIMESTAMP, data
    FROM json_file_contents;