-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_check_type.html
CREATE DOMAIN dgreater AS (
  c1 AS NUMBER, c2 AS NUMBER 
) 
  CHECK (c1 > c2);