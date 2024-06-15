-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-PROFILE.html
SQL> create or replace function my_mandatory_verify_function
 ( username     varchar2,
   password     varchar2,
   old_password varchar2)
 return boolean IS
begin
   -- mandatory verify function will always be evaluated regardless of the
   -- password verify function that is associated to a particular profile/user
   -- requires the minimum password length to be 8 characters
   if not ora_complexity_check(password, chars => 8) then
      return(false);
   end if;
   return(true);
end;
/
  2    3    4    5    6    7    8    9   10   11   12   13   14   15   16   17   18  
Function created.