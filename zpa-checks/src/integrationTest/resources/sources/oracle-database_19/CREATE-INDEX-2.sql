-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-INDEX.html
CREATE INDEX cust_eff_ix ON customers
  (NVL(cust_eff_to, TO_DATE('9000-01-01 00:00:00', 'yyyy-mm-dd hh24:mi:ss')));