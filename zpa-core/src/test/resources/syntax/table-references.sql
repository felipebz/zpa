select * from tab;
select * from sch.tab;
select * from tab@link.domain.com;
select * from sch.tab@link;
select * from "Sch"."Tab"@"Link"."Domain"."Com";
insert into tab values (1);
update tab set value = 1;
delete from tab;
merge into target using source on (target.id = source.id)
when matched then update set target.value = source.value;
select * from (select * from inner_table);
select * from table_function(value);
select * from (values (1)) as value_table(value_column);
