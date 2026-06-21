import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { Project } from '../store/habitSlice';
import { fetchActivityLogs } from '../api/activityApi';
import { fetchMember } from '../api/memberApi';
import type { ActivityType, EntityId } from '../api/types';
import { useInfiniteScroll } from '../hooks/useInfiniteScroll';
import { formatActivityDateHeader, formatActivityRelativeTime } from '../utils/activityLog';
import {
    activityBadge,
    formatActivityLogMessage,
    isSelfActivityActor,
    matchesActivityProjectFilter,
    resolveCurrentMemberId,
    type ActivityLogEntry,
} from '../utils/activityLogMessages';
import { applyMemberProfile } from '../utils/userProfile';

const ACTOR_AVATAR_COLORS = ['#4073ff', '#299438', '#db4c3f', '#eb8909', '#8b5cf6'];

function actorAvatarColor(actorId: EntityId): string {
    let hash = 0;
    for (let i = 0; i < actorId.length; i++) {
        hash = ((hash << 5) - hash + actorId.charCodeAt(i)) | 0;
    }
    return ACTOR_AVATAR_COLORS[Math.abs(hash) % ACTOR_AVATAR_COLORS.length]!;
}

type ProjectFilter = 'all' | EntityId;
type ActivityFilter = 'all' | ActivityType;
type DateFilter = 'all' | 'today' | 'week';

interface ActivityLogViewProps {
    projects: Project[];
    onOpenTask?: (taskId: EntityId) => void;
}

const ACTIVITY_FILTER_OPTIONS: { value: ActivityFilter; label: string }[] = [
    { value: 'all', label: '모든 활동' },
    { value: 'ADDED', label: '추가' },
    { value: 'COMPLETED', label: '완료' },
    { value: 'UNCOMPLETED', label: '완료 취소' },
    { value: 'INVITED', label: '초대' },
    { value: 'JOINED', label: '합류' },
    { value: 'MOVED', label: '이동' },
    { value: 'UPDATED', label: '수정' },
    { value: 'DELETED', label: '삭제' },
];

