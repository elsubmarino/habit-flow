package io.streak.habitflow.domain.label.service;

import io.streak.habitflow.domain.favorite.entity.Favorite;
import io.streak.habitflow.domain.favorite.repository.FavoriteRepository;
import io.streak.habitflow.domain.label.dto.request.LabelRequest;
import io.streak.habitflow.domain.label.dto.response.LabelResponse;
import io.streak.habitflow.domain.label.entity.Label;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.global.common.type.TargetType;
import io.streak.habitflow.global.error.ErrorCode;
import io.streak.habitflow.global.error.exception.BusinessException;
import io.streak.habitflow.global.util.HashidsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
        return LabelResponse.Detail.of(savedLabel, request.favorite(),savedLabel.getPublicId().toString());
    }

    @PreAuthorize("@labelAuth.canAccess(#publicLabelId)")
    public LabelResponse.Detail getLabelById(UUID publicLabelId, Long loginMemberId) {
        Label label = labelRepository.getOrThrowByPublicId(publicLabelId);
        boolean isFavorite = false;
        Optional<Favorite> favorite = favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
                loginMemberId,TargetType.LABEL, label.getId());
        if(favorite.isPresent()){
            isFavorite = true;
        }
        return LabelResponse.Detail.of(label,isFavorite,label.getPublicId().toString());
    }

    @PreAuthorize("@labelAuth.canAccess(#publicLabelId)")
    @Transactional
    public LabelResponse.Detail updateLabel(UUID publicLabelId, LabelRequest.Update request, Long loginMemberId) {
        Label label = labelRepository.getOrThrowByPublicId(publicLabelId);

        Member member = memberRepository.getReferenceById(loginMemberId);

        if(!label.getMember().getId().equals(loginMemberId)) {
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
        if(request.name().equals(label.getName()) &&
            request.color().equals(label.getColor())) {
            return LabelResponse.Detail.of(label, request.favorite(),label.getPublicId().toString());
        }

        label.updateLabel(request.name(),
                request.color());
        return LabelResponse.Detail.of(label, request.favorite(),label.getPublicId().toString());
    }

    public Slice<LabelResponse.Summary> getLabels(UUID lastPublicLabelId, Long memberId, Pageable pageable) {
        int pageSize = pageable.getPageSize();

        Long lastLabelId = labelRepository.getOrThrowByPublicId(lastPublicLabelId)
                .getId();

        List<Label> labels = labelRepository.findLabelsByMemberWithCursor(lastLabelId,memberId,pageable);

        boolean hasNext = false;
        if(labels.size() > pageSize){
            labels.remove(pageSize);
            hasNext = true;
        }

        List<LabelResponse.Summary> labelResponses =  labels.stream()
                .map(label->{
                    return LabelResponse.Summary.of(label,label.getPublicId().toString());
                })
                .toList();

        return new SliceImpl<>(labelResponses, pageable, hasNext);
    }

    @PreAuthorize("@labelAuth.canAccess(#publicLabelId)")
    @Transactional
    public void deleteLabel(UUID publicLabelId, Long loginMemberId){
        Label label = labelRepository.getOrThrowByPublicId(publicLabelId);

        if(!label.getMember().getId().equals(loginMemberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        labelRepository.deleteById(label.getId());
    }

    @Transactional
    @PreAuthorize("@labelAuth.canAccess(#publicLabelId)")
    public LabelResponse.Summary updateSortOrder(UUID publicLabelId, LabelRequest.UpdateSortOrder updateSortOrder, Long loginMemberId){
        Label label = labelRepository.getOrThrowByPublicId(publicLabelId);

        Favorite favorite = favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
                loginMemberId,TargetType.LABEL,label.getId()
        ).orElse(null);

        if(Objects.equals(label.getSortOrder(), updateSortOrder.sortOrder())){
            return LabelResponse.Summary.of(label, favorite != null,label.getPublicId().toString());
        }
        label.updateSortOrder(updateSortOrder.sortOrder());

        return LabelResponse.Summary.of(label, favorite != null,label.getPublicId().toString());
    }
}
