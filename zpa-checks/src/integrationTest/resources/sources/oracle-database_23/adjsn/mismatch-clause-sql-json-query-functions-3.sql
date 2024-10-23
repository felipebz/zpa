-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/mismatch-clause-sql-json-query-functions.html
SELECT json_value('{"first":     "Grace",
                    "middle":    "Brewster",
                    "last":      "Hopper",
                    "birthyear": "1906"}',
                  '$'
                  RETURNING person_t
                  ERROR ON MISMATCH (EXTRA DATA)
                  ERROR ON MISMATCH (MISSING DATA)
                  IGNORE ON MISMATCH (TYPE ERROR))
FROM DUAL;