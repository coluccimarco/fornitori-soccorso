package it.exprivia.fornitori.repository;

import java.time.OffsetDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Short-lived practice authorization, not proof of a vehicle owner's identity. */
@Repository
public class ChatLookupGrantRepository {
    private final JdbcTemplate jdbc;
    public ChatLookupGrantRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void grant(String sessionId, Long customerId, OffsetDateTime expiresAt, OffsetDateTime now) {
        jdbc.update("delete from chat_lookup_grants where expires_at <= ?", now);
        jdbc.update("""
                insert into chat_lookup_grants(session_id, customer_id, expires_at) values (?, ?, ?)
                on conflict (session_id) do update
                set customer_id = excluded.customer_id, expires_at = excluded.expires_at
                """, sessionId, customerId, expiresAt);
    }

    public void revoke(String sessionId) {
        jdbc.update("delete from chat_lookup_grants where session_id = ?", sessionId);
    }

    public boolean permits(String sessionId, Long customerId, OffsetDateTime now) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(select 1 from chat_lookup_grants
                where session_id = ? and customer_id = ? and expires_at > ?)
                """, Boolean.class, sessionId, customerId, now));
    }
}
