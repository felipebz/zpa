-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-filters-json_exists.html
SELECT po.po_document FROM j_purchaseorder po
  WHERE json_exists(po.po_document,
                    '$?(@.User == "ABULL"
                        && exists(@.LineItems[*]?(
                                    @.Part.UPCCode == 85391628927
                                    && @.Quantity > 3)))');