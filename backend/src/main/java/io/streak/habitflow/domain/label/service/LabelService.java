package io.streak.habitflow.domain.label.service;

import io.streak.habitflow.domain.label.dto.LabelRequest;
import io.streak.habitflow.domain.label.dto.LabelResponse;
import io.streak.habitflow.domain.label.entity.Label;
import io.streak.habitflow.domain.label.repository.LabelRepository;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabelService {
    private final LabelRepository labelRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public LabelResponse createLabel(LabelRequest labelRequest, UserDetails userDetails) {
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new IllegalArgumentException("멤버가 존재하지 않습니다."));
        Label label = Label.builder()
                .name(labelRequest.getName())
                .color(labelRequest.getColor())
                .member(member)
                .build();
        return LabelResponse.from(labelRepository.save(label));
    }

    public List<LabelResponse> getLabels(UserDetails userDetails) {
        String email  = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                        .orElseThrow(()->new IllegalArgumentException("멤버를 찾을 수 없습니다."));
        List<Label> labels = labelRepository.findByMemberId(member.getId());
        return labels.stream()
                .map(LabelResponse::from)
                .toList();
    }

    @Transactional
    public LabelResponse updateLabel(Long id,LabelRequest labelRequest, UserDetails userDetails) {
        Label label = labelRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("라벨이 존재하지 않습니다."));

        if(!label.getMember().getEmail().equals(userDetails.getUsername())) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        label.updateLabel(labelRequest.getName(), labelRequest.getColor());
        return LabelResponse.from(label);
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

    public List<LabelResponse> searchLabels(String keyword){
        return labelRepository.findByNameContaining(keyword)
                .stream().map(LabelResponse::from)
                .toList();
    }
}
