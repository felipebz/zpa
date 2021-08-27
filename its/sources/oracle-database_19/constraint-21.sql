-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/constraint.html
CREATE TABLE customer_addresses (
   add_id NUMBER,
   address REF cust_address_typ REFERENCES address_table);