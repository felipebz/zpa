-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-AUDIT-POLICY-Unified-Auditing.html
ALTER AUDIT POLICY dml_pol
  DROP ACTIONS INSERT on hr.employees,
               UPDATE on hr.employees;