-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-AUDIT-POLICY-Unified-Auditing.html
ALTER AUDIT POLICY security_pol
  ADD PRIVILEGES CREATE ANY LIBRARY, DROP ANY LIBRARY
      ACTIONS DELETE on hr.employees,
              INSERT on hr.employees,
              UPDATE on hr.employees,
              ALL on hr.departments
      ROLES dba, connect;