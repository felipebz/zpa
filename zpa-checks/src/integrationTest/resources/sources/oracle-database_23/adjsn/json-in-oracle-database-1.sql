-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-in-oracle-database.html
INSERT INTO j_purchaseorder
  VALUES (SYS_GUID(),
          to_date('30-DEC-2014'),
          '{"PONumber"             : 1600,
            "Reference"            : "ABULL-20140421",
            "Requestor"            : "Alexis Bull",
            "User"                 : "ABULL",
            "CostCenter"           : "A50",
            "ShippingInstructions" : {...},
            "Special Instructions" : null,
            "AllowPartialShipment" : true,
            "LineItems"            : [...]}');