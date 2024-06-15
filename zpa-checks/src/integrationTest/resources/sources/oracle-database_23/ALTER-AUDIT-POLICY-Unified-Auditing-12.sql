-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-AUDIT-POLICY-Unified-Auditing.html
ALTER AUDIT POLICY emp_updates_pol
  CONDITION 'UID = 102'
  EVALUATE PER STATEMENT;