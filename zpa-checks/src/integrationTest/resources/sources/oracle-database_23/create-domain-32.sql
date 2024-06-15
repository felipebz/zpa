-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE TABLE orders ( 
     id      NUMBER,
     cust    VARCHAR2(100),
     status  ORDER_STATUS
);