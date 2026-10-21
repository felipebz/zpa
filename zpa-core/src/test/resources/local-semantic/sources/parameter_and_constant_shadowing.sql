create or replace procedure case_a is
  c constant number := 1;
  procedure inner(p number) is
  begin
    say('param p='||p);
    declare
      p varchar2(10) := 'shadow';
      c varchar2(10) := 'cshadow';
    begin
      p := p||'!';
      c := c||'!';
      say('inner p='||p||' c='||c);
    end;
    say('after p='||p||' c='||c);
  end;
begin
  inner(42);
end;
/
