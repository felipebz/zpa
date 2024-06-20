-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/constraint.html
CREATE TABLE Product(
  Id NUMBER NOT NULL PRIMARY KEY,
  Name VARCHAR2(50),
  Price NUMBER CHECK (mod(price,4) = 0 and 10 <> price) PRECHECK,
  Color NUMBER CHECK (Color >= 10 and Color <=50 and mod(color,2) = 0)
    PRECHECK,
  Description VARCHAR2(50) CHECK (Length(Description) <= 40) PRECHECK,
  Constant NUMBER CHECK (Constant=10) PRECHECK,
  CONSTRAINT TC1 CHECK (Color > 0 AND Price > 10) PRECHECK,
  CONSTRAINT TC2 CHECK (CATEGORY IN ('Home', 'Apparel') AND Price > 10)
);