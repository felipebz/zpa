-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-data-type.html
WITH jtab AS
  (SELECT JSON(
     '{ "name" : "Alexis Bull",
        "Address": { "street" : "200 Sporting Green",
                     "city" : "South San Francisco",
                     "state" : "CA",
                     "zipCode" : 99236,
                     "country" : "United States of America" } }')
     AS jcol FROM DUAL)
  SELECT j.jcol.Address.city FROM jtab j;