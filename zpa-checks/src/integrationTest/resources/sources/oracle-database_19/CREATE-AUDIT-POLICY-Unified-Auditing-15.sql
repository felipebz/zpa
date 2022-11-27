-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-AUDIT-POLICY-Unified-Auditing.html
SELECT name FROM auditable_system_actions
  WHERE component = 'Datapump';