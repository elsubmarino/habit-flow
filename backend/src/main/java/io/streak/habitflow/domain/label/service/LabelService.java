package io.streak.habitflow.domain.label.service;

import io.streak.habitflow.domain.favorite.entity.Favorite;
import io.streak.habitflow.domain.favorite.repository.FavoriteRepository;
import io.streak.habitflow.domain.label.dto.request.LabelRequest;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.entity.Label;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.global.aop.CheckOwnership;
import io.streak.habitflow.global.common.type.TargetType;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabelService {
    private final LabelRepository labelRepository;
    private final MemberRepository memberRepository;
    private final FavoriteRepository favoriteRepository;
    private final HashidsProvider hashidsProvider;

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
        String encodedId = hashidsProvider.encode(savedLabel.getId());
        return LabelResponse.Detail.of(savedLabel, request.favorite(),encodedId);
    }

    public LabelResponse.Detail getLabelById(Long labelId, Long memberId) {
        Label label = labelRepository.getOrThrow(labelId);
        boolean isFavorite = false;
        Optional<Favorite> favorite = favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
                memberId,TargetType.LABEL, label.getId());
        if(favorite.isPresent()){
            isFavorite = true;
        }
        String encodedId = hashidsProvider.encode(label.getId());
        return LabelResponse.Detail.of(label,isFavorite,encodedId);
    }

    @Transactional
    public LabelResponse.Detail updateLabel(Long labelId, LabelRequest.Update request, Long memberId) {
        Label label = labelRepository.getOrThrow(labelId);

        Member member = memberRepository.getReferenceById(memberId);

        if(!label.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
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
        String encodedId = hashidsProvider.encode(label.getId());
        if(request.name().equals(label.getName()) &&
            request.color().equals(label.getColor())) {
            return LabelResponse.Detail.of(label, request.favorite(),encodedId);
        }

        label.updateLabel(request.name(),
                request.color());
        return LabelResponse.Detail.of(label, request.favorite(),encodedId);
    }

    public Slice<LabelResponse.Summary> getLabelPage(Long labelId, Long memberId, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        List<Label> labels = labelRepository.findLabelsByMemberWithCursor(labelId,memberId,pageable);

        boolean hasNext = false;
        if(labels.size() > pageSize){
            labels.remove(pageSize);
            hasNext = true;
        }

        List<LabelResponse.Summary> labelResponses =  labels.stream()
                .map(label->{
                    String encodedId = hashidsProvider.encode(label.getId());
                    return LabelResponse.Summary.of(label,encodedId);
                })
                .toList();

        return new SliceImpl<>(labelResponses, pageable, hasNext);
    }

    @Transactional
    public void deleteLabel(Long labelId, Long memberId){
        Label label = labelRepository.getOrThrow(labelId);

        if(!label.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        labelRepository.deleteById(labelId);
    }

    @Transactional
    @CheckOwnership(type="LABEL")
    public LabelResponse.Summary updateSortOrder(Long labelId, LabelRequest.UpdateSortOrder updateSortOrder, Long memberId){
        Label label = labelRepository.getReferenceById(labelId);
        String encodedId = hashidsProvider.encode(label.getId());

        Favorite favorite = favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
                memberId,TargetType.LABEL,label.getId()
        ).orElse(null);

        if(Objects.equals(label.getSortOrder(), updateSortOrder.sortOrder())){
            return LabelResponse.Summary.of(label, favorite != null,encodedId);
        }
        label.updateSortOrder(updateSortOrder.sortOrder());

        return LabelResponse.Summary.of(label, favorite != null,encodedId);
    }
}
