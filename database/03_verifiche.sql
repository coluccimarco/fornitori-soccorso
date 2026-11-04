-- FORNITORI / report atteso baseline Excel.
SELECT 'utenti' entity, count(*) observed, 10 expected FROM app_users
UNION ALL SELECT 'autisti',count(*),8 FROM drivers
UNION ALL SELECT 'mezzi',count(*),8 FROM vehicles
UNION ALL SELECT 'compagnie',count(*),4 FROM insurance_companies
UNION ALL SELECT 'clienti',count(*),40 FROM customers
UNION ALL SELECT 'officine',count(*),6 FROM workshops
UNION ALL SELECT 'missioni-fixture',count(*),30 FROM missions WHERE is_fixture=TRUE;

-- 16 OK, 6 pending, 4 scaduta, 4 non_accettata / totale costo 1509.71
SELECT
 COUNT(*) FILTER (WHERE stato='eseguita') AS eseguite,
 COUNT(*) FILTER (WHERE stato IN ('nuova','proposta','accettata')) AS pending,
 COUNT(*) FILTER (WHERE stato='scaduta') AS scadute,
 COUNT(*) FILTER (WHERE stato='non_accettata') AS non_accettate,
 ROUND(COALESCE(SUM(costo_finale) FILTER (WHERE stato='eseguita'),0),2) AS costo_eseguite
FROM missions WHERE is_fixture=TRUE;

-- Totali autista di riferimento
SELECT d.id, d.nome,
 COUNT(*) FILTER (WHERE m.stato='eseguita') AS missioni_ok,
 COALESCE(SUM(m.costo_finale) FILTER (WHERE m.stato='eseguita'),0) AS guadagno_eur
FROM drivers d LEFT JOIN missions m ON m.driver_id=d.id AND m.is_fixture=TRUE
GROUP BY d.id,d.nome ORDER BY d.id;

-- Incoerenze ORIGINALI dei record didattici: autista #3 ha 2 missioni aperte.
SELECT driver_id,count(*) active_missions,array_agg(codice ORDER BY codice) codici
FROM missions
WHERE is_fixture=TRUE AND stato IN ('proposta','accettata')
GROUP BY driver_id HAVING count(*)>1;

-- Disponibilità reale dei soli lavori live: NON fidarsi di drivers.stato_seed.
SELECT d.id,d.nome,
 CASE WHEN EXISTS(SELECT 1 FROM missions m WHERE m.driver_id=d.id AND NOT m.is_fixture AND m.stato IN ('proposta','accettata'))
 THEN 'occupato' ELSE 'disponibile' END AS disponibilita_operativa
FROM drivers d ORDER BY d.id;

-- Controllo integrià: tutte le missioni devono avere cliente valido (FK), costi solo per OK.
SELECT codice,stato,km_effettivi,costo_finale
FROM missions WHERE is_fixture=TRUE AND
 ((stato='eseguita' AND costo_finale IS NULL) OR (stato<>'eseguita' AND costo_finale IS NOT NULL));

-- Nota: le missioni storico al 28/10/2026 NON vanno automaticamente fatte scadere dal job live.
