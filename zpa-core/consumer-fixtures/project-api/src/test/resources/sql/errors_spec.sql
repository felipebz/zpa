CREATE OR REPLACE PACKAGE error_pkg IS
  app_error EXCEPTION;
  PRAGMA EXCEPTION_INIT(app_error, -20999);
  CURSOR c_pending(p_status NUMBER) RETURN send_queue%ROWTYPE;
  CURSOR c_all IS SELECT id FROM send_queue;
END error_pkg;
/
