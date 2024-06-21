-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-schema.html
SELECT po_document
  FROM j_purchaseorder
  WHERE po_document IS JSON VALIDATE
    '{"type"       : "object",
      "properties" : {"PONumber": {"type"    : "number",
                                   "minimum" : 0}}}'