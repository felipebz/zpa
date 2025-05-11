-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-filters-json_exists.html
SELECT po.data FROM j_purchaseorder po
  WHERE json_exists(po.data,
                    '$?(@.User == "ABULL"
                        && exists(@.LineItems[*]?(
                                    @.Part.UPCCode == 85391628927
                                    && @.Quantity > 3)))');