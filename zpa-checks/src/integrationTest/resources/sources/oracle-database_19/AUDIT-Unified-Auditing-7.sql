-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/AUDIT-Unified-Auditing.html
SELECT policy_name, enabled_option, entity_name, success, failure
  FROM audit_unified_enabled_policies
  WHERE policy_name = 'SECURITY_POL';