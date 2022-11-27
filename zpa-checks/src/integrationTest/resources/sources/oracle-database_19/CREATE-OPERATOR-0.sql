-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-OPERATOR.html
CREATE FUNCTION eq_f(a VARCHAR2, b VARCHAR2) RETURN NUMBER AS
BEGIN
   IF a = b THEN RETURN 1;
   ELSE RETURN 0;
   END IF;
END;
/

CREATE OPERATOR eq_op
   BINDING (VARCHAR2, VARCHAR2) 
   RETURN NUMBER 
   USING eq_f;