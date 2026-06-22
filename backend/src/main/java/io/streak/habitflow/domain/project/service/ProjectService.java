package io.streak.habitflow.domain.project.service;

import io.streak.habitflow.domain.activitylog.dto.ChangeSet;
import io.streak.habitflow.domain.favorite.entity.Favorite;
import io.streak.habitflow.domain.favorite.repository.FavoriteRepository;
import io.streak.habitflow.domain.favorite.type.TargetType;
import io.streak.habitflow.domain.member.entity.Member;
import io.streak.habitflow.domain.member.repository.MemberRepository;
import io.streak.habitflow.domain.member.service.MailService;
import io.streak.habitflow.domain.project.dto.query.ProjectSearchSummaryQuery;
import io.streak.habitflow.domain.project.dto.query.ProjectSummaryQuery;
import io.streak.habitflow.domain.project.dto.request.ProjectRequest;
import io.streak.habitflow.domain.project.dto.response.ProjectResponse;
import io.streak.habitflow.domain.project.entity.Project;
import io.streak.habitflow.domain.project.entity.ProjectMember;
import io.streak.habitflow.domain.project.event.ProjectAcceptEvent;
import io.streak.habitflow.domain.project.event.ProjectInvitationEvent;
import io.streak.habitflow.domain.project.repository.ProjectMemberRepository;
import io.streak.habitflow.domain.project.repository.ProjectRepository;
import io.streak.habitflow.domain.task.event.TaskChangedEvent;
import io.streak.habitflow.domain.task.type.ActivityType;
import io.streak.habitflow.global.aop.CheckOwnership;
import io.streak.habitflow.global.util.HashidsProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final FavoriteRepository favoriteRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RedisTemplate<String, String> redisTemplate;
    private final MailService mailService;
    private final HashidsProvider hashidsProvider;

    private static final String INVITE_TOKEN_PREFIX = "PROJECT_INVITE:";
    private static final long INVITE_EXPIRATION_HOURS = 24L;

    @Transactional
    public ProjectResponse.Detail createProject(ProjectRequest.Create request,
                                         Long memberId) {
        Member member = memberRepository.getReferenceById(memberId);

        long projectCount = projectMemberRepository.countByMember(member);
        if (projectCount > 500) {
            throw new IllegalArgumentException("보유하고 있는 프로젝트가 500개 이상이므로 더 이상 생성할 수 없습니다.");
        }

        Long maxSortOrder = projectRepository.findMaxSortOrder(memberId, request.parentId());
        Long nextOrderOrder = maxSortOrder + 1024L;

        Project.ProjectBuilder projectBuilder = Project.builder()
                .name(request.name())
                .color(request.color())
                .accessType(request.accessType())
                .layoutType(request.layoutType())
                .sortOrder(nextOrderOrder);

        if(request.parentId() != null){
            Project parentProject = projectRepository.findById(request.parentId())
                    .orElseThrow(()->new EntityNotFoundException("부모 프로젝트가 존재하지 않습니다."));
            projectBuilder.parent(parentProject);
        }

        Project project = projectBuilder.build();

        Project savedProject = projectRepository.save(project);



        ProjectMember projectMember = ProjectMember.builder()
                .project(savedProject)
                .member(member)
                .build();
        projectMemberRepository.save(projectMember);

        if(request.favorite()){
            Favorite favorite = Favorite.builder()
                    .targetType(TargetType.PROJECT)
                    .targetId(savedProject.getId())
                    .member(member)
                    .build();
            favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
                    member.getId(),
                    TargetType.PROJECT,
                    savedProject.getId()
            ).orElseGet(()->favoriteRepository.save(favorite));
        }


        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                project.getId(),
                memberId,
                io.streak.habitflow.domain.task.type.TargetType.PROJECT,
                ActivityType.ADDED,
                project.getName(),
                Collections.emptyList()
        ));
        String encodedId = hashidsProvider.encode(savedProject.getId());
        return ProjectResponse.Detail.of(savedProject, request.favorite(),encodedId);
    }

    @Transactional
    @CheckOwnership(type="PROJECT")
    public ProjectResponse.Detail updateProject(ProjectRequest.Create request, Long projectId, Long memberId) {
        Project project =  projectRepository.getOrThrow(projectId);
        String oldProjectName = project.getName();

        Member member = memberRepository.getReferenceById(memberId);

        Project parentProject = null;
        if(request.parentId() != null){
            parentProject = projectRepository.findById(request.parentId())
                    .orElseThrow(()->new EntityNotFoundException("부모 프로젝트가 존재하지 않습니다."));
        }

        if(request.favorite()){
            Favorite favorite = Favorite.builder()
                    .targetType(TargetType.PROJECT)
                    .targetId(project.getId())
                    .member(member)
                    .build();
            favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
                    member.getId(),
                    TargetType.PROJECT,
                    project.getId()
            ).orElseGet(()->favoriteRepository.save(favorite));
        }else{
            favoriteRepository.deleteByMemberIdAndTargetTypeAndTargetId(member.getId(),TargetType.PROJECT,project.getId());
        }

        String encodedId = hashidsProvider.encode(project.getId());
        if(request.name().equals(project.getName()) &&
                request.color().equals(project.getColor()) &&
                request.accessType() == project.getAccessType() &&
                request.layoutType() == project.getLayoutType() &&
                request.parentId() != null && Objects.equals(request.parentId(),project.getParent().getId())){
            return ProjectResponse.Detail.of(project,request.favorite(),encodedId);
        }

        project.updateProject(request.name(),
                request.color(),
                request.accessType(),
                request.layoutType(),
                parentProject);

        if(request.name() != null && !request.name().equals(oldProjectName)){
            List<ChangeSet> changeSets = new ArrayList<>();
            changeSets.add(new ChangeSet("name",oldProjectName,request.name()));
            applicationEventPublisher.publishEvent(new TaskChangedEvent(
                    project.getId(),
                    memberId,
                    io.streak.habitflow.domain.task.type.TargetType.PROJECT,
                    ActivityType.UPDATED,
                    request.name(),
                    changeSets
            ));
        }
        return ProjectResponse.Detail.of(project,request.favorite(),encodedId);
    }

    public ProjectResponse.Detail getProjectById(Long projectId, Long memberId) {
       Project project = projectRepository.getOrThrow(projectId);
       boolean isFavorite = false;
       Optional<Favorite> favorite = favoriteRepository.findByMemberIdAndTargetTypeAndTargetId(
               memberId, TargetType.PROJECT, project.getId());
       if(favorite.isPresent()){
           isFavorite = true;
       }
       String encodedId = hashidsProvider.encode(project.getId());
       return ProjectResponse.Detail.of(project,isFavorite,encodedId);
    }

    public List<ProjectResponse.Summary> getProjectsByMember(Long memberId) {
        List<ProjectSummaryQuery> projectListQueries = projectRepository.findByMemberId(memberId);
        return projectListQueries.stream()
                .map(query ->{
                    String encodedId = hashidsProvider.encode(query.id());
                    return ProjectResponse.Summary.of(query,encodedId);
                })
                .toList();
    }



    @Transactional
    @CheckOwnership(type="PROJECT")
    @SuppressWarnings("unused")
    public void deleteProject(Long projectId, Long memberId) {
        Project project = projectRepository.getReferenceById(projectId);
        projectMemberRepository.deleteByProjectId(projectId);
        favoriteRepository.deleteByTargetTypeAndTargetId(TargetType.PROJECT, projectId);
        projectRepository.deleteById(projectId);

        applicationEventPublisher.publishEvent(new TaskChangedEvent(
                project.getId(),
                memberId,
                io.streak.habitflow.domain.task.type.TargetType.PROJECT,
                ActivityType.DELETED,
                project.getName(),
                Collections.emptyList()
        ));

    }

    public List<ProjectResponse.Summary> searchProjects(String keyword, Long memberId, Pageable pageable) {
        List<ProjectSearchSummaryQuery> projectListQueries = projectRepository.searchKeyword(keyword,memberId, pageable);
        return projectListQueries.stream()
                .map(query->{
                    String encodedId = hashidsProvider.encode(query.id());
                    return ProjectResponse.Summary.ofSearch(query,encodedId);
                })
                .toList();
    }

    @Transactional
    @CheckOwnership(type="PROJECT")
    public void invite(ProjectRequest.Invite inviteRequest, Long projectId, Long memberId){
        Project project = projectRepository.getOrThrow(projectId);
        Member inviter = memberRepository.getOrThrow(memberId);

        List<String> inviteEmails  = inviteRequest.emails();
        if(inviteEmails == null || inviteEmails.isEmpty()) return;

        for(String email: inviteEmails){
            memberRepository.findByEmail(email).ifPresent(targetMember->{
                if(projectMemberRepository.existsByProjectAndMember(project,targetMember)){
                    throw new IllegalArgumentException(email+" 님은 이미 프로젝트 멤버입니다.");
                }
            });

            String InvitationToken = UUID.randomUUID().toString();
            String redisValue = project.getId()+":"+email+":"+inviter.getId()+":"+inviter.getName();
            redisTemplate.opsForValue().set(
                    INVITE_TOKEN_PREFIX + InvitationToken,
                    redisValue,
                    INVITE_EXPIRATION_HOURS,
                    TimeUnit.HOURS
            );

            mailService.sendProjectInvitationMail(email, project.getName(), inviter.getName(), InvitationToken);
        }

        List<Member> inviteMembers = memberRepository.findByEmailIn(inviteEmails);

        List<ProjectInvitationEvent.MemberInfo> inviteeInfos = inviteMembers.stream()
                .map((Member m) ->new ProjectInvitationEvent.MemberInfo(m.getId(),m.getName()))
                .toList();

        applicationEventPublisher.publishEvent(new ProjectInvitationEvent(
                project.getId(),
                project.getName(),
                inviter.getId(),
                inviter.getName(),
                inviteeInfos
        ));
    }

    @Transactional
    public void acceptInvitation(String token, Long loginMemberId){
        String redisKey = INVITE_TOKEN_PREFIX+token;
        String redisValue = redisTemplate.opsForValue().get(redisKey);
        if(redisValue == null){
            throw new IllegalArgumentException("만료되었거나 유효하지 않은 초대 링크입니다.");
        }

        String[]parts = redisValue.split(":");
        Long projectId = Long.parseLong(parts[0]);
        String targetEmail = parts[1];
        Long inviterId = Long.parseLong(parts[2]);
        String inviterName = parts[3];

        Member loginMember = memberRepository.getOrThrow(loginMemberId);
        if(!loginMember.getEmail().equals(targetEmail)){
            throw new AccessDeniedException("본인에게 온 초대장만 수락할 수 있습니다.");
        }

        Project project = projectRepository.getOrThrow(projectId);

        if(!projectMemberRepository.existsByProjectAndMember(project, loginMember)){
            ProjectMember projectMember = ProjectMember.builder()
                    .project(project)
                    .member(loginMember)
                    .build();
            projectMemberRepository.save(projectMember);
        }

        redisTemplate.delete(redisKey);

        applicationEventPublisher.publishEvent(new ProjectAcceptEvent(
                project.getId(),
                project.getName(),
                loginMember.getId(),
                loginMember.getName(),
                inviterId,
                inviterName
        ));
    }

    @CheckOwnership(type="PROJECT")
    @SuppressWarnings("unused")
    public List<ProjectResponse.Member> getProjectMembers(Long projectId,Long memberId) {
        Project project = projectRepository.getOrThrow(projectId);
        List<ProjectMember> projectMembers = projectMemberRepository.findByProject(project);
        return projectMembers.stream()
                .map(projectMember->{
                    String encodedMemberId = hashidsProvider.encode(projectMember.getMember().getId());
                    return ProjectResponse.Member.of(projectMember,encodedMemberId);
                })
                .toList();
    }

    @Transactional
    @CheckOwnership(type="PROJECT")
    @SuppressWarnings("unused")
    public void deleteProjectMember(Long projectId,Long memberId, ProjectRequest.DeleteMember request) {
        Project project = projectRepository.getOrThrow(projectId);
        Long realMemberId = hashidsProvider.decode(request.memberId());
        Member member = memberRepository.getReferenceById(realMemberId);
        projectMemberRepository.deleteByProjectAndMember(project,member);
    }

    @Transactional
    @CheckOwnership(type="PROJECT")
    public ProjectResponse.Summary updateSortOrder(Long projectId, ProjectRequest.UpdateSortOrder updateSortOrder, Long memberId){
        Project project = projectRepository.getReferenceById(projectId);
        String encodedId = hashidsProvider.encode(project.getId());

        if(Objects.equals(project.getSortOrder(), updateSortOrder.sortOrder())){
            return ProjectResponse.Summary.of(project, encodedId);
        }
        project.updateSortOrder(updateSortOrder.sortOrder());

        return ProjectResponse.Summary.of(project, encodedId);
    }
}
