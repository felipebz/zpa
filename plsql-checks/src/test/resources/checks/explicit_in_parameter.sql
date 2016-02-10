create or replace procedure foo(bar number) is -- Noncompliant {{Explicitly declare this parameter as IN.}}
begin
  null;
end;
/
create or replace procedure foo(p1 in number,
                                p2 out number,
                                p3 in out number) is
  cursor cur(pcur number) is -- cursor parameters cannot raise a violation
    select 1
      from dual;
begin
  null;
end;
/