-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/subprograms-invoked-triggers.html
CREATE OR REPLACE PROCEDURE Before_delete (Id IN NUMBER, Ename VARCHAR2)
IS LANGUAGE Java
name 'thjvTriggers.beforeDelete (oracle.jdbc.NUMBER, oracle.jdbc.CHAR)';

CREATE OR REPLACE TRIGGER Pre_del_trigger BEFORE DELETE ON Tab 
FOR EACH ROW
CALL Before_delete (:OLD.Id, :OLD.Ename)
/