-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/constraint.html
ALTER TABLE sales 
    ADD CONSTRAINT sales_pk PRIMARY KEY (prod_id, cust_id) DISABLE;