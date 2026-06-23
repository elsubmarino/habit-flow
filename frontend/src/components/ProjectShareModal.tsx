import { useCallback, useEffect, useMemo, useState } from 'react';
import { getApiErrorMessage } from '../api/apiError';
import { fetchProjectMembers, inviteToProject, removeProjectMember } from '../api/projectApi';
import type { ProjectMemberListDto, EntityId } from '../api/types';
import { useDialog } from '../context/DialogContext';
import { useToast } from '../context/ToastContext';
import type { Project } from '../store/habitSlice';
import { useUserProfile } from '../hooks/useUserProfile';
import { ChevronDownIcon, CloseIcon, HelpCircleIcon, LockIcon } from './icons';

interface ProjectShareModalProps {
    project: Project;
    onClose: () => void;
}

interface ShareMember {
    id: string;
    memberId: EntityId | null;
    name: string;
    email: string;
    role: 'owner' | 'collaborator';
    isSelf?: boolean;
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function isValidEmail(value: string): boolean {
    return EMAIL_PATTERN.test(value.trim());
}

function memberInitial(name: string, email: string): string {
    const source = name.trim() || email.trim();
    return source.charAt(0).toUpperCase() || '?';
}

function toShareMember(dto: ProjectMemberListDto, selfEmail: string): ShareMember {
    const isSelf = dto.email.toLowerCase() === selfEmail.toLowerCase();
    return {
        id: dto.email,
        memberId: dto.memberId ?? null,
        name: dto.memberName,
        email: dto.email,
        role: isSelf ? 'owner' : 'collaborator',
        isSelf,
    };
}

const ProjectShareModal: React.FC<ProjectShareModalProps> = ({ project, onClose }) => {
    const { showToast, showErrorToast } = useToast();
    const { confirm } = useDialog();
    const profile = useUserProfile();
    const selfEmail = profile.email;
    const [draft, setDraft] = useState('');
    const [chipEmail, setChipEmail] = useState<string | null>(null);
    const [members, setMembers] = useState<ShareMember[]>([]);
    const [membersLoading, setMembersLoading] = useState(true);
    const [membersError, setMembersError] = useState<string | null>(null);
    const [accessOpen, setAccessOpen] = useState(false);
    const [inviteSubmitting, setInviteSubmitting] = useState(false);
    const [removingEmail, setRemovingEmail] = useState<string | null>(null);

    const loadMembers = useCallback(async () => {
        setMembersLoading(true);
        setMembersError(null);
        try {
            const data = await fetchProjectMembers(project.id);
            const mapped = data.map(dto => toShareMember(dto, selfEmail));
            mapped.sort((a, b) => Number(b.isSelf) - Number(a.isSelf));
            setMembers(mapped);
        } catch (err) {
            setMembersError(getApiErrorMessage(err, '멤버 목록을 불러오지 못했습니다.'));
            setMembers([]);
        } finally {
            setMembersLoading(false);
        }
    }, [project.id, selfEmail]);

    const draftTrimmed = draft.trim();
    const showInlineInvite = isValidEmail(draftTrimmed) && chipEmail == null;
    const showFooterInvite = chipEmail != null || showInlineInvite;

    useEffect(() => {
        void loadMembers();
    }, [loadMembers]);

    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [onClose]);

    const memberEmails = useMemo(
        () => new Set(members.map(m => m.email.toLowerCase())),
        [members],
    );

    const isOwner = useMemo(
        () => members.some(member => member.isSelf),
        [members],
    );

    const resetInviteInput = () => {
        setDraft('');
        setChipEmail(null);
    };

