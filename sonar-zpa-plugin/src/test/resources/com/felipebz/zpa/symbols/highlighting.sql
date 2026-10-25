<<b>>
declare
  v number;
  c sys_refcursor;
  type r is record (n number);
  type t is table of r;
  a t;
  n number;
begin
  b.v := 1;
  open c for select 1 from dual;
  close c;
  n := a(1).n;
end;
/
