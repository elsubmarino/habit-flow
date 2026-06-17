import { useVirtualizer, type VirtualItem } from '@tanstack/react-virtual';
import { memo, useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { habitRowKey, type Habit, type UpcomingDayBundle } from '../store/habitSlice';
import {
    addDays,
    formatMonthYear,
    formatUpcomingSectionTitle,
    getUpcomingCalendarMaxDate,
    getUpcomingTimelineDateKeys,
    getWeekStrip,
    isAfterToday,
    isWithinUpcomingCalendarRange,
    startOfWeekMonday,
    toISODate,
} from '../utils/date';
import HabitItem from './HabitItem';
import InlineAddTaskButton from './InlineAddTaskButton';
import OverdueTasksSection from './OverdueTasksSection';

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];
const EMPTY_DAY_HEIGHT = 30;
const TASK_ROW_BASE_HEIGHT = 44;
const TASK_ROW_DESCRIPTION_HEIGHT = 22;
const TASK_ROW_META_HEIGHT = 22;
const DAY_HEADER_HEIGHT = 28;
const ADD_BUTTON_HEIGHT = 36;
const SELECTED_EMPTY_EXTRA = 22;
const DAY_LOAD_MORE_HEIGHT = 40;
const COMPACT_EMPTY_DAY_HEIGHT = DAY_HEADER_HEIGHT + 4 + ADD_BUTTON_HEIGHT;

interface UpcomingTaskListProps {
    habits: Habit[];
    overdueHabits: Habit[];
    upcomingDays: Record<string, UpcomingDayBundle>;
    upcomingDayCounts: Record<string, number> | null;
    upcomingSummaryStatus: 'idle' | 'loading' | 'loaded' | 'failed';
    selectedDate: string;
    weekStartIso: string | null;
    jumpStatus: 'idle' | 'loading' | 'failed';
    scrollContainerRef?: React.RefObject<HTMLElement | null>;
    onJumpToDate: (iso: string) => void;
    onSelectDate: (iso: string) => void;
    onEnsureDay: (dateKey: string) => void;
    onLoadMoreDay: (dateKey: string) => void;
    onOpenDetails?: (habit: Habit) => void;
    onOpenProject?: (projectId: number) => void;
    onAddTask?: () => void;
    onTaskCompleted?: (habit: Habit) => void;
    onTaskDeleted?: (habitId: number) => void;
}

interface UpcomingDaySectionProps {
    dateKey: string;
    habits: Habit[];
    isSelected: boolean;
    dayBundle: UpcomingDayBundle | undefined;
    mayHaveTasks: boolean;
    onLoadMoreDay?: (dateKey: string) => void;
    onOpenDetails?: (habit: Habit) => void;
    onOpenProject?: (projectId: number) => void;
    onAddTask?: () => void;
    onTaskCompleted?: (habit: Habit) => void;
    onTaskDeleted?: (habitId: number) => void;
}

const UpcomingDaySection = memo(function UpcomingDaySection({
    dateKey,
    habits,
    isSelected,
    dayBundle,
    mayHaveTasks,
    onLoadMoreDay,
    onOpenDetails,
    onOpenProject,
    onAddTask,
    onTaskCompleted,
    onTaskDeleted,
}: UpcomingDaySectionProps) {
    const isEmpty = habits.length === 0;
    const showDayLoadMore = dayBundle?.hasNext;

    return (
        <section
            className={[
                'upcoming-date-section',
                isSelected && 'is-selected',
                isEmpty && 'is-empty',
            ].filter(Boolean).join(' ')}
            data-date-key={dateKey}
        >
            <h2 className="upcoming-date-title">
                {formatUpcomingSectionTitle(dateKey)}
            </h2>
            {!isEmpty && (
                <ul className="task-list">
                    {habits.map(habit => (
                        <HabitItem
                            key={habitRowKey(habit)}
                            habit={habit}
                            layout="upcoming"
                            onOpenDetails={onOpenDetails}
                            onOpenProject={onOpenProject}
                            onTaskCompleted={onTaskCompleted}
                            onTaskDeleted={onTaskDeleted}
                        />
                    ))}
                </ul>
            )}
            {isSelected && isEmpty && !dayBundle?.loaded && mayHaveTasks && (
                <p className="upcoming-date-loading">불러오는 중…</p>
            )}
            {isSelected && isEmpty && dayBundle?.loaded && (
                <p className="upcoming-date-empty">이 날짜에 예정된 작업이 없습니다.</p>
            )}
            {onAddTask && <InlineAddTaskButton onClick={onAddTask} />}
            {showDayLoadMore && dayBundle && onLoadMoreDay && (
                <button
                    type="button"
                    className="upcoming-day-load-more"
                    disabled={dayBundle.status === 'loadingMore'}
                    onClick={() => onLoadMoreDay(dateKey)}
                >
                    {dayBundle.status === 'loadingMore' ? '불러오는 중…' : '더 보기'}
                </button>
            )}
        </section>
    );
});

