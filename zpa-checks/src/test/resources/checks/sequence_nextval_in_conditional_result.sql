declare
  value number;
begin
  select case when 1 = 1 then sequence_one.nextval else 0 end into value from dual; -- Noncompliant
  select case when 1 = 1 then 0 else sequence_two.nextval end into value from dual; -- Noncompliant
  select case value when 1 then sequence_one.nextval else 0 end into value from dual; -- Noncompliant
  select case value when 1 then owner_name.sequence_one.nextval else 0 end into value from dual; -- Noncompliant
  select case value when 1 then sequence_one.NEXTVAL else 0 end into value from dual; -- Noncompliant
  select case value when 1 then "SEQUENCE_ONE".nextval else 0 end into value from dual; -- Noncompliant
  select case when 1 = 1 then to_char(sequence_one.nextval) else 'x' end into value from dual; -- Noncompliant
  select case when 1 = 1 then sequence_one.nextval + 1 else 0 end into value from dual; -- Noncompliant [[sc=44;ec=51]]

  select case when sequence_one.nextval > 100 then 1 else 0 end into value from dual;
  select case sequence_one.nextval when 1 then 'first' else 'other' end into value from dual;
  select case value when sequence_one.nextval then 'matched' else 'other' end into value from dual;
  select case when 1 = 1 then sequence_one.currval else 0 end into value from dual;
  select case when 1 = 1 then sequence_one."NEXTVAL" else 0 end into value from dual;
  value := sequence_one.nextval;
  value := case when 1 = 1 then sequence_one.nextval else 0 end;

  select decode(1, 1, sequence_one.nextval, 0) into value from dual; -- Noncompliant
  select decode(1, 1, sequence_one.nextval) into value from dual; -- Noncompliant
  select decode(1, 1, 0, 2, sequence_two.nextval) into value from dual; -- Noncompliant
  select DECODE(1, 1, sequence_one.nextval, 0) into value from dual; -- Noncompliant
  select DeCoDe(1, 1, sequence_one.nextval, 0) into value from dual; -- Noncompliant
  select "DECODE"(1, 1, sequence_one.nextval, 0) into value from dual; -- Noncompliant
  select decode(1, 1, 0, sequence_one.nextval) into value from dual; -- Noncompliant
  select decode(1, 1, to_char(sequence_one.nextval), 0) into value from dual; -- Noncompliant
  select decode(sequence_one.nextval, 1, 'yes', 'no') into value from dual;
  select decode(1, sequence_one.nextval, 'yes', 'no') into value from dual;
  select decode(1, 1, sequence_one.currval, 0) into value from dual;
  select "decode"(1, 1, sequence_one.nextval, 0) into value from dual;
  select "DeCoDe"(1, 1, sequence_one.nextval, 0) into value from dual;
  select package_name.decode(1, 1, sequence_one.nextval, 0) into value from dual;
  select package_name."DECODE"(1, 1, sequence_one.nextval, 0) into value from dual;
  select schema_name.decode(1, 1, sequence_one.nextval, 0) into value from dual;
  select schema_name."DECODE"(1, 1, sequence_one.nextval, 0) into value from dual;

  select decode(
    1,
    1, sequence_one.nextval, -- Noncompliant
    2, sequence_two.nextval, -- Noncompliant
    sequence_three.nextval -- Noncompliant
  ) into value from dual;

  select case when 1 = 1 then case when 2 = 2 then sequence_one.nextval else 0 end else 0 end into value from dual; -- Noncompliant
  select case when 1 = 1 then case sequence_one.nextval when 1 then 1 else 2 end else 0 end into value from dual; -- Noncompliant
  select case when 1 = 1 then case when sequence_one.nextval > 0 then 1 else 2 end else 0 end into value from dual; -- Noncompliant
  select case when 1 = 1 then decode(sequence_one.nextval, 1, 'a', 'b') else 'x' end into value from dual; -- Noncompliant
  select decode(1, 1, case sequence_one.nextval when 1 then 'a' else 'b' end, 'x') into value from dual; -- Noncompliant
  select case when 1 = 1 then (select t.nextval from some_table t) else 0 end into value from dual;
  insert into target_table (value_column) values (case when 1 = 1 then sequence_one.nextval else 0 end); -- Noncompliant
  update target_table set value_column = case when 1 = 1 then sequence_one.nextval else 0 end; -- Noncompliant
  merge into target_table target using source_table source on (target.id = source.id) when matched then update set target.value_column = case when 1 = 1 then sequence_one.nextval else 0 end; -- Noncompliant
end;
