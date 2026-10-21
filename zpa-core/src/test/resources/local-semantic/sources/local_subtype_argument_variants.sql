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

exec say('== CASE_BA inner block shadows amount_t as NUMBER, v declared in inner');
DECLARE
  SUBTYPE amount_t IS DATE;
BEGIN
  DECLARE
    SUBTYPE amount_t IS NUMBER;
    v amount_t;
  BEGIN
    pkg.processar(v);
  END;
END;
/
exec say('== CASE_BB v declared in outer (DATE), inner shadows amount_t as NUMBER');
DECLARE
  SUBTYPE amount_t IS DATE;
  v amount_t;
BEGIN
  DECLARE
    SUBTYPE amount_t IS NUMBER;
    w amount_t;
  BEGIN
    pkg.processar(v);
    pkg.processar(w);
  END;
END;
/
exec say('== CASE_BC v pkg_other.g_amt%TYPE');
DECLARE
  v pkg_other.g_amt%TYPE;
BEGIN
  pkg.processar(v);
END;
/
exec say('== CASE_BD v pkg_other.amt_t (direct package subtype)');
DECLARE
  v pkg_other.amt_t;
BEGIN
  pkg.processar(v);
END;
/
exec say('== CASE_BE v NUMBER := 1');
DECLARE
  v NUMBER := 1;
BEGIN
  pkg.processar(v);
END;
/
exec say('== CASE_BF literals 1 / sysdate / date literal');
BEGIN
  pkg.processar(1);
  pkg.processar(sysdate);
  pkg.processar(date '2020-01-01');
END;
/
exec say('== CASE_BG local subtype of package subtype amt_n (number)');
DECLARE
  SUBTYPE amount_t IS pkg_other.amt_n;
  v amount_t;
BEGIN
  pkg.processar(v);
END;
/
exec say('== CASE_BH label-qualified outer DATE var shadowed by inner NUMBER var');
BEGIN
  <<o>>
  DECLARE
    v DATE;
  BEGIN
    DECLARE
      v NUMBER;
    BEGIN
      pkg.processar(o.v);
      pkg.processar(v);
    END;
  END;
END;
/
exec say('== CASE_BI local SUBTYPE integer IS DATE');
DECLARE
  SUBTYPE integer IS DATE;
  v integer;
BEGIN
  pkg.processar(v);
END;
/
exec say('== CASE_BJ v %TYPE of local subtype-typed variable');
DECLARE
  SUBTYPE amount_t IS DATE;
  a amount_t;
  v a%TYPE;
BEGIN
  pkg.processar(v);
END;
/
exec say('== CASE_BK record field of local record type');
DECLARE
  SUBTYPE amount_t IS DATE;
  TYPE r_t IS RECORD (f amount_t);
  r r_t;
BEGIN
  pkg.processar(r.f);
END;
/
begin
  dyn('CASE_BL v pkg_other.amt_t%TYPE (%TYPE on a subtype)', q'~DECLARE v pkg_other.amt_t%TYPE; BEGIN pkg.processar(v); END;~');
  dyn('CASE_BM string literal arg', q'~BEGIN pkg.processar('2020-01-01'); END;~');
  dyn('CASE_BN NULL arg', q'~BEGIN pkg.processar(NULL); END;~');
  dyn('CASE_BO local var named pkg hides package', q'~DECLARE pkg NUMBER; v DATE; BEGIN pkg.processar(v); END;~');
  dyn('CASE_BP local procedure processar(varchar2) does NOT hide pkg.processar (qualified)', q'~DECLARE PROCEDURE processar(p VARCHAR2) IS BEGIN say('local processar'); END; v DATE; BEGIN pkg.processar(v); processar(v); END;~');
  dyn('CASE_BQ subtype declared in inner block, used after it ends', q'~DECLARE SUBTYPE amount_t IS NUMBER; BEGIN DECLARE SUBTYPE amount_t IS DATE; BEGIN NULL; END; DECLARE v amount_t; BEGIN pkg.processar(v); END; END;~');
  dyn('CASE_BR local subtype amount_t IS VARCHAR2(10) -> ambiguous?', q'~DECLARE SUBTYPE amount_t IS VARCHAR2(10); v amount_t; BEGIN pkg.processar(v); END;~');
  dyn('CASE_BS TIMESTAMP var', q'~DECLARE v TIMESTAMP; BEGIN pkg.processar(v); END;~');
  dyn('CASE_BT PLS_INTEGER var', q'~DECLARE v PLS_INTEGER := 1; BEGIN pkg.processar(v); END;~');
end;
/

DECLARE v pkg_other.amt_t%TYPE; BEGIN pkg.processar(v); END;
/

BEGIN pkg.processar('2020-01-01'); END;
/

BEGIN pkg.processar(NULL); END;
/

DECLARE pkg NUMBER; v DATE; BEGIN pkg.processar(v); END;
/

DECLARE PROCEDURE processar(p VARCHAR2) IS BEGIN say('local processar'); END; v DATE; BEGIN pkg.processar(v); processar(v); END;
/

DECLARE SUBTYPE amount_t IS NUMBER; BEGIN DECLARE SUBTYPE amount_t IS DATE; BEGIN NULL; END; DECLARE v amount_t; BEGIN pkg.processar(v); END; END;
/

DECLARE SUBTYPE amount_t IS VARCHAR2(10); v amount_t; BEGIN pkg.processar(v); END;
/

DECLARE v TIMESTAMP; BEGIN pkg.processar(v); END;
/

DECLARE v PLS_INTEGER := 1; BEGIN pkg.processar(v); END;
/
