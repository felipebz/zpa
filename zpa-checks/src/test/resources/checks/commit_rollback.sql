create procedure test is

  procedure test2 is
    pragma autonomous_transaction;
  begin
    commit; -- no violation here, it is in an autonomous transaction
  end;
  
begin
 commit; -- Noncompliant {{Avoid COMMIT calls unless it is in an autonomous transaction.}}
 
 begin
   rollback; -- Noncompliant {{Avoid ROLLBACK calls unless it is in an autonomous transaction.}}

   -- should ignore rollback to savepoint
   rollback to savepoint s1;
   rollback to s1;
 end;
end;
/

begin
  commit; -- correct, it isn't a database object
end;
/

-- A nested subprogram runs in the transaction of the unit that calls it, so its own declarations do not matter.
create procedure autonomous_outer is
  pragma autonomous_transaction;

  procedure nested is
    l_value number := 1;
  begin
    commit;
  end;
begin
  begin
    rollback;
  end;
  for i in 1 .. 2 loop
    commit;
  end loop;
  nested;
end;
/

-- The pragma is accepted only in a declarative part (PLS-00710 in the executable section).
create procedure pragma_in_body is
begin
  pragma autonomous_transaction;
  commit; -- Noncompliant
end;
/

create package body tx_pkg is
  procedure autonomous is
    pragma autonomous_transaction;
  begin
    commit;
  end;

  procedure plain is
  begin
    commit; -- Noncompliant
  end;
end;
/

create trigger autonomous_trigger
after insert on tab
declare
  pragma autonomous_transaction;
begin
  commit;
end;
/

create trigger plain_trigger
after insert on tab
begin
  commit; -- Noncompliant
end;
/

create type body tx_type as
  member procedure autonomous is
    pragma autonomous_transaction;
  begin
    commit;
  end;

  member procedure plain is
  begin
    rollback; -- Noncompliant
  end;
end;
/

-- Conditional compilation may hide the pragma.
create procedure conditional_pragma is
  $if $$autonomous $then
  pragma autonomous_transaction;
  $end
begin
  commit;
end;
/

-- A directive that keeps no text hides nothing Oracle compiles under the configured settings.
create procedure error_directive is
  $if dbms_db_version.ver_le_10 $then
    $error 'unsupported' $end
  $end
  l_value number;
begin
  commit; -- Noncompliant
end;
/

-- The kept branch has no pragma, but Oracle may compile the other one.
create procedure pragma_in_other_branch is
  $if $$plain $then
  l_value number;
  $else
  pragma autonomous_transaction;
  $end
begin
  commit;
end;
/
