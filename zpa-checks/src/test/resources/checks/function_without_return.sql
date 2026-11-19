-- Reported: one case for each kind of statement.

create or replace function only_null return number is -- Noncompliant {{Add a RETURN statement: this function can reach its end without returning a value.}} [[secondary=+3]]
begin
  null;
end;
/

create or replace function logs_and_falls(p_id number) return number is -- Noncompliant [[secondary=+6]]
  l_qty number;
begin
  select qty into l_qty from bin_stock where id = p_id;
  return l_qty;
exception
  when others then
    audit_pkg.note(sqlerrm);
end;
/

create or replace function if_without_else(p number) return number is -- Noncompliant
begin
  if p > 0 then
    return 1;
  end if;
end;
/

create or replace function if_branch_falls(p number) return number is -- Noncompliant
begin
  if p > 0 then
    return 1;
  elsif p < 0 then
    null;
  else
    return 0;
  end if;
end;
/

create or replace function case_branch_falls(p number) return number is -- Noncompliant
begin
  case p
    when 1 then return 10;
    when 2 then null;
  end case;
end;
/

create or replace function searched_case_else_falls(p number) return number is -- Noncompliant
begin
  case
    when p = 1 then return 10;
    else null;
  end case;
end;
/

create or replace function loop_with_exit return number is -- Noncompliant
  i number := 0;
begin
  loop
    i := i + 1;
    exit when i > 3;
  end loop;
end;
/

create or replace function outer_label_exit return number is -- Noncompliant
begin
  <<outer>>
  loop
    loop
      exit outer;
    end loop;
  end loop;
end;
/

create or replace function while_falls(p number) return number is -- Noncompliant
begin
  while p > 0 loop
    return 1;
  end loop;
end;
/

create or replace function while_true_exit return number is -- Noncompliant
begin
  while true loop
    exit;
  end loop;
end;
/

create or replace function for_falls return number is -- Noncompliant
begin
  for i in 1 .. 3 loop
    return i;
  end loop;
end;
/

create or replace function cursor_for_falls return number is -- Noncompliant
begin
  for r in (select id from bin_stock) loop
    return r.id;
  end loop;
end;
/

create or replace function forall_falls return number is -- Noncompliant
  type ids_t is table of number;
  ids ids_t := ids_t(1);
begin
  forall i in 1 .. ids.count
    delete from bin_stock where id = ids(i);
end;
/

create or replace function block_handler_falls return number is -- Noncompliant
begin
  begin
    return 1;
  exception
    when others then
      null;
  end;
end;
/

create or replace function block_body_falls return number is -- Noncompliant
begin
  begin
    null;
  end;
end;
/

create or replace function external_call_falls return number is -- Noncompliant
begin
  audit_pkg.fail('x');
end;
/

create or replace package body bin_api as
  procedure only_note(p varchar2) is
  begin
    audit_pkg.note(p);
  end;

  function handler_calls_note return number is -- Noncompliant
  begin
    return 1;
  exception
    when others then
      only_note('x');
  end;

  -- A nested function, a member of the body.
  function outer_fn return number is
    function inner_fn return number is -- Noncompliant
    begin
      null;
    end;
  begin
    return inner_fn;
  end;
end;
/

create or replace type body bin_obj as
  member function label return varchar2 is -- Noncompliant
  begin
    null;
  end;

  map member function rank_of return number is -- Noncompliant
  begin
    null;
  end;

  -- A constructor that ends without RETURN raises ORA-06503 too.
  constructor function bin_obj(p number) return self as result is -- Noncompliant
  begin
    self.n := p;
  end;
end;
/

-- Falls off both at its END and in a handler.
create or replace function falls_twice(p number) return number is -- Noncompliant [[secondary=+8,+10]]
begin
  if p > 0 then
    return 1;
  end if;
exception
  when no_data_found then
    return 0;
  when others then
    null;
end;
/

-- Not reported.

create or replace function if_else_returns(p number) return number is
begin
  if p > 0 then
    return 1;
  elsif p < 0 then
    return -1;
  else
    return 0;
  end if;
end;
/

create or replace function case_without_else_returns(p number) return number is
begin
  case p
    when 1 then return 10;
    when 2 then return 20;
  end case;
end;
/

create or replace function loop_returns return number is
  i number := 0;
begin
  loop
    i := i + 1;
    if i > 3 then
      return i;
    end if;
  end loop;
end;
/

create or replace function inner_exit_only return number is
begin
  loop
    for i in 1 .. 3 loop
      exit;
    end loop;
    loop
      exit;
    end loop;
  end loop;
end;
/

create or replace function while_true_returns return number is
begin
  while true loop
    return 1;
  end loop;
end;
/

create or replace function code_after_endless_loop return number is
begin
  loop
    null;
  end loop;
  -- unreachable: the loop ends only through an exception
  if sysdate > sysdate - 1 then
    null;
  end if;
end;
/

create or replace function handler_raises return number is
begin
  return 1;
exception
  when no_data_found then
    raise;
  when others then
    raise_application_error(-20001, 'failed');
end;
/

create or replace function nested_blocks return number is
begin
  begin
    begin
      return 1;
    exception
      when others then
        raise;
    end;
  exception
    when others then
      return 0;
  end;
end;
/

create or replace package bin_errors as
  function handler_calls_fail return number;
end;
/

create or replace package body bin_errors as
  procedure fail(p varchar2) is
  begin
    audit_pkg.note(p);
    raise_application_error(-20001, p);
  end;

  procedure fail_twice(p varchar2) is
  begin
    fail(p);
  end;

  function handler_calls_fail return number is
  begin
    return 1;
  exception
    when others then
      fail_twice('x');
  end;
end;
/

-- Skipped.

create or replace function java_spec return number as language java name 'Bin.spec() return int';
/

create or replace package bin_spec as
  function forward_only return number;
end;
/

create or replace function piped return bin_rows pipelined is
begin
  pipe row (1);
end;
/

create or replace function jumps return number is
begin
  goto done;
  <<done>>
  null;
end;
/

create or replace function selected return number is
begin
  $if $$bin_flag $then
  return 1;
  $else
  null;
  $end
end;
/

create or replace function exit_unknown_label return number is
begin
  loop
    exit bin_nowhere;
  end loop;
end;
/
