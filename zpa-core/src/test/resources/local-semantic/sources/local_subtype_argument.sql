create or replace package pkg is
  procedure processar(p number);
  procedure processar(p date);
end;
/
create or replace package body pkg is
  procedure processar(p number) is begin say('pkg.processar(NUMBER)'); end;
  procedure processar(p date) is begin say('pkg.processar(DATE)'); end;
end;
/
create or replace package pkg_other is
  subtype amt_t is date;
  subtype amt_n is number;
  g_amt amt_t;
  g_num amt_n;
end;
/

exec say('== CASE_BASE base');
DECLARE
  SUBTYPE amount_t IS DATE;
  v amount_t;
BEGIN
  pkg.processar(v);
END;
/
