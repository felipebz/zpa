-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CAST.html
CREATE TYPE address_book_t AS TABLE OF cust_address_typ;
/
CREATE TYPE address_array_t AS VARRAY(3) OF cust_address_typ;
/
CREATE TABLE cust_address (
  custno            NUMBER, 
  street_address    VARCHAR2(40), 
  postal_code       VARCHAR2(10), 
  city              VARCHAR2(30),
  state_province    VARCHAR2(10), 
  country_id        CHAR(2));

CREATE TABLE cust_short (custno NUMBER, name VARCHAR2(31));

CREATE TABLE states (state_id NUMBER, addresses address_array_t);