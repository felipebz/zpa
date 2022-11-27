FUNCTION webutil_get_latency (p_roundtrips_number in number, p_estimated_time_ms in number) RETURN NUMBER IS

/* DOK:=====================================================================================================================
 * Bezeichnung       :  webutil_get_latency
 * Inhalt            :  Berechnung der Latenzzeit für die jeweilige Messung
 *
 * Paramter              p_roundtrips_number, Anzahl der Roundtrips für die Messung
 *                      p_estimated_time_ms, Prozessdauer ohne Latenzen 
 * 
 * Autor             :  F.Hoffmann / Cologne Data
 * Erstellungsdatum  :  31.03.2017
 *
 * Änd-Nr.  Datum        Name        Beschreibung
 * --------------------------------------------------------------------------------------------------------------------------
 *  1       
*/

BEGIN
  
  return (to_number(name_in(':parameter.p_stop') - to_number(name_in('parameter.p_start') - p_estimated_time_ms)) / p_roundtrips_number);  -- Differenz in Millisekunden abzüglich Verzögerung in Millisekunden und datenbankzugriffe für 20 Roundtrips
  
END;
