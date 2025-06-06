-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/populate-json-type-column-querying-textual-json.html
INSERT /*+ PARALLEL APPEND */
  INTO j_purchaseorder_new (id, date_loaded, data)
  SELECT id, date_loaded, JSON(data)
    FROM j_purchaseorder;