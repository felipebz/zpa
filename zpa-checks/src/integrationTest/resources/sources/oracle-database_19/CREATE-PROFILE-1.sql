-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-PROFILE.html
CREATE PROFILE new_profile
  LIMIT PASSWORD_REUSE_MAX 10
        PASSWORD_REUSE_TIME 30;