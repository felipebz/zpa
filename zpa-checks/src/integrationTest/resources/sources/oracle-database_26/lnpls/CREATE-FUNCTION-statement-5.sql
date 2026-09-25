-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/CREATE-FUNCTION-statement.html
CREATE OR REPLACE FUNCTION hello_inline(
  "who" VARCHAR2
) RETURN VARCHAR2
AS MLE LANGUAGE JAVASCRIPT
{{
  return `Hello, ${who}`;
}};
/