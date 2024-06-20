-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CURSOR-Expressions.html
CREATE FUNCTION f(cur SYS_REFCURSOR, mgr_hiredate DATE) 
   RETURN NUMBER IS
   emp_hiredate DATE;
   before number :=0;
   after number:=0;
begin
  loop
    fetch cur into emp_hiredate;
    exit when cur%NOTFOUND;
    if emp_hiredate > mgr_hiredate then
      after:=after+1;
    else
      before:=before+1;
    end if;
  end loop;
  close cur;
  if before > after then
    return 1;
  else
    return 0;
  end if;
end;
/