function f_get_current_time_ms return number

/* DOK:=================================================================================================
 * Bezeichnung      :  Funktion f_current_time_ms
 * Inhalt            :  Ermittlung eines Zeitstempels in ms
 *
 * Returnwert        :  -/-
 * Autor            :  F.Hoffmann / Cologne Data
 * Erstellungsdatum  :  31.03.2017
 * Änd-Nr.  Datum         Name         Beschreibung
 * -----------------------------------------------------------------------------------------------------------------------------
 *     1    21.1.2018    Fhoffmann   Optimization by PGutiérrez 
*/

    is
        out_result number;
    begin
        out_result := JAVA_SYSTEM.CURRENTTIMEMILLIS;
        --select extract(day from(systimestamp - to_timestamp('2017-01-01', 'YYYY-MM-DD'))) * 86400000 
        --    + to_number(to_char(sys_extract_utc(systimestamp), 'SSSSSFF3'))
        --into out_result
        --from dual;
        return out_result;
    end f_get_current_time_ms;