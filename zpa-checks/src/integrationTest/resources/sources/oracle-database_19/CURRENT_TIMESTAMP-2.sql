-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CURRENT_TIMESTAMP.html
INSERT INTO current_test VALUES
  (TO_TIMESTAMP_TZ(CURRENT_TIMESTAMP, 'DD-MON-RR HH.MI.SSXFF PM'));