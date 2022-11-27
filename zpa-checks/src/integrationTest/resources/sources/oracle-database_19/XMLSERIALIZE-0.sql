-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/XMLSERIALIZE.html
SELECT XMLSERIALIZE(CONTENT XMLTYPE('<Owner>Grandco</Owner>')) AS xmlserialize_doc
   FROM DUAL;