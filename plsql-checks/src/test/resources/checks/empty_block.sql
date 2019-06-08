begin -- Noncompliant {{Either remove or fill this block of code.}}
  null;
end;
/
create type body t as
  not overriding member procedure foo as
  begin -- Noncompliant
    null;
  end;

  overriding member procedure foo as
  begin  -- Compliant, don't report violation on overriding member
    null;
  end;

  overriding member procedure foo as
  begin
    begin -- Noncompliant
      null;
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
