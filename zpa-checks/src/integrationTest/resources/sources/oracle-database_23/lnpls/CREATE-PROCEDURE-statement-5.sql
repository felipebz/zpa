-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-PROCEDURE-statement.html
CREATE OR REPLACE MLE MODULE hello_mod
LANGUAGE JAVASCRIPT AS 
  export function hello(who){
    console.log(`Hello, ${who}`);
  }
/
CREATE OR REPLACE PROCEDURE hello(
  "p_who" VARCHAR2
)
AS MLE MODULE hello_mod
SIGNATURE 'hello';
/