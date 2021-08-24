ALTER TABLE sales 
    ADD CONSTRAINT sales_pk PRIMARY KEY (prod_id, cust_id) DISABLE;