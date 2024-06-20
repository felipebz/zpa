-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/constraint.html
SQL> CREATE TABLE Product(
       Id NUMBER NOT NULL PRIMARY KEY,
       Name VARCHAR2(50),
       Category VARCHAR2(10) NOT NULL,
       Price NUMBER CHECK (mod(price,4) = 0 and 10 <> price),
       Color NUMBER CHECK (Color >= 10 and Color <=50) PRECHECK,
       Description VARCHAR2(50) CHECK (Length(Description) <= 40),
       Created_At DATE,
       Updated_At DATE,
       CONSTRAINT TC1 CHECK (Color > 0 AND Price > 10),
       CONSTRAINT TC2 CHECK (CATEGORY IN ('Home', 'Apparel')) NOPRECHECK,
       CONSTRAINT TC3 CHECK (Created_At > Updated_At)
     );

Table PRODUCT created.