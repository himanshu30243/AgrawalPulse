package com.agrawalpulse.analytics.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// Deliberately reads family-service's/membership-service's tables directly with hand-written
// SQL instead of calling their REST APIs - this is the system's one documented exception to
// "never read another service's tables directly" (see docs/microservices-contract.md, "Analytics
// exception"). analytics-service is a read-only reporting/CQRS-style layer that aggregates across
// service ownership boundaries, so going through each owning service's REST API row-by-row would
// be both slower and pointless indirection for pure aggregate counts.
@Repository
public class AnalyticsQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AnalyticsQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countTotalFamilies(UUID chapterId) {
        String sql = "SELECT count(*) FROM families WHERE (:chapterId::uuid IS NULL OR chapter_id = :chapterId)";
        MapSqlParameterSource params = new MapSqlParameterSource("chapterId", chapterId);
        Long result = jdbcTemplate.queryForObject(sql, params, Long.class);
        return result == null ? 0 : result;
    }

    public long countActiveMemberships(UUID chapterId, int year) {
        String sql = """
                SELECT count(*) FROM memberships m
                JOIN families f ON f.id = m.family_id
                WHERE m.status = 'ACTIVE' AND m.year = :year
                  AND (:chapterId::uuid IS NULL OR f.chapter_id = :chapterId)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("chapterId", chapterId)
                .addValue("year", year);
        Long result = jdbcTemplate.queryForObject(sql, params, Long.class);
        return result == null ? 0 : result;
    }

    // "City" here means the chapter's own city (a chapter represents one city/region - see
    // HLD.md), not family.district: chapters.city/state are controlled reference values set up
    // once per chapter, whereas family.district is free-text an admin can type any way, which
    // makes it unreliable to GROUP BY at national scale. Joins into chapters - owned by
    // user-service, but reachable here under the documented analytics exception (both tables
    // live in the one shared database).
    public long countTotalPopulation(UUID chapterId, String gender, String city) {
        String sql = """
                SELECT count(*) FROM family_members fm
                JOIN families f ON f.id = fm.family_id
                JOIN chapters c ON c.id = f.chapter_id
                WHERE (:chapterId::uuid IS NULL OR f.chapter_id = :chapterId)
                  AND (:gender::text IS NULL OR fm.gender = :gender)
                  AND (:city::text IS NULL OR c.city = :city)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("chapterId", chapterId)
                .addValue("gender", gender)
                .addValue("city", city);
        Long result = jdbcTemplate.queryForObject(sql, params, Long.class);
        return result == null ? 0 : result;
    }

    public Map<String, Long> countPopulationByCity(UUID chapterId, String gender) {
        String sql = """
                SELECT c.city AS label, count(*) AS cnt
                FROM family_members fm
                JOIN families f ON f.id = fm.family_id
                JOIN chapters c ON c.id = f.chapter_id
                WHERE (:chapterId::uuid IS NULL OR f.chapter_id = :chapterId)
                  AND (:gender::text IS NULL OR fm.gender = :gender)
                GROUP BY c.city
                ORDER BY c.city
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("chapterId", chapterId)
                .addValue("gender", gender);
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
            counts.put(rs.getString("label"), rs.getLong("cnt"));
        });
        return counts;
    }

    public Map<String, Long> countPopulationByState(UUID chapterId, String gender) {
        String sql = """
                SELECT c.state AS label, count(*) AS cnt
                FROM family_members fm
                JOIN families f ON f.id = fm.family_id
                JOIN chapters c ON c.id = f.chapter_id
                WHERE (:chapterId::uuid IS NULL OR f.chapter_id = :chapterId)
                  AND (:gender::text IS NULL OR fm.gender = :gender)
                GROUP BY c.state
                ORDER BY c.state
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("chapterId", chapterId)
                .addValue("gender", gender);
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
            counts.put(rs.getString("label"), rs.getLong("cnt"));
        });
        return counts;
    }

    public Map<String, Long> countReadyByGender(UUID chapterId, LocalDate femaleThresholdDob, LocalDate maleThresholdDob) {
        String sql = """
                SELECT fm.gender AS gender, count(*) AS cnt
                FROM family_members fm
                JOIN families f ON f.id = fm.family_id
                WHERE fm.marital_status = 'SINGLE'
                  AND (:chapterId::uuid IS NULL OR f.chapter_id = :chapterId)
                  AND ((fm.gender = 'FEMALE' AND fm.date_of_birth <= :femaleThresholdDob)
                    OR (fm.gender = 'MALE' AND fm.date_of_birth <= :maleThresholdDob)
                    OR (fm.gender = 'OTHER' AND fm.date_of_birth <= :maleThresholdDob))
                GROUP BY fm.gender
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("chapterId", chapterId)
                .addValue("femaleThresholdDob", femaleThresholdDob)
                .addValue("maleThresholdDob", maleThresholdDob);

        Map<String, Long> counts = new HashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
            counts.put(rs.getString("gender"), rs.getLong("cnt"));
        });
        return counts;
    }

    public Map<String, Long> countFamiliesByChapter(UUID chapterId) {
        String sql = """
                SELECT c.name AS chapter, count(f.id) AS cnt
                FROM families f
                JOIN chapters c ON c.id = f.chapter_id
                WHERE (:chapterId::uuid IS NULL OR f.chapter_id = :chapterId)
                GROUP BY c.id, c.name
                ORDER BY c.name
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("chapterId", chapterId);
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
            counts.put(rs.getString("chapter"), rs.getLong("cnt"));
        });
        return counts;
    }

    public Map<String, Long> countMembershipsByStatus(UUID chapterId, int year) {
        String sql = """
                SELECT m.status AS status, count(*) AS cnt
                FROM memberships m
                JOIN families f ON f.id = m.family_id
                WHERE m.year = :year
                  AND (:chapterId::uuid IS NULL OR f.chapter_id = :chapterId)
                GROUP BY m.status
                ORDER BY m.status
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("chapterId", chapterId)
                .addValue("year", year);
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
            counts.put(rs.getString("status"), rs.getLong("cnt"));
        });
        return counts;
    }

    public long getTotalMembershipCollection(UUID chapterId) {
        String sql = """
                SELECT COALESCE(SUM(mp.amount), 0) AS total
                FROM membership_payments mp
                JOIN memberships m ON m.id = mp.membership_id
                JOIN families f ON f.id = m.family_id
                WHERE (:chapterId::uuid IS NULL OR f.chapter_id = :chapterId)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("chapterId", chapterId);
        Long result = jdbcTemplate.queryForObject(sql, params, Long.class);
        return result == null ? 0 : result;
    }

    public Map<String, Map<String, Long>> countAgeEligibilityDistribution(UUID chapterId, LocalDate femaleThresholdDob, LocalDate maleThresholdDob) {
        String sql = """
                SELECT
                    CASE
                        WHEN EXTRACT(YEAR FROM AGE(fm.date_of_birth)) < 20 THEN '18-19'
                        WHEN EXTRACT(YEAR FROM AGE(fm.date_of_birth)) < 25 THEN '20-24'
                        WHEN EXTRACT(YEAR FROM AGE(fm.date_of_birth)) < 30 THEN '25-29'
                        WHEN EXTRACT(YEAR FROM AGE(fm.date_of_birth)) < 35 THEN '30-34'
                        WHEN EXTRACT(YEAR FROM AGE(fm.date_of_birth)) < 40 THEN '35-39'
                        ELSE '40+'
                    END AS age_band,
                    fm.gender,
                    COUNT(*) AS cnt
                FROM family_members fm
                JOIN families f ON f.id = fm.family_id
                WHERE fm.marital_status = 'SINGLE'
                  AND (:chapterId::uuid IS NULL OR f.chapter_id = :chapterId)
                  AND ((fm.gender = 'FEMALE' AND fm.date_of_birth <= :femaleThresholdDob)
                    OR (fm.gender = 'MALE' AND fm.date_of_birth <= :maleThresholdDob)
                    OR (fm.gender = 'OTHER' AND fm.date_of_birth <= :maleThresholdDob))
                GROUP BY age_band, fm.gender
                ORDER BY age_band, fm.gender
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("chapterId", chapterId)
                .addValue("femaleThresholdDob", femaleThresholdDob)
                .addValue("maleThresholdDob", maleThresholdDob);

        Map<String, Map<String, Long>> distribution = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
            String ageBand = rs.getString("age_band");
            String gender = rs.getString("gender");
            long count = rs.getLong("cnt");
            distribution.computeIfAbsent(ageBand, k -> new HashMap<>()).put(gender, count);
        });
        return distribution;
    }
}
