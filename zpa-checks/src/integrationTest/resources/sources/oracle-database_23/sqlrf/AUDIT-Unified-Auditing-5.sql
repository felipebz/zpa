-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/AUDIT-Unified-Auditing.html
SELECT policy_name, enabled_option, entity_name
  FROM audit_unified_enabled_policies
  WHERE policy_name = 'READ_DIR_POL';