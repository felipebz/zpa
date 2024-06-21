-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-dataguide.html
INSERT INTO j_purchaseorder
  VALUES (
    SYS_GUID(),
    to_date('30-MAR-2016'),
    '{"PO_ID"     : 4230,
      "PO_Ref"  : "JDEER-20140421",
      "PO_Items"  : [ {"Part_No"       : 98981327234,
                      "Item_Quantity" : 13} ]}');