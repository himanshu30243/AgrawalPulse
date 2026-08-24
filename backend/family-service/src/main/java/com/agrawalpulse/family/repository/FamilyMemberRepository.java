package com.agrawalpulse.family.repository;

import com.agrawalpulse.family.dto.CensusCandidateDto;
import com.agrawalpulse.family.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {

    // chapter_id is checked directly (row-level, primary tenant boundary) - the family_id/family
    // relationship is a secondary integrity link, not what enforces isolation here.
    List<FamilyMember> findByFamilyIdAndChapterId(UUID familyId, UUID chapterId);

    boolean existsByIdAndChapterId(UUID id, UUID chapterId);

    // JPQL constructor expression, not a native query: fm.familyId is a plain UUID column (no
    // mapped @ManyToOne - see FamilyMember), so this joins two entity roots on a value match
    // rather than navigating an association. Deliberately NOT nativeQuery=true - a native query
    // returning a UUID column into an interface projection hits a real cross-database portability
    // bug (H2's JDBC driver hands the projection proxy a raw byte[] instead of java.util.UUID for
    // that combination, though Postgres doesn't; JPQL routes through each entity's mapped Java
    // type instead of a raw JDBC ResultSet column type, so both databases return proper UUID/enum
    // instances the same way). The join to families is only to pull in district (a Family-owned
    // display field) - the WHERE clause still scopes on fm.chapterId directly, not through the join.
    @Query("""
            SELECT new com.agrawalpulse.family.dto.CensusCandidateDto(
                fm.id, fm.chapterId, fm.dateOfBirth, fm.gender, fm.education, fm.profession, f.district, fm.maritalStatus)
            FROM FamilyMember fm, Family f
            WHERE f.id = fm.familyId AND fm.chapterId = :chapterId
            """)
    List<CensusCandidateDto> findCensusCandidatesByChapterId(@Param("chapterId") UUID chapterId);
}
