package it.exprivia.fornitori.repository;

import java.time.OffsetDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Short-lived practice authorization, not proof of a vehicle owner's identity. */
@Repository
public class ChatLookupGrantRepository {
    private final JdbcTemplate jdbc;
    public ChatLookupGrantRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** customerId nullo: targa senza polizza verificabile, la pratica nascerà da verificare per la centrale. */
    public void grant(String sessionId, Long customerId, String plate, OffsetDateTime expiresAt, OffsetDateTime now) {
        jdbc.update("delete from chat_lookup_grants where expires_at <= ?", now);
        jdbc.update("""
                insert into chat_lookup_grants(session_id, customer_id, targa_norm, expires_at) values (?, ?, ?, ?)
                on conflict (session_id) do update
                set customer_id = excluded.customer_id, targa_norm = excluded.targa_norm, expires_at = excluded.expires_at
                """, sessionId, customerId, plate, expiresAt);
    }

    public void revoke(String sessionId) {
        jdbc.update("delete from chat_lookup_grants where session_id = ?", sessionId);
    }

    /** plate nulla: richiesta del vecchio formato, legata al solo cliente. */
    public boolean permits(String sessionId, Long customerId, String plate, OffsetDateTime now) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(select 1 from chat_lookup_grants
                where session_id = ? and customer_id is not distinct from ?::bigint
                  and (?::varchar is null or targa_norm = ?) and expires_at > ?)
                """, Boolean.class, sessionId, customerId, plate, plate, now));
    }

    /** Senza un cliente da bloccare, serializza così i tentativi concorrenti con lo stesso requestId. */
    public void lockRequest(String requestId) {
        jdbc.query("select pg_advisory_xact_lock(hashtextextended(?, 0))", result -> null, requestId);
    }
}
