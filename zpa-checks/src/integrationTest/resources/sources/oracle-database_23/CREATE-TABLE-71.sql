-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
CREATE TABLE tm1 (c1 NUMBER, c2 NUMBER, c3 VARCHAR2(15),c4 NUMBER, c5 NUMBER, 
                  c6 NUMBER, c7 NUMBER, DOMAIN dm1 (c1, c2, c3, c4),
                  DOMAIN dn2(c5, c6), DOMAIN dn1(c7));