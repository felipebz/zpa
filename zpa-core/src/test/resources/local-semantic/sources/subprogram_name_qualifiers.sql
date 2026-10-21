create or replace procedure case_a is
  x number := 1;
begin
  declare
    x number := 2;
  begin
    say('case_a.x='||case_a.x||' x='||x);
  end;
end;
/
create or replace procedure case_b is
  procedure outerp(v varchar2) is
    w varchar2(10) := 'op.w';
    procedure innerp(v varchar2) is
      w varchar2(10) := 'ip.w';
    begin
      say('outerp.v='||outerp.v||' innerp.v='||innerp.v||' v='||v||' outerp.w='||outerp.w||' w='||w);
    end;
  begin
    innerp('ip.v');
  end;
begin
  outerp('op.v');
end;
/
create or replace procedure case_c is
  procedure p(x number) is
    x2 number := p.x;
  begin
    say('p.x via decl init='||x2);
  end;
begin
  p(5);
end;
/
