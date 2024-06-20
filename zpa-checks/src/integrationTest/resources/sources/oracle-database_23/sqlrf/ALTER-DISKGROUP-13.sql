-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-DISKGROUP.html
ALTER DISKGROUP dgroup_01
  ADD ALIAS '+dgroup_01/alias_dir/datafile.dbf'
    FOR '+dgroup_01.261.1';