-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SQL-JSON-Conditions.html
CREATE TABLE jtab (
  id    NUMBER(9) PRIMARY KEY,
  jcol  JSON CHECK(jcol IS JSON VALIDATE CAST USING '{
                  "type": "object",
                  "properties": {
                       "firstName": {
                        "extendedType": "string",
                        "maxLength": 50
                      },
                      "birthDate" : {
                        "extendedType": "date"
                      }
                    },
                    "required": ["firstName", "birthDate"]
                  }'
  )
);