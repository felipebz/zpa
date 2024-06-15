-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-AUDIT-POLICY-Unified-Auditing.html
ALTER AUDIT POLICY java_pol
  ADD ACTIONS CREATE JAVA, ALTER JAVA, DROP JAVA;