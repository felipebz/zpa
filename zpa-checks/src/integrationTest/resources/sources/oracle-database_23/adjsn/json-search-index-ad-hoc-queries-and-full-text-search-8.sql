-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-search-index-ad-hoc-queries-and-full-text-search.html
SELECT po_document FROM j_purchaseorder
  WHERE json_exists(po_document,
                    '$.ShippingInstructions.Address.country');