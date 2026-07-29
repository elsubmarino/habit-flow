package io.streak.habitflow.domain.member.repository;

import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    List<Member> findByEmailIn(List<String> email);
    List<Member> findAllByPublicIdIn(Collection<UUID> publicIds);

    Optional<Member> findByPublicId(UUID publicId);

    default Member getOrThrow(Long memberId){
        return findById(memberId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }

    default Member getOrThrowByPublicId(UUID publicId){
        return findByPublicId(publicId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
}
