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

begin
  dyn('CASE_CA local SUBTYPE number IS DATE; v number', q'~DECLARE SUBTYPE number IS DATE; v number; BEGIN pkg.processar(v); END;~');
  dyn('CASE_CB local processar(varchar2) + qualified pkg.processar(v DATE) + unqualified', q'~DECLARE v DATE; PROCEDURE processar(p VARCHAR2) IS BEGIN say('local processar(varchar2)'); END; BEGIN pkg.processar(v); processar(v); END;~');
  dyn('CASE_CC local SUBTYPE varchar2 IS DATE; v varchar2', q'~DECLARE SUBTYPE varchar2 IS DATE; v varchar2; BEGIN pkg.processar(v); END;~');
  dyn('CASE_CD anchor to outer var after inner shadowing type', q'~DECLARE SUBTYPE amount_t IS DATE; a amount_t; BEGIN DECLARE SUBTYPE amount_t IS NUMBER; v a%TYPE; BEGIN pkg.processar(v); END; END;~');
  dyn('CASE_CE function returning local subtype', q'~DECLARE SUBTYPE amount_t IS DATE; FUNCTION f RETURN amount_t IS BEGIN RETURN NULL; END; BEGIN pkg.processar(f); END;~');
  dyn('CASE_CF collection element of local subtype', q'~DECLARE SUBTYPE amount_t IS DATE; TYPE t IS TABLE OF amount_t INDEX BY PLS_INTEGER; c t; BEGIN c(1) := sysdate; pkg.processar(c(1)); END;~');
  dyn('CASE_CG local var named pkg_other hides package in %TYPE', q'~DECLARE pkg_other NUMBER; v pkg_other.g_amt%TYPE; BEGIN pkg.processar(v); END;~');
  dyn('CASE_CH CASE expression mixing', q'~DECLARE SUBTYPE amount_t IS DATE; v amount_t; BEGIN pkg.processar(NVL(v, sysdate)); END;~');
end;
/

DECLARE SUBTYPE number IS DATE; v number; BEGIN pkg.processar(v); END;
/

DECLARE v DATE; PROCEDURE processar(p VARCHAR2) IS BEGIN say('local processar(varchar2)'); END; BEGIN pkg.processar(v); processar(v); END;
/

DECLARE SUBTYPE varchar2 IS DATE; v varchar2; BEGIN pkg.processar(v); END;
/

DECLARE SUBTYPE amount_t IS DATE; a amount_t; BEGIN DECLARE SUBTYPE amount_t IS NUMBER; v a%TYPE; BEGIN pkg.processar(v); END; END;
/

DECLARE SUBTYPE amount_t IS DATE; FUNCTION f RETURN amount_t IS BEGIN RETURN NULL; END; BEGIN pkg.processar(f); END;
/

DECLARE SUBTYPE amount_t IS DATE; TYPE t IS TABLE OF amount_t INDEX BY PLS_INTEGER; c t; BEGIN c(1) := sysdate; pkg.processar(c(1)); END;
/

DECLARE pkg_other NUMBER; v pkg_other.g_amt%TYPE; BEGIN pkg.processar(v); END;
/

DECLARE SUBTYPE amount_t IS DATE; v amount_t; BEGIN pkg.processar(NVL(v, sysdate)); END;
/
