create table bin (id number, bay number);

create procedure clear_bay(bay number) is
begin
  delete from bin where bin.bay = bay; -- Noncompliant
  delete from bin where bin.bay = clear_bay.bay;
end;
/
