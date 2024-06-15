-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-AUDIT-POLICY-Unified-Auditing.html
SELECT name FROM auditable_system_actions
  WHERE component = 'Standard'
  ORDER BY name;