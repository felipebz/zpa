-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Multiset-Operators.html
CREATE TYPE cust_address_tab_typ AS
  TABLE OF cust_address_typ;
/