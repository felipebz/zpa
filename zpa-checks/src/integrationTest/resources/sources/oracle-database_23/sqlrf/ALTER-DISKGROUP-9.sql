-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-DISKGROUP.html
ALTER DISKGROUP dgroup_01
  ADD TEMPLATE template_01
    ATTRIBUTES (UNPROTECTED COARSE);