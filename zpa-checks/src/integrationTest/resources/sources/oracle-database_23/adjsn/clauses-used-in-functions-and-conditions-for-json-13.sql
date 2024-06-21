-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/clauses-used-in-functions-and-conditions-for-json.html
SELECT json_value('{"first":     "Grace",
                    "middle":    "Brewster",
                    "last":      "Hopper",
                    "birthyear": "1906"}',
                  '$'
                  RETURNING person_t
                  ERROR ON MISMATCH (EXTRA DATA))
FROM DUAL;