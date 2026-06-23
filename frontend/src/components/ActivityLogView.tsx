import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { Project } from '../store/habitSlice';
import { fetchActivityLogs, type ActivityLogSearchParams } from '../api/activityApi';
import { ACTIVITY_LOG_PAGE_SIZE } from '../api/pagination';
import { activityLogSearchKey } from '../api/activityLogSearch';
import { fetchMember } from '../api/memberApi';
import type { ActivityType, EntityId } from '../api/types';
import { useInfiniteScroll } from '../hooks/useInfiniteScroll';
import { formatActivityDateHeader, formatActivityRelativeTime } from '../utils/activityLog';
import {
    activityBadge,
    formatActivityLogMessage,
    isSelfActivityActor,
    resolveCurrentMemberId,
    type ActivityLogEntry,
} from '../utils/activityLogMessages';
import { applyMemberProfile } from '../utils/userProfile';
import ActivityLogCheckboxFilter from './ActivityLogCheckboxFilter';
import ActivityLogDateRangePicker from './ActivityLogDateRangePicker';

const ACTOR_AVATAR_COLORS = ['#4073ff', '#299438', '#db4c3f', '#eb8909', '#8b5cf6'];

function actorAvatarColor(actorId: EntityId): string {
    let hash = 0;
    for (let i = 0; i < actorId.length; i++) {
        hash = ((hash << 5) - hash + actorId.charCodeAt(i)) | 0;
    }
    return ACTOR_AVATAR_COLORS[Math.abs(hash) % ACTOR_AVATAR_COLORS.length]!;
}

interface ActivityLogViewProps {
    projects: Project[];
    onOpenTask?: (taskId: EntityId) => void;
}

const ACTIVITY_FILTER_OPTIONS: { value: ActivityType; label: string }[] = [
    { value: 'ADDED', label: '추가' },
    { value: 'COMPLETED', label: '완료' },
    { value: 'UNCOMPLETED', label: '완료 취소' },
    { value: 'INVITED', label: '초대' },
    { value: 'JOINED', label: '합류' },
    { value: 'MOVED', label: '이동' },
    { value: 'UPDATED', label: '수정' },
    { value: 'DELETED', label: '삭제' },
];

function buildSearchParams(
    selectedProjectIds: Set<EntityId>,
    selectedActivityTypes: Set<ActivityType>,
    fromDate: string | null,
    toDate: string | null,
): ActivityLogSearchParams | undefined {
    const params: ActivityLogSearchParams = {};
    let hasFilter = false;

    if (selectedProjectIds.size > 0) {
        params.targetType = 'PROJECT';
        params.targetIds = [...selectedProjectIds];
        hasFilter = true;
    }
    if (selectedActivityTypes.size > 0) {
        params.activityType = [...selectedActivityTypes];
        hasFilter = true;
    }
    if (fromDate) {
        params.fromDate = fromDate;
        if (toDate && toDate !== fromDate) {
            params.toDate = toDate;
        }
        hasFilter = true;
    }

    return hasFilter ? params : undefined;
}

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
    const [selectedProjectIds, setSelectedProjectIds] = useState<Set<EntityId>>(() => new Set());
    const [selectedActivityTypes, setSelectedActivityTypes] = useState<Set<ActivityType>>(() => new Set());
    const [fromDate, setFromDate] = useState<string | null>(null);
    const [toDate, setToDate] = useState<string | null>(null);

    const projectOptions = useMemo(
        () => projects.map(project => ({ value: project.id, label: project.name })),
        [projects],
    );

    const searchParams = useMemo(
        () => buildSearchParams(selectedProjectIds, selectedActivityTypes, fromDate, toDate),
        [selectedProjectIds, selectedActivityTypes, fromDate, toDate],
    );
    const searchKey = activityLogSearchKey(searchParams);

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
        setItems([]);
        setHasNext(false);
        setNextCursor(null);

        void fetchActivityLogs(undefined, ACTIVITY_LOG_PAGE_SIZE, searchParams)
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
    }, [searchKey, searchParams]);

    const handleLoadMore = useCallback(() => {
        if (nextCursor == null || loadMoreStatus === 'loading') return;

        setLoadMoreStatus('loading');
        void fetchActivityLogs(nextCursor, ACTIVITY_LOG_PAGE_SIZE, searchParams)
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
    }, [loadMoreStatus, nextCursor, searchParams]);

    const loadMoreSentinelRef = useInfiniteScroll(
        !loading && hasNext,
        hasNext,
        loadMoreStatus === 'loading',
        handleLoadMore,
        scrollBodyRef,
    );

    const grouped = useMemo(() => {
        const map = new Map<string, ActivityLogEntry[]>();
        for (const entry of items) {
            const key = entry.createdAt.slice(0, 10);
            if (!map.has(key)) map.set(key, []);
            map.get(key)!.push(entry);
        }
        return [...map.entries()].sort(([a], [b]) => b.localeCompare(a));
    }, [items]);

    const exportMarkdown = () => {
        const exportedAt = new Date().toLocaleString('ko-KR');
        const lines = [
            '# HabitFlow 활동 보고',
            '',
            `-보낸 시각: ${exportedAt}`,
            `- 총 ${items.length}건`,
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
                    <ActivityLogCheckboxFilter
                        icon="#"
                        allLabel="모든 프로젝트"
                        options={projectOptions}
                        selected={selectedProjectIds}
                        onChange={setSelectedProjectIds}
                    />
                    <ActivityLogCheckboxFilter
                        icon="≡"
                        allLabel="모든 활동"
                        options={ACTIVITY_FILTER_OPTIONS}
                        selected={selectedActivityTypes}
                        onChange={setSelectedActivityTypes}
                    />
                    <ActivityLogDateRangePicker
                        fromDate={fromDate}
                        toDate={toDate}
                        onChange={({ fromDate: nextFrom, toDate: nextTo }) => {
                            setFromDate(nextFrom);
                            setToDate(nextTo);
                        }}
                    />
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
