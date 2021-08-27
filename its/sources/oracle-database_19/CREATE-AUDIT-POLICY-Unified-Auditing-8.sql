-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-AUDIT-POLICY-Unified-Auditing.html
SELECT *
  FROM audit_unified_policies
  WHERE policy_name = 'TABLE_POL';