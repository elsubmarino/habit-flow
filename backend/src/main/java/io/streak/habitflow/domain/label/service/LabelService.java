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
import io.streak.habitflow.global.common.dto.ScrollResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
    public LabelResponse createLabel(LabelCreateRequest labelCreateRequest, Long memberId) {
        Member member = memberRepository.getReferenceById(memberId);
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

    public LabelResponse getLabelById(Long labelId, Long memberId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(()->new IllegalArgumentException("라벨이 존재하지 않습니다."));
        boolean isFavorite = false;
        Optional<Favorite> favorite = favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
                memberId,TargetType.LABEL, label.getId());
        if(favorite.isPresent()){
            isFavorite = true;
        }
        return LabelResponse.from(label,isFavorite);
    }

    @Transactional
    public LabelResponse updateLabel(Long labelId, LabelUpdateRequest labelUpdateRequest, Long memberId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(()->new IllegalArgumentException("라벨이 존재하지 않습니다."));

        Member member = memberRepository.getReferenceById(memberId);

        if(!label.getMember().getId().equals(memberId)) {
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
        }else{
            favoriteRepository.deleteByMemberIdAndTargetTypeAndTargetId(member.getId(),TargetType.LABEL,label.getId());
        }

        label.updateLabel(labelUpdateRequest.getName(),
                labelUpdateRequest.getColor());
        return LabelResponse.from(label, labelUpdateRequest.isFavorite());
    }

    public ScrollResponse<LabelListResponse> getLabels(Long labelId, Long memberId, Pageable pageable) {
        List<Label> labels = labelRepository.searchLabelsByCondition(labelId,memberId,pageable);

        boolean hasNext = false;
        Long nextCursor = null;

        if(labels.size() > pageable.getPageSize()){
            hasNext = true;
            labels = labels.subList(0, pageable.getPageSize());
        }

        if(!labels.isEmpty()){
            nextCursor = labels.get(labels.size()-1).getId();
        }

        List<LabelListResponse> labelResponses =  labels.stream()
                .map(LabelListResponse::from)
                .toList();

        return ScrollResponse.<LabelListResponse>builder()
                .content(labelResponses)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    @Transactional
    public void deleteLabel(Long labelId, Long memberId){
        Label label = labelRepository.findById(labelId)
                        .orElseThrow(()->new IllegalArgumentException("검색된 라벨이 존재하지 않습니다."));

        if(!label.getMember().getId().equals(memberId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        labelRepository.deleteById(labelId);
    }

    public List<LabelListResponse> searchLabels(String keyword){
        return labelRepository.findByNameContaining(keyword)
                .stream().map(LabelListResponse::from)
                .toList();
    }
}
