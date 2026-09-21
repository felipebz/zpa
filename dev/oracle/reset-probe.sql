set serveroutput on size unlimited
set feedback off
set verify off
set define off

declare
  l_failures pls_integer := 0;
begin
  for r in (
    select object_type, object_name
      from user_objects
     where object_type in (
       'MATERIALIZED VIEW',
       'VIEW',
       'TRIGGER',
       'PACKAGE',
       'PROCEDURE',
       'FUNCTION',
       'SYNONYM',
       'SEQUENCE',
       'TABLE',
       'TYPE'
     )
     order by case object_type
       when 'MATERIALIZED VIEW' then 1
       when 'VIEW' then 2
       when 'TRIGGER' then 3
       when 'PACKAGE' then 4
       when 'PROCEDURE' then 5
       when 'FUNCTION' then 6
       when 'SYNONYM' then 7
       when 'SEQUENCE' then 8
       when 'TABLE' then 9
       when 'TYPE' then 10
       else 100
     end
  ) loop
    begin
      case r.object_type
        when 'MATERIALIZED VIEW' then
          execute immediate 'drop materialized view "' || replace(r.object_name, '"', '""') || '"';
        when 'VIEW' then
          execute immediate 'drop view "' || replace(r.object_name, '"', '""') || '"';
        when 'TRIGGER' then
          execute immediate 'drop trigger "' || replace(r.object_name, '"', '""') || '"';
        when 'PACKAGE' then
          execute immediate 'drop package "' || replace(r.object_name, '"', '""') || '"';
        when 'PROCEDURE' then
          execute immediate 'drop procedure "' || replace(r.object_name, '"', '""') || '"';
        when 'FUNCTION' then
          execute immediate 'drop function "' || replace(r.object_name, '"', '""') || '"';
        when 'SYNONYM' then
          execute immediate 'drop synonym "' || replace(r.object_name, '"', '""') || '"';
        when 'SEQUENCE' then
          execute immediate 'drop sequence "' || replace(r.object_name, '"', '""') || '"';
        when 'TABLE' then
          execute immediate 'drop table "' || replace(r.object_name, '"', '""') || '" cascade constraints purge';
        when 'TYPE' then
          execute immediate 'drop type "' || replace(r.object_name, '"', '""') || '" force';
      end case;
    exception
      when others then
        l_failures := l_failures + 1;
        dbms_output.put_line(
          'ZPA_CLEANUP_WARNING|' || r.object_type || '|' || r.object_name || '|' || sqlerrm
        );
    end;
  end loop;

  if l_failures > 0 then
    raise_application_error(
      -20990,
      'Probe schema cleanup failed for ' || l_failures || ' object(s)'
    );
  end if;
end;
/