function dayMayHaveTasks(
    dateKey: string,
    upcomingDayCounts: Record<string, number> | null,
    upcomingSummaryStatus: UpcomingTaskListProps['upcomingSummaryStatus'],
): boolean {
    if (upcomingSummaryStatus === 'failed') return true;
    if (upcomingSummaryStatus !== 'loaded') return false;
    return (upcomingDayCounts?.[dateKey] ?? 0) > 0;
}

interface UpcomingVirtualRowProps {
    virtualRow: VirtualItem;
    scrollMargin: number;
    dateKey: string;
    dayHabits: Habit[];
    dayBundle: UpcomingDayBundle | undefined;
    mayHaveTasks: boolean;
    upcomingSummaryStatus: UpcomingTaskListProps['upcomingSummaryStatus'];
    isSelected: boolean;
    habitsSignature: string;
    measureRow: (node: Element | null) => void;
    onEnsureDay: (dateKey: string) => void;
    onLoadMoreDay?: (dateKey: string) => void;
    onOpenDetails?: (habit: Habit) => void;
    onOpenProject?: (projectId: number) => void;
    onAddTask?: () => void;
    onTaskCompleted?: (habit: Habit) => void;
    onTaskDeleted?: (habitId: number) => void;
}

const UpcomingVirtualRow = memo(function UpcomingVirtualRow({
    virtualRow,
    scrollMargin,
    dateKey,
    dayHabits,
    dayBundle,
    mayHaveTasks,
    upcomingSummaryStatus,
    isSelected,
    habitsSignature,
    measureRow,
    onEnsureDay,
    onLoadMoreDay,
    onOpenDetails,
    onOpenProject,
    onAddTask,
    onTaskCompleted,
    onTaskDeleted,
}: UpcomingVirtualRowProps) {
    const rowRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        onEnsureDay(dateKey);
    }, [dateKey, onEnsureDay, upcomingSummaryStatus]);

    useLayoutEffect(() => {
        const node = rowRef.current;
        if (!node) return;

        measureRow(node);

        const observer = new ResizeObserver(() => {
            measureRow(node);
        });
        observer.observe(node);

        return () => observer.disconnect();
    }, [
        habitsSignature,
        dayBundle?.loaded,
        dayBundle?.hasNext,
        dayBundle?.status,
        isSelected,
        mayHaveTasks,
        measureRow,
    ]);

    const setRowRef = useCallback((node: HTMLDivElement | null) => {
        rowRef.current = node;
        if (node) {
            measureRow(node);
        }
    }, [measureRow]);

    return (
        <div
            ref={setRowRef}
            className="upcoming-virtual-row"
            data-index={virtualRow.index}
            style={{
                transform: `translateY(${virtualRow.start - scrollMargin}px)`,
            }}
        >
            <UpcomingDaySection
                dateKey={dateKey}
                habits={dayHabits}
                isSelected={isSelected}
                dayBundle={dayBundle}
                mayHaveTasks={mayHaveTasks}
                onLoadMoreDay={onLoadMoreDay}
                onOpenDetails={onOpenDetails}
                onOpenProject={onOpenProject}
                onAddTask={onAddTask}
                onTaskCompleted={onTaskCompleted}
                onTaskDeleted={onTaskDeleted}
            />
        </div>
    );
});

function parseISO(iso: string): Date {
    return new Date(iso + 'T00:00:00');
}

function buildMonthGrid(year: number, month: number): { date: Date; inMonth: boolean }[] {
    const first = new Date(year, month, 1);
    const startOffset = first.getDay();
    const start = new Date(year, month, 1 - startOffset);
    const cells: { date: Date; inMonth: boolean }[] = [];
    for (let i = 0; i < 42; i++) {
        const d = new Date(start);
        d.setDate(start.getDate() + i);
        cells.push({ date: d, inMonth: d.getMonth() === month });
    }
    return cells;
}

