create or replace procedure case_a is
  procedure outerp(p number) is
    acc number := 0;
    procedure bump is begin acc := acc + p; end;
    function twice(p number) return number is begin return p * 2; end;
  begin
    bump; bump;
    say('acc='||acc||' twice(7)='||twice(7)||' p='||p);
  end;
begin
  outerp(5);
end;
/
