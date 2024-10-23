-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-PROCEDURE-statement.html
CREATE OR REPLACE PROCEDURE hello_inline(
  "who" VARCHAR2
)
AS MLE LANGUAGE JAVASCRIPT
{{
  console.log(`Hello, ${who}`);
}};
/