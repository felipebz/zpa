-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-LOCKDOWN-PROFILE.html
ALTER LOCKDOWN PROFILE hr_prof
  DISABLE STATEMENT = ('ALTER SYSTEM')
          CLAUSE = ('SET')
          OPTION = ('PDB_FILE_NAME_CONVERT')
          VALUE = ('cdb1_pdb0', 'cdb1_pdb1');