-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-MATERIALIZED-VIEW-LOG.html
ALTER MATERIALIZED VIEW LOG ON employees
   ADD (commission_pct)
   EXCLUDING NEW VALUES;