-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/LOCALTIMESTAMP.html
INSERT INTO local_test
  VALUES (TO_TIMESTAMP(LOCALTIMESTAMP, 'DD-MON-RR HH.MI.SSXFF'));