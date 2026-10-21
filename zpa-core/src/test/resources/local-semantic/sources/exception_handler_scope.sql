create or replace procedure case_a is
begin
  declare
    x varchar2(10) := 'loc';
  begin
    raise program_error;
  exception when others then say('handler sees x='||x);
  end;
end;
/
create or replace procedure case_b is
  e exception;
begin
  declare
    e exception;
  begin
    raise e;
  end;
exception
  when e then say('caught by outer e');
  when others then say('others: sqlcode='||sqlcode);
end;
/
create or replace procedure case_c is
  no_data_found exception;
  v number;
begin
  begin
    select 1 into v from dual where 1 = 0;
  exception
    when no_data_found then say('local no_data_found handler');
    when standard.no_data_found then say('standard.no_data_found handler');
  end;
  begin
    raise no_data_found;
  exception
    when standard.no_data_found then say('standard handler caught local');
    when others then say('others caught local ndf, sqlcode='||sqlcode);
  end;
end;
/
create or replace procedure case_d is
begin
  begin
    declare
      x number := 1;
    begin
      raise program_error;
    end;
  exception when others then say('outer handler x='||x);
  end;
end;
/
