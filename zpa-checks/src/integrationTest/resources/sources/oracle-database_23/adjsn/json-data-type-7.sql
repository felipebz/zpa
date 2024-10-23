-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-data-type.html
SELECT json_serialize('{"price" : 20, "currency" : "€"}' ASCII ORDERED)
  FROM DUAL;