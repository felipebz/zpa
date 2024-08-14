-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-dataguide.html
EXEC DBMS_JSON.drop_virtual_columns('J_PURCHASEORDER', 'PO_DOCUMENT');
ALTER INDEX po_search_idx REBUILD
  PARAMETERS ('DATAGUIDE ON CHANGE add_vc');
INSERT INTO j_purchaseorder
  VALUES (
    SYS_GUID(),
    to_date('30-JUN-2015'),
    '{"PO_Number"     : 4230,
      "PO_Reference"  : "JDEER-20140421",
      "PO_LineItems"  : [ {"Part_Number"  : 230912362345,
                           "Quantity"     : 3.0} ]}');
DESCRIBE j_purchaseorder;