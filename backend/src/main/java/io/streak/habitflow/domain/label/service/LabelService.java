package io.streak.habitflow.domain.label.service;

import io.streak.habitflow.domain.favorite.entity.Favorite;
import io.streak.habitflow.domain.favorite.repository.FavoriteRepository;
import io.streak.habitflow.domain.favorite.type.TargetType;
import io.streak.habitflow.domain.label.dto.request.LabelRequest;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.entity.Label;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.access.AccessDeniedException;
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
    public LabelResponse.Detail createLabel(LabelRequest.Create request, Long memberId) {
        Member member = memberRepository.getReferenceById(memberId);
        Label label = Label.builder()
                .name(request.name())
                .color(request.color())
                .member(member)
                .build();

        Label savedLabel = labelRepository.save(label);

        if(request.favorite()){
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
        return LabelResponse.Detail.of(savedLabel, request.favorite());
    }

    public LabelResponse.Detail getLabelById(Long labelId, Long memberId) {
        Label label = labelRepository.getOrThrow(labelId);
        boolean isFavorite = false;
        Optional<Favorite> favorite = favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
                memberId,TargetType.LABEL, label.getId());
        if(favorite.isPresent()){
            isFavorite = true;
        }
        return LabelResponse.Detail.of(label,isFavorite);
    }

    @Transactional
    public LabelResponse.Detail updateLabel(Long labelId, LabelRequest.Update request, Long memberId) {
        Label label = labelRepository.getOrThrow(labelId);

        Member member = memberRepository.getReferenceById(memberId);

        if(!label.getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("수정 권한이 없습니다.");
        }

        if(request.favorite()) {
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

        label.updateLabel(request.name(),
                request.color());
        return LabelResponse.Detail.of(label, request.favorite());
    }

    public Slice<LabelResponse.Summary> getLabels(Long labelId, Long memberId, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<Label> labels = labelRepository.searchLabelsByCondition(labelId,memberId,pageable);

        boolean hasNext = false;
        if(labels.size() > pageSize){
            labels.remove(pageSize);
            hasNext = true;
        }

        List<LabelResponse.Summary> labelResponses =  labels.stream()
                .map(LabelResponse.Summary::from)
                .toList();

        return new SliceImpl<>(labelResponses, pageable, hasNext);
    }

    @Transactional
    public void deleteLabel(Long labelId, Long memberId){
        Label label = labelRepository.getOrThrow(labelId);

        if(!label.getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }
        labelRepository.deleteById(labelId);
    }
}
