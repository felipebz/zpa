begin
  null; -- Noncompliant {{Either remove or fill this block of code.}}
end;
/
create type body t as
  not overriding member procedure foo as
  begin
    null; -- Noncompliant
  end;

  overriding member procedure foo as
  begin
    null;  -- Compliant, don't report violation on overriding member
  end;

  overriding member procedure foo as
  begin
    begin
      null; -- Noncompliant
    end;
  end;
end;
/
declare
  var number;
begin
  null;
  var := 1;
end;
/
declare
  var number;
begin
  var := 1;
end;
/
