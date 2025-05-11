-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-filters-json_exists.html
SELECT po.data FROM j_purchaseorder po
  WHERE json_exists(po.data,
                    '$?(@.LineItems.Part.UPCCode == 85391628927)');
SELECT po.data FROM j_purchaseorder po
  WHERE json_exists(po.data,
                    '$.LineItems?(@.Part.UPCCode == 85391628927)');
SELECT po.data FROM j_purchaseorder po
  WHERE json_exists(po.data,
                    '$.LineItems.Part?(@.UPCCode == 85391628927)');