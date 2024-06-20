-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_SERIALIZE.html
SELECT JSON_SERIALIZE('{price:20, currency:" €"}' ASCII PRETTY ORDERED) from dual;