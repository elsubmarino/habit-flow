package io.streak.habitflow.domain.label.service;

import io.streak.habitflow.domain.favorite.entity.Favorite;
import io.streak.habitflow.domain.favorite.repository.FavoriteRepository;
import io.streak.habitflow.domain.favorite.type.TargetType;
import io.streak.habitflow.domain.label.dto.request.LabelCreateRequest;
import io.streak.habitflow.domain.label.dto.request.LabelUpdateRequest;
import io.streak.habitflow.domain.label.dto.response.LabelListResponse;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.entity.Label;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.entity.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabelService {
    private final LabelRepository labelRepository;
    private final MemberRepository memberRepository;
    private final FavoriteRepository favoriteRepository;

    @Transactional
    public LabelResponse createLabel(LabelCreateRequest labelCreateRequest, UserDetails userDetails) {
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("멤버가 존재하지 않습니다."));
        Label label = Label.builder()
                .name(labelCreateRequest.getName())
                .color(labelCreateRequest.getColor())
                .member(member)
                .build();

        Label savedLabel = labelRepository.save(label);

        if(labelCreateRequest.isFavorite()){
            Favorite favorite = Favorite.builder()
                    .targetType(TargetType.LABEL)
                    .targetId(savedLabel.getId())
                    .member(member)
                    .build();
            favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
                    member.getId(),
                    TargetType.LABEL,
                    savedLabel.getId()
            ).orElseGet(()->favoriteRepository.save(favorite));
        }
        return LabelResponse.from(savedLabel, labelCreateRequest.isFavorite());
    }

    public LabelResponse getLabelById(Long id, UserDetails userDetails) {
        Label label = labelRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("라벨이 존재하지 않습니다."));
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("사용자가 존재하지 않습니다."));
        boolean isFavorite = false;
        Optional<Favorite> favorite = favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
                member.getId(), TargetType.LABEL, label.getId());
        if(favorite.isPresent()){
            isFavorite = true;
        }
        return LabelResponse.from(label,isFavorite);
    }

    @Transactional
    public LabelResponse updateLabel(Long id, LabelUpdateRequest labelUpdateRequest, UserDetails userDetails) {
        Label label = labelRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("라벨이 존재하지 않습니다."));

        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if(!label.getMember().getEmail().equals(userDetails.getUsername())) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        if(labelUpdateRequest.isFavorite()) {
            Favorite favorite = Favorite.builder()
                    .targetType(TargetType.LABEL)
                    .targetId(label.getId())
                    .member(member)
                    .build();
            favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
                    member.getId(),
                    TargetType.LABEL,
                    label.getId()
            ).orElseGet(() -> favoriteRepository.save(favorite));
        }

        label.updateLabel(labelUpdateRequest.getName(),
                labelUpdateRequest.getColor());
        return LabelResponse.from(label, labelUpdateRequest.isFavorite());
    }

    public List<LabelListResponse> getLabels(UserDetails userDetails) {
        String email  = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                        .orElseThrow(()->new IllegalArgumentException("멤버를 찾을 수 없습니다."));
        List<Label> labels = labelRepository.findByMemberId(member.getId());
        return labels.stream()
                .map(LabelListResponse::from)
                .toList();
    }

    @Transactional
    public void deleteLabel(Long id, UserDetails userDetails){
        Label label = labelRepository.findById(id)
                        .orElseThrow(()->new IllegalArgumentException("검색된 라벨이 존재하지 않습니다."));

        if(!label.getMember().getEmail().equals(userDetails.getUsername())) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        labelRepository.deleteById(id);
    }

    public List<LabelListResponse> searchLabels(String keyword){
        return labelRepository.findByNameContaining(keyword)
                .stream().map(LabelListResponse::from)
                .toList();
    }
}
