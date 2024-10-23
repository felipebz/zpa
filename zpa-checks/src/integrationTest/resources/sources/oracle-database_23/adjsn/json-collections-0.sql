-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-collections.html
CREATE JSON COLLECTION TABLE employee
  (salary AS (json_value(DATA, '$.salary.number()')),
   CONSTRAINT sal_chk CHECK (salary > 0));