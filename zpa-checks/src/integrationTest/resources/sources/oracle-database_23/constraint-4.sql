-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/constraint.html
CREATE TABLE locations_demo
    ( location_id    NUMBER(4) 
    , street_address VARCHAR2(40)
    , postal_code    VARCHAR2(12)
    , city           VARCHAR2(30)
    , state_province VARCHAR2(25)
    , country_id     CHAR(2)
    , CONSTRAINT loc_id_pk PRIMARY KEY (location_id));