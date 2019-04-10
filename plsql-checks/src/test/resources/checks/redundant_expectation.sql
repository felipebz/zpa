declare
  x number;
  y number;
begin
  ut.expect(x).to_be_equal(x); -- Noncompliant
--          ^

  ut.expect(x).to_be_equal(y);

  ut.expect(x).to_be_null();
end;
