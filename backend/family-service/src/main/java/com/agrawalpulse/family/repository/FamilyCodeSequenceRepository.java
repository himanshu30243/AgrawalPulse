package com.agrawalpulse.family.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

// Hands out the next per-(societyCode, cityCode) sequence number for family codes. A plain JDBC
// upsert rather than a JpaRepository because the operation needs to be a single atomic
// "increment and return the new value" statement - a separate find-then-increment-then-save
// would let two concurrent registrations in the same society+city read the same starting value
// and collide. Postgres serializes concurrent INSERT ... ON CONFLICT upserts on the same key, so
// this is race-free without any application-level locking.
@Repository
public class FamilyCodeSequenceRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FamilyCodeSequenceRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int nextSequence(String societyCode, String cityCode) {
        String sql = """
                INSERT INTO family_code_sequences (society_code, city_code, last_sequence)
                VALUES (:societyCode, :cityCode, 1)
                ON CONFLICT (society_code, city_code)
                DO UPDATE SET last_sequence = family_code_sequences.last_sequence + 1
                RETURNING last_sequence
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("societyCode", societyCode)
                .addValue("cityCode", cityCode);
        return jdbcTemplate.queryForObject(sql, params, Integer.class);
    }
}
