-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SYS_TYPEID.html
SELECT name, SYS_TYPEID(VALUE(p)) "Type_id" FROM persons p;