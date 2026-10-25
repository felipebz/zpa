create package pkg is
  cursor cur is -- Noncompliant {{Move the body of the cursor "CUR" to the package body.}}
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
create package pkg is
  cursor cur(p number) is -- Noncompliant {{Move the body of the cursor "CUR" to the package body.}}
    select dummy from dual where p = 1;
  cursor cur(p varchar2) is -- Noncompliant {{Move the body of the cursor "CUR" to the package body.}}
    select dummy from dual where p = 'x';
end;
/
create package pkg is
  $if $$with_cursor $then
  cursor kept is -- Noncompliant {{Move the body of the cursor "KEPT" to the package body.}}
    select dummy from dual;
  $else
  cursor skipped is
    select dummy from dual;
  $end
end;
/
