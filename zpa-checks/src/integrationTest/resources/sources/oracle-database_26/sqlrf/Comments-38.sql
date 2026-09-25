-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Comments.html
SELECT /*+NO_XML_QUERY_REWRITE*/ XMLQUERY('<A/>' RETURNING CONTENT)
  FROM DUAL;