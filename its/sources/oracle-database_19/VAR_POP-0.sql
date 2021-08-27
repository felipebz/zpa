-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/VAR_POP.html
SUM((expr - (SUM(expr) / COUNT(expr)))2) / COUNT(expr)