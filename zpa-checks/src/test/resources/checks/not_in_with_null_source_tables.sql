create table bin_rack (id number not null, code varchar2(5));
create table bin_box (rack_id number, sealed_rack_id number not null);

create or replace procedure run_racks is
  n number;
begin
  -- A column written without NOT NULL may hold null.
  select count(*) into n from bin_rack where id not in (select rack_id from bin_box); -- Noncompliant
  select count(*) into n from bin_rack where id not in (select sealed_rack_id from bin_box);
end;
/
