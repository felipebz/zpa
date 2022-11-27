-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-INDEX.html
CREATE BITMAP INDEX typeid_i ON books (SYS_TYPEID(author));