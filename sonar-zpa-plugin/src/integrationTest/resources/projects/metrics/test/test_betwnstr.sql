create or replace package test_betwnstr as
  -- %suite(Between string function)

  -- %test(Returns substring from start position to end position)
  procedure normal_case;

  -- %test(Returns substring when start position is zero)
  procedure zero_start_position;

  -- %test(Returns string until end if end position is greater than string length)
  procedure big_end_position;

  -- %test(Returns null for null input string value)
  procedure null_string;

  -- %test(A demo of test raising runtime exception)
  procedure bad_params;

  -- %test(A demo of failing test)
  procedure bad_test;

  -- %test(Demo of a disabled test)
  -- %disabled
  procedure disabled_test;

end;
/
create or replace package body test_betwnstr as

  procedure normal_case is
  begin
    ut.expect(betwnstr('1234567', 2, 5)).to_equal('2345');
  end;

  procedure zero_start_position is
  begin
    ut.expect(betwnstr('1234567', 0, 5)).to_(equal('12345'));
  end;

  procedure big_end_position is
  begin
    ut.expect(betwnstr('1234567', 0, 500)).to_(equal('1234567'));
  end;

  procedure null_string is
  begin
    ut.expect(betwnstr(null, 2, 5)).to_(be_null());
  end;

  procedure bad_params is
  begin
    ut.expect(betwnstr('1234567', 'a', 'b')).to_(be_null());
  end;

  procedure bad_test
    is
  begin
    ut.expect(betwnstr('1234567', 0, 500)).to_(equal('1'));
  end;

  procedure disabled_test is
  begin
    ut.expect(betwnstr(null, null, null)).not_to(be_null);
  end;

end;
/
