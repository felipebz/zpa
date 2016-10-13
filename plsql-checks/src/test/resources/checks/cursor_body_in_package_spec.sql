create package pkg is
  cursor cur is -- Noncompliant {{Move the body of the cursor "cur" to the package body.}}
    select dummy from dual;
end;
/
create package pkg is
  type cur_type is record(dummy varchar2(1));
  cursor cur return cur_type; -- Compliant
end;
/
create package body pkg is
  cursor cur is -- Compliant
    select dummy from dual;
end;
/