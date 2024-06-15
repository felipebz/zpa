-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE DOMAIN dj5 AS JSON CONSTRAINT dj5chk
    CHECK (dj5 IS JSON validate
          '{
           "type": "object",
           "properties": {
             "a": {
               "type": "number"
              }
           }
         }'
    );