const ActivityLogView: React.FC<ActivityLogViewProps> = ({ projects }) => {
    const [profileVersion, setProfileVersion] = useState(0);
    const memberId = useMemo(() => resolveCurrentMemberId(), [profileVersion]);
    const scrollBodyRef = useRef<HTMLDivElement>(null);
    const [items, setItems] = useState<ActivityLogEntry[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadMoreStatus, setLoadMoreStatus] = useState<'idle' | 'loading' | 'failed'>('idle');
    const [hasNext, setHasNext] = useState(false);
    const [nextCursor, setNextCursor] = useState<EntityId | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [projectFilter, setProjectFilter] = useState<ProjectFilter>('all');
    const [activityFilter, setActivityFilter] = useState<ActivityFilter>('all');
    const [dateFilter, setDateFilter] = useState<DateFilter>('all');

    useEffect(() => {
        let active = true;
        void fetchMember()
            .then(member => {
                if (!active) return;
                applyMemberProfile(member);
                setProfileVersion(version => version + 1);
            })
            .catch(() => undefined);

        return () => {
            active = false;
        };
    }, []);

    useEffect(() => {
        let active = true;
        setLoading(true);
        setLoadMoreStatus('idle');

        void fetchActivityLogs()
            .then(page => {
                if (!active) return;
                setItems(page.content);
                setHasNext(page.hasNext);
                setNextCursor(page.nextCursor);
                setError(null);
            })
            .catch(err => {
                if (!active) return;
                setError(err instanceof Error ? err.message : '활동 내역을 불러오지 못했습니다.');
                setItems([]);
                setHasNext(false);
                setNextCursor(null);
            })
            .finally(() => {
                if (active) setLoading(false);
            });

        return () => {
            active = false;
        };
    }, []);

    const handleLoadMore = useCallback(() => {
        if (nextCursor == null || loadMoreStatus === 'loading') return;

        setLoadMoreStatus('loading');
        void fetchActivityLogs(nextCursor)
            .then(page => {
                setItems(prev => {
                    const existingIds = new Set(prev.map(item => item.id));
                    const appended = page.content.filter(item => !existingIds.has(item.id));
                    return [...prev, ...appended];
                });
                setHasNext(page.hasNext);
                setNextCursor(page.nextCursor);
                setLoadMoreStatus('idle');
            })
            .catch(err => {
                setError(err instanceof Error ? err.message : '활동 내역을 더 불러오지 못했습니다.');
                setLoadMoreStatus('failed');
            });
    }, [loadMoreStatus, nextCursor]);

    const loadMoreSentinelRef = useInfiniteScroll(
        !loading && hasNext,
        hasNext,
        loadMoreStatus === 'loading',
        handleLoadMore,
        scrollBodyRef,
    );

    const filtered = useMemo(() => {
        const now = new Date();
        const todayKey = now.toISOString().slice(0, 10);
        const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);

        return items.filter(entry => {
            if (projectFilter !== 'all' && !matchesActivityProjectFilter(entry, projectFilter)) {
                return false;
            }
            if (activityFilter !== 'all' && entry.activityType !== activityFilter) return false;
            const created = new Date(entry.createdAt);
            if (dateFilter === 'today' && entry.createdAt.slice(0, 10) !== todayKey) return false;
            if (dateFilter === 'week' && created < weekAgo) return false;
            return true;
        });
    }, [items, projectFilter, activityFilter, dateFilter]);

    const grouped = useMemo(() => {
        const map = new Map<string, ActivityLogEntry[]>();
        for (const entry of filtered) {
            const key = entry.createdAt.slice(0, 10);
            if (!map.has(key)) map.set(key, []);
            map.get(key)!.push(entry);
        }
        return [...map.entries()].sort(([a], [b]) => b.localeCompare(a));
    }, [filtered]);

    const exportMarkdown = () => {
        const exportedAt = new Date().toLocaleString('ko-KR');
        const lines = [
            '# HabitFlow 활동 보고',
            '',
            `-보낸 시각: ${exportedAt}`,
            `- 총 ${filtered.length}건`,
            '',
        ];

        for (const [dateKey, entries] of grouped) {
            lines.push(`## ${formatActivityDateHeader(dateKey, entries.length)}`);
            lines.push('');
            for (const entry of entries) {
                const time = new Date(entry.createdAt).toLocaleTimeString('ko-KR', {
                    hour: '2-digit',
                    minute: '2-digit',
                });
                lines.push(`- ${time} ${formatActivityLogMessage(entry, memberId)}`);
            }
            lines.push('');
        }

        const blob = new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'habitflow-activity.md';
        a.click();
        URL.revokeObjectURL(url);
    };

    return (
        <div className="activity-log-page">
            <header className="activity-log-header">
                <div className="activity-log-title-row">
                    <h1 className="activity-log-title">보고: 전체</h1>
                    <button type="button" className="activity-export-btn" onClick={exportMarkdown}>
                        보내기
                    </button>
                </div>

                <div className="activity-filters">
                    <label className="activity-filter">
                        <span className="activity-filter-icon" aria-hidden>▦</span>
                        <select disabled value="all">
                            <option value="all">모든 작업 영역</option>
                        </select>
                    </label>
                    <label className="activity-filter">
                        <span className="activity-filter-icon" aria-hidden>#</span>
                        <select
                            value={String(projectFilter)}
                            onChange={e => {
                                const v = e.target.value;
                                setProjectFilter(v === 'all' ? 'all' : v);
                            }}
                        >
                            <option value="all">모든 프로젝트</option>
                            {projects.map(p => (
                                <option key={p.id} value={String(p.id)}>
                                    {p.name}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label className="activity-filter">
                        <span className="activity-filter-icon" aria-hidden>≡</span>
                        <select
                            value={activityFilter}
                            onChange={e => setActivityFilter(e.target.value as ActivityFilter)}
                        >
                            {ACTIVITY_FILTER_OPTIONS.map(opt => (
                                <option key={opt.value} value={opt.value}>
                                    {opt.label}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label className="activity-filter">
                        <span className="activity-filter-icon" aria-hidden>📅</span>
                        <select
                            value={dateFilter}
                            onChange={e => setDateFilter(e.target.value as DateFilter)}
                        >
                            <option value="all">모든 날짜</option>
                            <option value="today">오늘</option>
                            <option value="week">지난 7일</option>
                        </select>
                    </label>
                </div>
            </header>

            <div ref={scrollBodyRef} className="activity-log-body">
                {loading && <p className="activity-empty">불러오는 중…</p>}
                {error && <p className="activity-empty">{error}</p>}
                {!loading && !error && grouped.length === 0 ? (
                    <p className="activity-empty">활동 내역이 없습니다.</p>
                ) : (
                    grouped.map(([dateKey, entries]) => (
                        <section key={dateKey} className="activity-day-group">
                            <h2 className="activity-day-title">
                                {formatActivityDateHeader(dateKey, entries.length)}
                            </h2>
                            <ul className="activity-list" key={memberId ?? 'guest'}>
                                {entries.map(entry => {
                                    const isSelf = isSelfActivityActor(entry, memberId);
                                    const actorInitial = isSelf
                                        ? '당'
                                        : entry.actor.name.charAt(0).toUpperCase();

                                    return (
                                    <li key={entry.id} className="activity-row">
                                        <span className="activity-avatar-wrap">
                                            <span
                                                className="activity-avatar"
                                                style={{ backgroundColor: actorAvatarColor(entry.actor.id) }}
                                            >
                                                {actorInitial}
                                            </span>
                                            <span
                                                className={`activity-badge badge-${entry.activityType.toLowerCase()}`}
                                                aria-hidden
                                            >
                                                {activityBadge(entry.activityType)}
                                            </span>
                                        </span>
                                        <p className="activity-text">
                                            {formatActivityLogMessage(entry, memberId)}
                                        </p>
                                        <time className="activity-time" dateTime={entry.createdAt}>
                                            {formatActivityRelativeTime(entry.createdAt)}
                                        </time>
                                    </li>
                                    );
                                })}
                            </ul>
                        </section>
                    ))
                )}
                {!loading && hasNext && (
                    <div ref={loadMoreSentinelRef} className="tasks-scroll-sentinel" aria-hidden="true">
                        {loadMoreStatus === 'loading' && (
                            <p className="tasks-scroll-loading">불러오는 중…</p>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default ActivityLogView;
