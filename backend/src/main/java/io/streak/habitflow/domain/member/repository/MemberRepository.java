package io.streak.habitflow.domain.member.repository;

import io.streak.habitflow.domain.member.entity.Member;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    List<Member> findByEmailIn(List<String> email);

    default Member getOrThrow(Long memberId){
        return findById(memberId).orElseThrow(()->new EntityNotFoundException("멤버가 존재하지 않습니다."));
    }
}
