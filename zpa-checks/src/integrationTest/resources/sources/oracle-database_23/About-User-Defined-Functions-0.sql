-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/About-User-Defined-Functions.html
CREATE TABLE new_emps (new_sal NUMBER, ...);
CREATE FUNCTION new_sal RETURN NUMBER IS BEGIN ... END;