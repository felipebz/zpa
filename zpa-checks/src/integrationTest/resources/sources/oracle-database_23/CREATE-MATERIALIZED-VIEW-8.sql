-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-MATERIALIZED-VIEW.html
CREATE MATERIALIZED VIEW all_customers
   PCTFREE 5 PCTUSED 60 
   TABLESPACE example 
   STORAGE (INITIAL 50K) 
   USING INDEX STORAGE (INITIAL 25K)
   REFRESH START WITH ROUND(SYSDATE + 1) + 11/24 
   NEXT NEXT_DAY(TRUNC(SYSDATE), 'MONDAY') + 15/24 
   AS SELECT * FROM sh.customers@remote 
         UNION
      SELECT * FROM sh.customers@local;