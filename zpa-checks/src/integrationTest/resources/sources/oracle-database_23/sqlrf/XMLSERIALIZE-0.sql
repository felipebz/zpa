-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/XMLSERIALIZE.html
SELECT XMLSERIALIZE(CONTENT XMLTYPE('<Owner>Grandco</Owner>')) AS xmlserialize_doc
   FROM DUAL;