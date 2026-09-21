-- Run as an administrative user inside the target PDB, for example FREEPDB1.
-- Adjust username, password and tablespace if needed.

create user zpa_probe identified by "CHANGE_ME"
  default tablespace users
  temporary tablespace temp
  quota unlimited on users;

grant create session,
      create table,
      create view,
      create procedure,
      create sequence,
      create trigger,
      create type,
      create synonym
  to zpa_probe;
