-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
CREATE INDEX cost_ix ON costs(channel_id) LOCAL;
ALTER TABLE costs
  SPLIT PARTITION costs_q4_2003 at
    (TO_DATE('01-Nov-2003','dd-mon-yyyy')) 
    INTO (PARTITION c_p1, PARTITION c_p2) 
  UPDATE INDEXES (cost_ix (PARTITION c_p1 tablespace tbs_02, 
                           PARTITION c_p2 tablespace tbs_03));