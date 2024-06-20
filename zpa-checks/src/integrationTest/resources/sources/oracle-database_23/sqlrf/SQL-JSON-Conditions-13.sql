-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SQL-JSON-Conditions.html
SELECT COUNT(1) FROM jsontab1 WHERE j IS JSON 
VALIDATE
         '{"type" : "object",
              "properties" : {
                  "id" : {
                      "type" : "number"
                   }
               }
           }';