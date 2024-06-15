-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
CREATE TABLE product(
  id NUMBER NOT NULL PRIMARY KEY,
  name VARCHAR2(50),
  price NUMBER CHECK (mod(price,4) = 0 and 10 <> price) PRECHECK,
  color NUMBER CHECK (color >= 10 and color <=50 and mod(color,2) = 0)
    PRECHECK,
  description VARCHAR2(50) CHECK (length(description) <= 40) PRECHECK,
  constant NUMBER CHECK (constant=10) PRECHECK,
  CONSTRAINT TC1 CHECK (color > 0 AND price > 10) PRECHECK,
  CONSTRAINT TC2 CHECK (CATEGORY IN ('home', 'apparel') AND price > 10)
);