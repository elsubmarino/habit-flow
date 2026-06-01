import { useEffect, useMemo, useState } from 'react';
import type { Project } from '../store/habitSlice';
import {
    activityDateKey,
    activityBadge,
    formatActivityDateHeader,
    formatActivityRelativeTime,
    readActivities,
    type ActivityEntry,
    type ActivityType,
} from '../utils/activityLog';
import { getUserProfile } from '../utils/userProfile';

interface ActivityLogViewProps {
    projects: Project[];
    onOpenTask: (taskId: number) => void;
}

type ProjectFilter = 'all' | number;
type ActivityFilter = 'all' | ActivityType;
type DateFilter = 'all' | 'today' | 'week';

const ACTIVITY_FILTER_OPTIONS: { value: ActivityFilter; label: string }[] = [
    { value: 'all', label: '모든 활동' },
    { value: 'added', label: '추가' },
    { value: 'completed', label: '완료' },
    { value: 'moved', label: '이동' },
    { value: 'date_changed', label: '날짜 변경' },
    { value: 'deleted', label: '삭제' },
];

function renderActivityText(entry: ActivityEntry, onOpenTask: (id: number) => void) {
    const taskBtn = (
        <button type="button" className="activity-task-link" onClick={() => onOpenTask(entry.taskId)}>
            {entry.taskName}
        </button>
    );
    switch (entry.type) {
        case 'added':
            return (
                <>
                    {entry.actor}님이 {taskBtn}을(를) 추가했습니다
                </>
            );
        case 'completed':
            return (
                <>
                    {entry.actor}님이 {taskBtn}을(를) 완료했습니다
                </>
            );
        case 'uncompleted':
            return (
                <>
                    {entry.actor}님이 {taskBtn} 완료를 취소했습니다
                </>
            );
        case 'deleted':
            return (
                <>
                    {entry.actor}님이 {taskBtn}을(를) 삭제했습니다
                </>
            );
        case 'moved':
            return (
                <>
                    {entry.actor}님이 {taskBtn}을(를) 이동시켰습니다
                </>
            );
        case 'date_changed':
            return (
                <>
                    {entry.actor}님이 {taskBtn}의 날짜를 {entry.meta ?? ''}(으)로 변경했습니다
                </>
            );
        default:
            return (
                <>
                    {entry.actor}님이 {taskBtn}을(를) 수정했습니다
                </>
            );
    }
}

const ActivityLogView: React.FC<ActivityLogViewProps> = ({ projects, onOpenTask }) => {
    const [items, setItems] = useState<ActivityEntry[]>(() => readActivities());
    const [projectFilter, setProjectFilter] = useState<ProjectFilter>('all');
    const [activityFilter, setActivityFilter] = useState<ActivityFilter>('all');
    const [dateFilter, setDateFilter] = useState<DateFilter>('all');
    const profile = getUserProfile();

    useEffect(() => {
        const reload = () => setItems(readActivities());
        reload();
        window.addEventListener('habitflow:activity', reload);
        return () => window.removeEventListener('habitflow:activity', reload);
    }, []);

    const filtered = useMemo(() => {
        const now = new Date();
        const todayKey = now.toISOString().slice(0, 10);
        const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);

        return items.filter(entry => {
            if (projectFilter !== 'all' && entry.projectId !== projectFilter) return false;
            if (activityFilter !== 'all' && entry.type !== activityFilter) return false;
            const created = new Date(entry.createdAt);
            if (dateFilter === 'today' && activityDateKey(entry.createdAt) !== todayKey) return false;
            if (dateFilter === 'week' && created < weekAgo) return false;
            return true;
        });
    }, [items, projectFilter, activityFilter, dateFilter]);

    const grouped = useMemo(() => {
        const map = new Map<string, ActivityEntry[]>();
        for (const entry of filtered) {
            const key = activityDateKey(entry.createdAt);
            if (!map.has(key)) map.set(key, []);
            map.get(key)!.push(entry);
        }
        return [...map.entries()].sort(([a], [b]) => b.localeCompare(a));
    }, [filtered]);

    const exportCsv = () => {
        const header = '날짜,사용자,활동,작업,프로젝트\n';
        const rows = filtered
            .map(e =>
                [
                    e.createdAt,
                    e.actor,
                    e.type,
                    `"${e.taskName.replace(/"/g, '""')}"`,
                    e.projectName ?? '',
                ].join(','),
            )
            .join('\n');
        const blob = new Blob([header + rows], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'habitflow-activity.csv';
        a.click();
        URL.revokeObjectURL(url);
    };

    return (
        <div className="activity-log-page">
            <header className="activity-log-header">
                <div className="activity-log-title-row">
                    <h1 className="activity-log-title">보고: 전체</h1>
                    <button type="button" className="activity-export-btn" onClick={exportCsv}>
                       보내기
                    </button>
                </div>

                <div className="activity-filters">
                    <label className="activity-filter">
                        <span className="activity-filter-icon" aria-hidden>
                            ▦
                        </span>
                        <select disabled value="all">
                            <option value="all">모든 작업 영역</option>
                        </select>
                    </label>
                    <label className="activity-filter">
                        <span className="activity-filter-icon" aria-hidden>
                            #
                        </span>
                        <select
                            value={String(projectFilter)}
                            onChange={e => {
                                const v = e.target.value;
                                setProjectFilter(v === 'all' ? 'all' : Number(v));
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
                        <span className="activity-filter-icon" aria-hidden>
                            👤
                        </span>
                        <select defaultValue="me">
                            <option value="me">{profile.displayName} 그리고 미할당됨</option>
                            <option value="all">모두</option>
                        </select>
                    </label>
                    <label className="activity-filter">
                        <span className="activity-filter-icon" aria-hidden>
                            ≡
                        </span>
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
                        <span className="activity-filter-icon" aria-hidden>
                            📅
                        </span>
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

            <div className="activity-log-body">
                {grouped.length === 0 ? (
                    <p className="activity-empty">활동 내역이 없습니다.</p>
                ) : (
                    grouped.map(([dateKey, entries]) => (
                        <section key={dateKey} className="activity-day-group">
                            <h2 className="activity-day-title">
                                {formatActivityDateHeader(dateKey, entries.length)}
                            </h2>
                            <ul className="activity-list">
                                {entries.map(entry => (
                                    <li key={entry.id} className="activity-row">
                                        <span className="activity-avatar-wrap">
                                            <span
                                                className="activity-avatar"
                                                style={{ background: entry.actorColor }}
                                            >
                                                {entry.actor.charAt(0).toUpperCase()}
                                            </span>
                                            <span
                                                className={`activity-badge badge-${entry.type}`}
                                                aria-hidden
                                            >
                                                {activityBadge(entry.type)}
                                            </span>
                                        </span>
                                        <div className="activity-content">
                                            <p className="activity-text">
                                                {renderActivityText(entry, onOpenTask)}
                                            </p>
                                            {entry.projectName && (
                                                <span className="activity-project">
                                                    {entry.projectName} #
                                                </span>
                                            )}
                                        </div>
                                        <time className="activity-time" dateTime={entry.createdAt}>
                                            {formatActivityRelativeTime(entry.createdAt)}
                                        </time>
                                    </li>
                                ))}
                            </ul>
                        </section>
                    ))
                )}
            </div>
        </div>
    );
};

export default ActivityLogView;
