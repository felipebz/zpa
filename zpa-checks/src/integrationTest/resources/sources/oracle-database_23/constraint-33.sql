-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/constraint.html
CREATE TABLE games
  (scores NUMBER, CONSTRAINT unq_num UNIQUE (scores)
   INITIALLY DEFERRED DEFERRABLE);