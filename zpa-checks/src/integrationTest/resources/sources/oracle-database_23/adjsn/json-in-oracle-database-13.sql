-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-in-oracle-database.html
SELECT json_serialize('{"price" : 20, "currency" : "€"}' ASCII ORDERED)
  FROM DUAL;