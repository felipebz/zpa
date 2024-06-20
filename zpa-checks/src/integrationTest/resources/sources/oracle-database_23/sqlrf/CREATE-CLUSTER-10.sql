-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-CLUSTER.html
CREATE INDEX emp_dept_index 
   ON CLUSTER emp_dept 
   TABLESPACE USERS 
   STORAGE (INITIAL 50K 
      NEXT 50K 
      MINEXTENTS 2 
      MAXEXTENTS 10 
      PCTINCREASE 33);