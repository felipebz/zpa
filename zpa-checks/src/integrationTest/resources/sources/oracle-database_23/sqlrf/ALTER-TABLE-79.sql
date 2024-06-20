-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
ALTER TABLE JOBS_TEMP MOVE    
      STORAGE ( INITIAL 20K    
                NEXT 40K    
                MINEXTENTS 2    
                MAXEXTENTS 20    
                PCTINCREASE 0 )    
      TABLESPACE USERS;