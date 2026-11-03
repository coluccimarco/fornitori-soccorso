-- FORNITORI: ASSERT dei risultati del file Excel del docente (dataset storico).
-- Deve FALLIRE se la baseline non coincide. Non equivale a un test già eseguito.
DO $$
DECLARE
    total_m INTEGER;
    ok_m INTEGER;
    pending_m INTEGER;
    expired_m INTEGER;
    rejected_m INTEGER;
    euros NUMERIC(12,2);
    bad_drivers INTEGER;
BEGIN
    SELECT COUNT(*),
           COUNT(*) FILTER (WHERE stato='eseguita'),
           COUNT(*) FILTER (WHERE stato IN ('nuova','proposta','accettata')),
           COUNT(*) FILTER (WHERE stato='scaduta'),
           COUNT(*) FILTER (WHERE stato='non_accettata'),
           COALESCE(SUM(costo_finale) FILTER (WHERE stato='eseguita'),0)::NUMERIC(12,2)
      INTO total_m,ok_m,pending_m,expired_m,rejected_m,euros
    FROM missions WHERE is_fixture=TRUE;
    IF ROW(total_m,ok_m,pending_m,expired_m,rejected_m,euros)
       IS DISTINCT FROM ROW(30,16,6,4,4,1509.71::NUMERIC(12,2)) THEN
        RAISE EXCEPTION 'BASELINE non conforme: n %, OK %, pending %, scadute %, rifiutate %, euro %',
            total_m,ok_m,pending_m,expired_m,rejected_m,euros;
    END IF;

    WITH expected(driver_id,missions_ok,total_eur) AS (
      VALUES (1,1,110.50::NUMERIC),(2,3,222.00::NUMERIC),
             (3,2,250.85::NUMERIC),(4,3,263.54::NUMERIC),
             (5,2,172.72::NUMERIC),(6,2,239.30::NUMERIC),
             (7,1,110.60::NUMERIC),(8,2,140.20::NUMERIC)
    ), actual AS (
      SELECT d.id driver_id,
             COUNT(m.id) FILTER (WHERE m.stato='eseguita') AS missions_ok,
             COALESCE(SUM(m.costo_finale) FILTER (WHERE m.stato='eseguita'),0)::NUMERIC(12,2) AS total_eur
        FROM drivers d
        LEFT JOIN missions m ON m.driver_id=d.id AND m.is_fixture=TRUE
        GROUP BY d.id
    )
    SELECT COUNT(*) INTO bad_drivers
    FROM expected e LEFT JOIN actual a USING(driver_id)
    WHERE a.driver_id IS NULL OR a.missions_ok<>e.missions_ok OR a.total_eur<>e.total_eur;
    IF bad_drivers <> 0 THEN
        RAISE EXCEPTION 'Totali per autista non conformi alla baseline Excel: %',bad_drivers;
    END IF;
    RAISE NOTICE 'BASELINE OK: 30 missioni, 16 OK, 6 pending, 4 scadute, 4 rifiutate, EUR 1509.71';
END $$;
