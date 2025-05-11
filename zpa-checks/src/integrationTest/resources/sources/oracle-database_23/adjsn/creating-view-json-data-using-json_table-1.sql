-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/creating-view-json-data-using-json_table.html
CREATE MATERIALIZED VIEW j_purchaseorder_materialized_view
  BUILD IMMEDIATE
  REFRESH FAST ON STATEMENT WITH PRIMARY KEY
  AS SELECT po.id, jt.*
       FROM j_purchaseorder po,
            json_table(po.data, '$'
              COLUMNS (
                po_number        NUMBER(10)         PATH '$.PONumber',
                reference        VARCHAR2(30 CHAR)  PATH '$.Reference',
                requestor        VARCHAR2(128 CHAR) PATH '$.Requestor',
                userid           VARCHAR2(10 CHAR)  PATH '$.User',
                costcenter       VARCHAR2(16)       PATH '$.CostCenter',
                ship_to_name     VARCHAR2(20 CHAR)
                                 PATH '$.ShippingInstructions.name',
                ship_to_street   VARCHAR2(32 CHAR)
                                 PATH '$.ShippingInstructions.Address.street',
                ship_to_city     VARCHAR2(32 CHAR)
                                 PATH '$.ShippingInstructions.Address.city',
                ship_to_county   VARCHAR2(32 CHAR)
                                 PATH '$.ShippingInstructions.Address.county',
                ship_to_postcode VARCHAR2(10 CHAR)
                                 PATH '$.ShippingInstructions.Address.postcode',
                ship_to_state    VARCHAR2(2 CHAR)
                                 PATH '$.ShippingInstructions.Address.state',
                ship_to_zip      VARCHAR2(8 CHAR)
                                 PATH '$.ShippingInstructions.Address.zipCode',
                ship_to_country  VARCHAR2(32 CHAR)
                                 PATH '$.ShippingInstructions.Address.country',
                ship_to_phone    VARCHAR2(24 CHAR)
                                 PATH '$.ShippingInstructions.Phone[0].number',
                NESTED PATH '$.LineItems[*]'
                  COLUMNS (
                    itemno      NUMBER(38)         PATH '$.ItemNumber', 
                    description VARCHAR2(256 CHAR) PATH '$.Part.Description', 
                    upc_code    NUMBER             PATH '$.Part.UPCCode', 
                    quantity    NUMBER(12,4)       PATH '$.Quantity', 
                    unitprice   NUMBER(14,2)       PATH '$.Part.UnitPrice'))) jt;