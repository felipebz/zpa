create procedure test is

  procedure test2 is
    pragma autonomous_transaction;
  begin
    commit; -- no violation here, it is in an autonomous transaction
  end;
  
begin
 commit; -- violation
 
 begin
   rollback; -- violation
 end;
end;
/

begin
  commit; -- correct, it isn't a database object
end;
/