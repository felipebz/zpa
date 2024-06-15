-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/NOAUDIT-Unified-Auditing.html
SELECT *
  FROM audit_unified_enabled_policies
  WHERE policy_name = 'TABLE_POL';