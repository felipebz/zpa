-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-json_value-function-based-index-json_exists-queries.html
SELECT count(*) FROM j_purchaseorder
  WHERE json_exists(po_document,
                    '$?(@.PONumber > 1500
                        && @.Reference == "ABULL-20140421")');