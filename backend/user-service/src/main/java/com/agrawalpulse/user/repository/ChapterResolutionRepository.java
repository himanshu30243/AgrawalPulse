package com.agrawalpulse.user.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// Atomically resolves an existing chapter by (city, state) or creates one on the fly - backs
// self-registration's chapter assignment (see UserServiceImpl#registerUser), replacing the old
// "assign the first chapter that happens to exist" placeholder. A single
// INSERT ... ON CONFLICT ... DO UPDATE ... RETURNING statement (rather than find-then-create) so
// two people signing up from the same brand-new city at once can never create two chapters for
// it - Postgres serializes concurrent upserts on the same unique-index key
// (uq_chapters_city_state, V8 migration). Same proven pattern as family-service's
// FamilyCodeSequenceRepository.
//
// Chapter.id uses Hibernate-side UUID generation (GenerationType.UUID, see BaseEntity) rather than
// a database default, so this raw-SQL path has to generate the id itself too, same as any entity
// created outside the ORM.
@Repository
public class ChapterResolutionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ChapterResolutionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID resolveOrCreateChapter(String city, String state) {
        String sql = """
                INSERT INTO chapters (id, name, city, state)
                VALUES (:id, :name, :city, :state)
                ON CONFLICT (lower(city), lower(state))
                DO UPDATE SET city = chapters.city
                RETURNING id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("name", city + " Chapter")
                .addValue("city", city)
                .addValue("state", state);
        return jdbcTemplate.queryForObject(sql, params, UUID.class);
    }
}
