-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-schema.html
SELECT data
  FROM j_purchaseorder
  WHERE data IS JSON VALIDATE
    '{"type"       : "object",
      "properties" : {"PONumber": {"type"    : "number",
                                   "minimum" : 0}}}'