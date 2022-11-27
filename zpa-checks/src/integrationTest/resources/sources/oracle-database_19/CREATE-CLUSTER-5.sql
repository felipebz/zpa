-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-CLUSTER.html
CREATE CLUSTER cust_orders (customer_id NUMBER(6))
   SIZE 512 SINGLE TABLE HASHKEYS 100;