function estimateTaskRowHeight(habit: Habit): number {
    let height = TASK_ROW_BASE_HEIGHT;
    if (habit.description) height += TASK_ROW_DESCRIPTION_HEIGHT;
    if (habit.subtaskCount > 0 || habit.commentCount > 0 || habit.dueDate) {
        height += TASK_ROW_META_HEIGHT;
    }
    return height;
}

function estimateDayHeight(
    dayHabits: Habit[],
    bundle: UpcomingDayBundle | undefined,
    isSelected: boolean,
    mayHaveTasks: boolean,
    hasMore: boolean,
    measuredHeight?: number,
): number {
    if (measuredHeight != null) {
        return measuredHeight;
    }

    if (dayHabits.length === 0) {
        let height = COMPACT_EMPTY_DAY_HEIGHT;
        if (isSelected && bundle?.loaded) {
            height += SELECTED_EMPTY_EXTRA + 8;
        } else if (isSelected && !bundle?.loaded && mayHaveTasks) {
            height += SELECTED_EMPTY_EXTRA;
        }
        if (hasMore) {
            height += DAY_LOAD_MORE_HEIGHT;
        }
        return Math.max(height, EMPTY_DAY_HEIGHT);
    }

    let height = DAY_HEADER_HEIGHT + 8;
    for (const habit of dayHabits) {
        height += estimateTaskRowHeight(habit);
    }
    height += ADD_BUTTON_HEIGHT;
    if (hasMore) {
        height += DAY_LOAD_MORE_HEIGHT;
    }
    return Math.max(height, EMPTY_DAY_HEIGHT);
}

function getOffsetWithinScrollContainer(element: HTMLElement, scrollContainer: HTMLElement): number {
    const containerRect = scrollContainer.getBoundingClientRect();
    const elementRect = element.getBoundingClientRect();
    return elementRect.top - containerRect.top + scrollContainer.scrollTop;
}

