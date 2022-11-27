-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/XML-Conditions.html
SELECT ANY_PATH FROM RESOURCE_VIEW
   WHERE EQUALS_PATH(res, '/sys/schemas/OE/www.example.com')=1;