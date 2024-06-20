-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Syntax-for-Schema-Objects-and-Parts-in-SQL-Statements.html
CREATE TYPE cust_address_typ
  OID '82A4AF6A4CD1656DE034080020E0EE3D'
  AS OBJECT
    (street_address    VARCHAR2(40),
     postal_code       VARCHAR2(10),
     city              VARCHAR2(30),
     state_province    VARCHAR2(10),
     country_id        CHAR(2));
/