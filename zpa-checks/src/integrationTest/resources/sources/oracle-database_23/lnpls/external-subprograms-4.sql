-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/external-subprograms.html
CREATE OR REPLACE FUNCTION js_raise_sal(
  p_empno NUMBER, 
  p_percent NUMBER
) RETURN NUMBER 
AS MLE MODULE js_adjuster
SIGNATURE 'raiseSal';
/