    const sendInvite = async (email: string) => {
        const normalized = email.trim().toLowerCase();
        if (!isValidEmail(normalized)) return;
        if (normalized === selfEmail.toLowerCase()) {
            showErrorToast('본인은 초대할 수 없습니다.');
            return;
        }
        if (memberEmails.has(normalized)) {
            showErrorToast('이미 프로젝트에 참여 중인 이메일입니다.');
            resetInviteInput();
            return;
        }

        setInviteSubmitting(true);
        try {
            await inviteToProject(project.id, [normalized]);
            resetInviteInput();
            showToast('초대 메일을 발송했습니다. 수신자가 메일에서 수락하면 프로젝트에 합류합니다.');
        } catch (err) {
            showErrorToast(getApiErrorMessage(err, '초대에 실패했습니다.'));
        } finally {
            setInviteSubmitting(false);
        }
    };

    const handleInlineInvite = () => {
        if (chipEmail) {
            void sendInvite(chipEmail);
            return;
        }
        if (showInlineInvite) {
            setChipEmail(draftTrimmed.toLowerCase());
            setDraft('');
        }
    };

    const handleFooterInvite = () => {
        if (chipEmail) {
            void sendInvite(chipEmail);
            return;
        }
        if (showInlineInvite) {
            void sendInvite(draftTrimmed);
        }
    };

    const handleRemoveMember = async (member: ShareMember) => {
        if (member.isSelf || removingEmail) return;

        if (!member.memberId) {
            showErrorToast('멤버 ID가 없어 제거할 수 없습니다.');
            return;
        }

        const displayName = member.name.trim() || member.email;
        if (!(await confirm({
            title: '참여자 제거',
            message: `"${displayName}"님을 이 프로젝트에서 제거할까요?\n제거된 사용자는 더 이상 프로젝트에 접근할 수 없습니다.`,
            confirmLabel: '제거',
            variant: 'danger',
        }))) {
            return;
        }

        setRemovingEmail(member.email);
        try {
            await removeProjectMember(project.id, member.memberId);
            setMembers(prev => prev.filter(item => item.email !== member.email));
            showToast(`${displayName}님을 프로젝트에서 제거했습니다.`);
        } catch (err) {
            showErrorToast(getApiErrorMessage(err, '참여자를 제거하지 못했습니다.'));
        } finally {
            setRemovingEmail(null);
        }
    };

