-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-domain.html
CREATE TABLE jtab(
      id   NUMBER PRIMARY KEY,
     jcol JSON DOMAIN dj5
    );