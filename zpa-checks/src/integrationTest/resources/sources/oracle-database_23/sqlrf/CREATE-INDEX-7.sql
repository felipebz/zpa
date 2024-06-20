-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-INDEX.html
CREATE INDEX ord_customer_ix_demo 
   ON orders (customer_id, sales_rep_id)
   COMPRESS 1;