    return (
        <div className="project-share-overlay" onClick={onClose}>
            <div
                className="project-share-modal"
                role="dialog"
                aria-labelledby="project-share-title"
                onClick={e => e.stopPropagation()}
            >
                <div className="project-share-header">
                    <h2 id="project-share-title" className="project-share-title">
                        <span className="project-share-hash" style={{ color: project.color }}>#</span>
                        {project.name}
                    </h2>
                    <button
                        type="button"
                        className="project-share-close"
                        aria-label="닫기"
                        onClick={onClose}
                    >
                        <CloseIcon />
                    </button>
                </div>

                <div className="project-share-invite-wrap">
                    <p className="project-share-invite-hint">
                        이메일로 초대장을 보냅니다. 수신자가 메일의 링크를 눌러 수락해야 합류합니다.
                    </p>
                    <div className="project-share-invite-input">
                        {chipEmail && (
                            <span className="project-share-chip">
                                <span className="project-share-chip-avatar">
                                    {memberInitial(chipEmail, chipEmail)}
                                </span>
                                <span className="project-share-chip-label">{chipEmail}</span>
                                <button
                                    type="button"
                                    className="project-share-chip-remove"
                                    aria-label="초대 대상 제거"
                                    onClick={() => setChipEmail(null)}
                                    disabled={inviteSubmitting}
                                >
                                    <CloseIcon />
                                </button>
                            </span>
                        )}
                        {!chipEmail && (
                            <input
                                type="email"
                                className="project-share-input"
                                placeholder="이메일 주소로 초대"
                                value={draft}
                                onChange={e => setDraft(e.target.value)}
                                onKeyDown={e => {
                                    if (e.key === 'Enter' && showInlineInvite && !inviteSubmitting) {
                                        e.preventDefault();
                                        handleInlineInvite();
                                    }
                                }}
                                disabled={inviteSubmitting}
                                autoFocus
                            />
                        )}
                        {showInlineInvite && (
                            <button
                                type="button"
                                className="project-share-inline-invite"
                                onClick={handleInlineInvite}
                                disabled={inviteSubmitting}
                            >
                                초대
                            </button>
                        )}
                    </div>
                </div>

                <section className="project-share-section">
                    <h3 className="project-share-section-label">접근</h3>
                    <div className="project-share-access">
                        <button
                            type="button"
                            className="project-share-access-btn"
                            onClick={() => setAccessOpen(v => !v)}
                        >
                            <span className="project-share-access-icon"><LockIcon /></span>
                            <span className="project-share-access-text">
                                <strong>비공개</strong>
                                <small>초대를 수락한 사람만 편집할 수 있습니다</small>
                            </span>
                            <span className="project-share-access-chevron">
                                <ChevronDownIcon />
                            </span>
                        </button>
                        {accessOpen && (
                            <div className="project-share-access-menu">
                                <button type="button" className="selected">비공개</button>
                                <button type="button" disabled>공개 (준비 중)</button>
                            </div>
                        )}
                    </div>
                </section>

                <section className="project-share-section">
                    <h3 className="project-share-section-label">이 프로젝트에</h3>
                    {membersLoading && (
                        <p className="project-share-member-status">멤버를 불러오는 중…</p>
                    )}
                    {!membersLoading && membersError && (
                        <p className="project-share-member-status error">{membersError}</p>
                    )}
                    {!membersLoading && !membersError && members.length === 0 && (
                        <p className="project-share-member-status">멤버가 없습니다.</p>
                    )}
                    {!membersLoading && !membersError && members.length > 0 && (
                        <ul className="project-share-member-list">
                            {members.map(member => (
                                <li key={member.id} className="project-share-member-row">
                                    <span
                                        className="project-share-member-avatar"
                                        style={{ background: member.isSelf ? project.color : '#eb8909' }}
                                    >
                                        {memberInitial(member.name, member.email)}
                                    </span>
                                    <div className="project-share-member-info">
                                        <p className="project-share-member-name">
                                            {member.name}
                                            {member.isSelf && <span className="project-share-you"> (나)</span>}
                                        </p>
                                        <p className="project-share-member-email">{member.email}</p>
                                    </div>
                                    <div className="project-share-member-actions">
                                        {member.isSelf ? (
                                            <span className="project-share-member-action muted">소유자</span>
                                        ) : (
                                            <>
                                                <span className="project-share-member-action muted">공동 작업자</span>
                                                {isOwner && (
                                                    <button
                                                        type="button"
                                                        className="project-share-remove-btn"
                                                        aria-label={`${member.name || member.email} 제거`}
                                                        title="프로젝트에서 제거"
                                                        disabled={removingEmail != null || inviteSubmitting}
                                                        onClick={() => void handleRemoveMember(member)}
                                                    >
                                                        {removingEmail === member.email ? (
                                                            <span className="project-share-remove-spinner" aria-hidden />
                                                        ) : (
                                                            <CloseIcon />
                                                        )}
                                                    </button>
                                                )}
                                            </>
                                        )}
                                    </div>
                                </li>
                            ))}
                        </ul>
                    )}
                </section>

                <div className="project-share-footer">
                    <button type="button" className="project-share-help-link">
                        <HelpCircleIcon />
                        공유에 대해 알아보기
                    </button>
                    <div className="project-share-footer-actions">
                        {showFooterInvite ? (
                            <>
                                <button
                                    type="button"
                                    className="project-share-cancel-btn"
                                    onClick={resetInviteInput}
                                    disabled={inviteSubmitting}
                                >
                                    취소
                                </button>
                                <button
                                    type="button"
                                    className="project-share-submit-btn"
                                    onClick={handleFooterInvite}
                                    disabled={inviteSubmitting}
                                >
                                    {inviteSubmitting ? '발송 중…' : '초대 메일 발송'}
                                </button>
                            </>
                        ) : (
                            <button
                                type="button"
                                className="project-share-cancel-btn"
                                onClick={onClose}
                            >
                                닫기
                            </button>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ProjectShareModal;
