-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/loading-external-json-data.html
CREATE TABLE json_file_contents (data JSON)
  ORGANIZATION EXTERNAL
    (TYPE ORACLE_BIGDATA
     ACCESS PARAMETERS (com.oracle.bigdata.fileformat = jsondoc)
     LOCATION (order_entry_dir:'PurchaseOrders.json'))
  PARALLEL
  REJECT LIMIT UNLIMITED;