const UpcomingTaskList: React.FC<UpcomingTaskListProps> = ({
    habits,
    overdueHabits,
    upcomingDays,
    upcomingDayCounts,
    upcomingSummaryStatus,
    selectedDate,
    weekStartIso,
    jumpStatus,
    scrollContainerRef,
    onJumpToDate,
    onSelectDate,
    onEnsureDay,
    onLoadMoreDay,
    onOpenDetails,
    onOpenProject,
    onAddTask,
    onTaskCompleted,
    onTaskDeleted,
}) => {
    const [monthOpen, setMonthOpen] = useState(false);
    const [viewYear, setViewYear] = useState(() => parseISO(selectedDate).getFullYear());
    const [viewMonth, setViewMonth] = useState(() => parseISO(selectedDate).getMonth());
    const monthPopoverRef = useRef<HTMLDivElement>(null);
    const upcomingViewRef = useRef<HTMLDivElement>(null);
    const stickyChromeRef = useRef<HTMLDivElement>(null);
    const timelineAnchorRef = useRef<HTMLDivElement>(null);
    const pendingScrollIndexRef = useRef<number | null>(null);
    const measuredHeightsRef = useRef<Map<string, number>>(new Map());
    const [timelineLayout, setTimelineLayout] = useState({
        ready: false,
        scrollMargin: 0,
        scrollPaddingStart: 0,
    });
    const { ready: timelineLayoutReady, scrollMargin, scrollPaddingStart } = timelineLayout;

    const todayIso = toISODate(new Date());
    const timelineDateKeys = useMemo(() => getUpcomingTimelineDateKeys(), []);
    const timelineDateSet = useMemo(() => new Set(timelineDateKeys), [timelineDateKeys]);

    const weekStart = useMemo(
        () => (weekStartIso ? parseISO(weekStartIso) : startOfWeekMonday(parseISO(selectedDate))),
        [weekStartIso, selectedDate],
    );
    const weekDays = useMemo(() => getWeekStrip(weekStart, 7), [weekStart]);
    const maxCalendarDate = useMemo(() => getUpcomingCalendarMaxDate(), []);
    const maxCalendarIso = useMemo(() => toISODate(maxCalendarDate), [maxCalendarDate]);
    const maxCalendarMonth = maxCalendarDate.getFullYear() * 12 + maxCalendarDate.getMonth();
    const canGoNextWeek = toISODate(addDays(weekStart, 7)) <= maxCalendarIso;
    const canGoNextMonth = viewYear * 12 + viewMonth < maxCalendarMonth;

    const grouped = useMemo(() => {
        const map = new Map<string, Habit[]>();
        for (const habit of habits) {
            if (habit.completedToday) continue;
            const key = habit.dueDate?.slice(0, 10);
            if (!key || !timelineDateSet.has(key)) continue;
            if (!isAfterToday(key, todayIso) && key !== todayIso) continue;
            if (!map.has(key)) map.set(key, []);
            map.get(key)!.push(habit);
        }
        return map;
    }, [habits, timelineDateSet, todayIso]);

    const getDayBundle = useCallback((dateKey: string) => upcomingDays[dateKey], [upcomingDays]);

    const dayMayHaveTasksForKey = useCallback((dateKey: string) => (
        dayMayHaveTasks(dateKey, upcomingDayCounts, upcomingSummaryStatus)
    ), [upcomingDayCounts, upcomingSummaryStatus]);

    const groupedSizeSignature = useMemo(() => {
        const parts: string[] = [];
        for (const [dateKey, dayHabits] of grouped) {
            parts.push(`${dateKey}:${dayHabits.length}`);
        }
        parts.sort();
        return parts.join('|');
    }, [grouped]);

    const dayBundleSignature = useMemo(
        () => Object.entries(upcomingDays)
            .map(([dateKey, bundle]) => `${dateKey}:${bundle.hasNext ? 1 : 0}:${bundle.loaded ? 1 : 0}`)
            .sort()
            .join('|'),
        [upcomingDays],
    );

    const virtualizer = useVirtualizer({
        count: timelineDateKeys.length,
        enabled: timelineLayoutReady,
        getScrollElement: () => scrollContainerRef?.current ?? null,
        getItemKey: index => {
            const dateKey = timelineDateKeys[index];
            const count = grouped.get(dateKey)?.length ?? 0;
            const hasMore = upcomingDays[dateKey]?.hasNext ? 1 : 0;
            const loaded = upcomingDays[dateKey]?.loaded ? 1 : 0;
            const selected = dateKey === selectedDate ? 1 : 0;
            return `${dateKey}#${count}#${hasMore}#${loaded}#${selected}`;
        },
        estimateSize: index => {
            const dateKey = timelineDateKeys[index];
            const dayHabits = grouped.get(dateKey) ?? [];
            const bundle = getDayBundle(dateKey);
            return estimateDayHeight(
                dayHabits,
                bundle,
                dateKey === selectedDate,
                dayMayHaveTasksForKey(dateKey),
                Boolean(bundle?.hasNext),
                measuredHeightsRef.current.get(dateKey),
            );
        },
        measureElement: element => element.getBoundingClientRect().height,
        overscan: 8,
        scrollMargin,
        scrollPaddingStart,
    });

    const measureRow = useCallback((node: Element | null) => {
        if (!node) return;
        const index = Number((node as HTMLElement).dataset.index);
        const dateKey = Number.isFinite(index) ? timelineDateKeys[index] : undefined;
        const height = node.getBoundingClientRect().height;
        if (dateKey && height > 0) {
            measuredHeightsRef.current.set(dateKey, height);
        }
        virtualizer.measureElement(node);
    }, [timelineDateKeys, virtualizer]);

    const updateScrollOffsets = useCallback(() => {
        const scrollEl = scrollContainerRef?.current;
        const anchor = timelineAnchorRef.current;
        const chrome = stickyChromeRef.current;
        if (!scrollEl || !anchor) return;
        setTimelineLayout({
            ready: true,
            scrollMargin: getOffsetWithinScrollContainer(anchor, scrollEl),
            scrollPaddingStart: chrome?.offsetHeight ?? 0,
        });
    }, [scrollContainerRef]);

    useLayoutEffect(() => {
        updateScrollOffsets();
        const scrollEl = scrollContainerRef?.current;
        if (!scrollEl) return;
        const observer = new ResizeObserver(() => updateScrollOffsets());
        observer.observe(scrollEl);
        if (upcomingViewRef.current) observer.observe(upcomingViewRef.current);
        if (timelineAnchorRef.current) observer.observe(timelineAnchorRef.current);
        if (stickyChromeRef.current) observer.observe(stickyChromeRef.current);
        return () => observer.disconnect();
    }, [scrollContainerRef, updateScrollOffsets, overdueHabits.length, jumpStatus]);

    useEffect(() => {
        if (!timelineLayoutReady) return;
        const frame = requestAnimationFrame(() => {
            for (const virtualRow of virtualizer.getVirtualItems()) {
                const el = document.querySelector(
                    `.upcoming-virtual-row[data-index="${virtualRow.index}"]`,
                );
                if (el) measureRow(el);
            }
        });
        return () => cancelAnimationFrame(frame);
    }, [
        groupedSizeSignature,
        dayBundleSignature,
        selectedDate,
        timelineLayoutReady,
        virtualizer,
        measureRow,
    ]);

    const scrollToDate = useCallback((iso: string) => {
        const index = timelineDateKeys.indexOf(iso);
        if (index < 0) return;
        pendingScrollIndexRef.current = index;
        virtualizer.scrollToIndex(index, { align: 'start', behavior: 'auto' });
    }, [timelineDateKeys, virtualizer]);

    const selectDate = useCallback((iso: string) => {
        if (!isWithinUpcomingCalendarRange(iso)) return;
        onSelectDate(iso);
        onJumpToDate(iso);
        requestAnimationFrame(() => scrollToDate(iso));
    }, [onSelectDate, onJumpToDate, scrollToDate]);

    useEffect(() => {
        const base = parseISO(selectedDate);
        setViewYear(base.getFullYear());
        setViewMonth(base.getMonth());
    }, [selectedDate]);

    useEffect(() => {
        if (jumpStatus === 'loading') return;
        if (pendingScrollIndexRef.current == null) return;
        const index = pendingScrollIndexRef.current;
        pendingScrollIndexRef.current = null;
        virtualizer.scrollToIndex(index, { align: 'start', behavior: 'auto' });
    }, [jumpStatus, virtualizer, habits]);

    const goToday = () => {
        setMonthOpen(false);
        selectDate(todayIso);
    };

    const shiftWeek = (deltaWeeks: number) => {
        const dayOffset = Math.round(
            (parseISO(selectedDate).getTime() - weekStart.getTime()) / 86_400_000,
        );
        const nextWeekStart = addDays(weekStart, deltaWeeks * 7);
        const iso = toISODate(addDays(nextWeekStart, Math.max(0, Math.min(6, dayOffset))));
        selectDate(iso);
    };

    useEffect(() => {
        if (!monthOpen) return;
        const handleClick = (e: MouseEvent) => {
            if (!monthPopoverRef.current?.contains(e.target as Node)) {
                setMonthOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClick);
        return () => document.removeEventListener('mousedown', handleClick);
    }, [monthOpen]);

    const monthLabel = new Date(viewYear, viewMonth, 1).toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: 'long',
    });
    const grid = useMemo(() => buildMonthGrid(viewYear, viewMonth), [viewYear, viewMonth]);

    return (
        <div className="upcoming-view" ref={upcomingViewRef}>
            <div className="upcoming-sticky-chrome" ref={stickyChromeRef}>
                <div className="upcoming-toolbar">
                    <div className="upcoming-month-wrap" ref={monthPopoverRef}>
                        <button
                            type="button"
                            className="upcoming-month-btn"
                            onClick={() => {
                                const base = parseISO(selectedDate);
                                setViewYear(base.getFullYear());
                                setViewMonth(base.getMonth());
                                setMonthOpen(v => !v);
                            }}
                        >
                            {formatMonthYear(parseISO(selectedDate))}
                            <span className="upcoming-caret">▾</span>
                        </button>
                        {monthOpen && (
                            <div className="upcoming-month-popover" onClick={e => e.stopPropagation()}>
                                <div className="calendar-nav">
                                    <button
                                        type="button"
                                        className="cal-nav-btn"
                                        onClick={() => {
                                            const d = new Date(viewYear, viewMonth - 1, 1);
                                            setViewYear(d.getFullYear());
                                            setViewMonth(d.getMonth());
                                        }}
                                        aria-label="이전 달"
                                    >
                                        ‹
                                    </button>
                                    <span className="cal-month-label">{monthLabel}</span>
                                    <button
                                        type="button"
                                        className="cal-nav-btn"
                                        disabled={!canGoNextMonth}
                                        onClick={() => {
                                            if (!canGoNextMonth) return;
                                            const d = new Date(viewYear, viewMonth + 1, 1);
                                            setViewYear(d.getFullYear());
                                            setViewMonth(d.getMonth());
                                        }}
                                        aria-label="다음 달"
                                    >
                                        ›
                                    </button>
                                    <button type="button" className="cal-today-btn" onClick={goToday} aria-label="오늘">◎</button>
                                </div>
                                <div className="calendar-weekdays">
                                    {WEEKDAYS.map(w => (
                                        <span key={w} className="calendar-weekday">{w}</span>
                                    ))}
                                </div>
                                <div className="calendar-grid">
                                    {grid.map(({ date, inMonth }) => {
                                        const iso = toISODate(date);
                                        const isSelected = selectedDate === iso;
                                        const isToday = iso === todayIso;
                                        const isDisabled = !isWithinUpcomingCalendarRange(iso);
                                        return (
                                            <button
                                                key={`${viewYear}-${viewMonth}-${iso}-${String(inMonth)}`}
                                                type="button"
                                                disabled={isDisabled}
                                                className={[
                                                    'calendar-day',
                                                    !inMonth && 'other-month',
                                                    isToday && 'is-today',
                                                    isSelected && 'is-selected',
                                                ].filter(Boolean).join(' ')}
                                                onClick={() => {
                                                    if (isDisabled) return;
                                                    selectDate(iso);
                                                    setMonthOpen(false);
                                                }}
                                            >
                                                {date.getDate()}
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>
                        )}
                    </div>
                    <div className="upcoming-today-nav">
                        <button type="button" onClick={() => shiftWeek(-1)}>‹</button>
                        <button type="button" className="upcoming-today-btn" onClick={goToday}>
                            오늘
                        </button>
                        <button
                            type="button"
                            disabled={!canGoNextWeek}
                            onClick={() => {
                                if (!canGoNextWeek) return;
                                shiftWeek(1);
                            }}
                        >
                            ›
                        </button>
                    </div>
                </div>

                <div className="upcoming-week-strip" role="tablist">
                    {weekDays.map(day => {
                        const isDisabled = !isWithinUpcomingCalendarRange(day.iso);
                        return (
                            <button
                                key={day.iso}
                                type="button"
                                role="tab"
                                disabled={isDisabled}
                                aria-selected={selectedDate === day.iso}
                                className={`upcoming-day-btn ${day.isToday ? 'is-today' : ''} ${selectedDate === day.iso ? 'selected' : ''}`}
                                onClick={() => selectDate(day.iso)}
                            >
                                <span className="upcoming-day-weekday">{day.weekday}</span>
                                <span className="upcoming-day-num">{day.dayNum}</span>
                            </button>
                        );
                    })}
                </div>
            </div>

            {overdueHabits.length > 0 && (
                <OverdueTasksSection
                    habits={overdueHabits}
                    layout="upcoming"
                    onOpenDetails={onOpenDetails}
                    onOpenProject={onOpenProject}
                    onTaskCompleted={onTaskCompleted}
                    onTaskDeleted={onTaskDeleted}
                />
            )}

            {jumpStatus === 'loading' && (
                <p className="upcoming-jump-loading" aria-live="polite">일정을 불러오는 중…</p>
            )}

            <div className="upcoming-timeline" ref={timelineAnchorRef}>
                <div
                    className="upcoming-timeline-inner"
                    style={{ height: `${virtualizer.getTotalSize()}px` }}
                >
                    {virtualizer.getVirtualItems().map(virtualRow => {
                        const dateKey = timelineDateKeys[virtualRow.index];
                        const dayHabits = grouped.get(dateKey) ?? [];
                        const dayBundle = getDayBundle(dateKey);
                        const mayHaveTasks = dayMayHaveTasks(
                            dateKey,
                            upcomingDayCounts,
                            upcomingSummaryStatus,
                        );
                        const habitsSignature = dayHabits.map(habit => habitRowKey(habit)).join(',');
                        return (
                            <UpcomingVirtualRow
                                key={virtualRow.key}
                                virtualRow={virtualRow}
                                scrollMargin={scrollMargin}
                                dateKey={dateKey}
                                dayHabits={dayHabits}
                                dayBundle={dayBundle}
                                mayHaveTasks={mayHaveTasks}
                                upcomingSummaryStatus={upcomingSummaryStatus}
                                isSelected={dateKey === selectedDate}
                                habitsSignature={habitsSignature}
                                measureRow={measureRow}
                                onEnsureDay={onEnsureDay}
                                onLoadMoreDay={onLoadMoreDay}
                                onOpenDetails={onOpenDetails}
                                onOpenProject={onOpenProject}
                                onAddTask={onAddTask}
                                onTaskCompleted={onTaskCompleted}
                                onTaskDeleted={onTaskDeleted}
                            />
                        );
                    })}
                </div>
            </div>
        </div>
    );
};

export default UpcomingTaskList;
