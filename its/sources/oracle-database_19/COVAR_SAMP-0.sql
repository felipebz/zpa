-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/COVAR_SAMP.html
(SUM(expr1 * expr2) - SUM(expr1) * SUM(expr2) / n) / (n-1)