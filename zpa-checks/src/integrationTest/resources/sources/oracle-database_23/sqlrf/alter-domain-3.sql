-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/alter-domain.html
ALTER DOMAIN year_of_birth
      MODIFY ORDER MOD(year_of_birth,100);