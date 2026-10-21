create or replace procedure case_a is
  function fact(n number) return number is begin if n <= 1 then return 1; end if; return n * fact(n - 1); end;
  function is_odd(n number) return boolean;
  function is_even(n number) return boolean is begin if n = 0 then return true; end if; return is_odd(n - 1); end;
  function is_odd(n number) return boolean is begin if n = 0 then return false; end if; return is_even(n - 1); end;
begin
  say('fact(5)='||fact(5)||' is_even(10)='||case when is_even(10) then 'T' else 'F' end);
end;
/
create or replace procedure case_b is
  function is_even(n number) return boolean is begin if n = 0 then return true; end if; return is_odd2(n - 1); end;
  function is_odd2(n number) return boolean is begin if n = 0 then return false; end if; return is_even(n - 1); end;
begin
  say('x');
end;
/
create or replace procedure case_c is
  procedure f(n number);
  procedure f(n number) is begin say('f'); end;
  procedure f(n number) is begin say('f again'); end;
begin
  f(1);
end;
/
