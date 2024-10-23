-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-FUNCTION-statement.html
CREATE OR REPLACE MLE MODULE hello_mod
LANGUAGE JAVASCRIPT AS 
  export function hello(who){
    return `Hello, ${who}`;
  }
/
CREATE OR REPLACE FUNCTION hello(
  "p_who" VARCHAR2
) RETURN VARCHAR2
AS MLE MODULE hello_mod
SIGNATURE 'hello';
/