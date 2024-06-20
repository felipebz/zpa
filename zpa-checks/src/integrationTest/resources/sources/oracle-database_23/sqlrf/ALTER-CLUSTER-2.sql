-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-CLUSTER.html
CREATE CLUSTER EMP_DEPT (DEPTNO NUMBER(3))   
   SIZE 600   
   TABLESPACE USERS   
   STORAGE (INITIAL 200K   
      NEXT 300K   
      MINEXTENTS 2   
      PCTINCREASE 33);