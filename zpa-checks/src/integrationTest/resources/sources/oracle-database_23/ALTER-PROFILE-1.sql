-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-PROFILE.html
ALTER PROFILE app_user 
   LIMIT PASSWORD_REUSE_TIME DEFAULT
   PASSWORD_REUSE_MAX UNLIMITED;