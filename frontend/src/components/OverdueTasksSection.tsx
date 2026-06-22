import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchMoreOverdue, patchTaskDueDateBatch } from '../store/habitSlice';
import type { EntityId } from '../api/types';
import type { Habit } from '../store/habitSlice';
import { useDialog } from '../context/DialogContext';
import { toISODate } from '../utils/date';
import type { ReorderHabitRequest } from '../utils/taskSortOrder';
import DatePickerDropdown, { type DatePickerChange } from './DatePickerDropdown';
import SortableTaskList from './SortableTaskList';
import type { TaskRowLayout } from './HabitItem';
import { ChevronDownIcon } from './icons';

interface OverdueTasksSectionProps {
    habits: Habit[];
    layout?: TaskRowLayout;
    sortable?: boolean;
    onReorder?: (request: ReorderHabitRequest) => void;
    onOpenDetails?: (habit: Habit) => void;
    onOpenProject?: (projectId: EntityId) => void;
    onTaskCompleted?: (habit: Habit) => void;
    onTaskDeleted?: (habitId: EntityId) => void;
}

const OverdueTasksSection: React.FC<OverdueTasksSectionProps> = ({
    habits,
    layout = 'list',
    sortable = false,
    onReorder,
    onOpenDetails,
    onOpenProject,
    onTaskCompleted,
    onTaskDeleted,
}) => {
    const dispatch = useAppDispatch();
    const { showErrorAlert } = useDialog();
    const { overdueHasNext, overdueLoadMoreStatus } = useAppSelector(state => state.habits);
    const [collapsed, setCollapsed] = useState(false);
    const [rescheduling, setRescheduling] = useState(false);
    const [showReschedulePicker, setShowReschedulePicker] = useState(false);
    const [pickerDate, setPickerDate] = useState<string | null>(() => toISODate(new Date()));
    const [pickerHasTime, setPickerHasTime] = useState(false);
    const [pickerTime, setPickerTime] = useState<string | null>(null);
    const [pickerRepeat, setPickerRepeat] = useState<string | null>(null);
    const rescheduleRef = useRef<HTMLDivElement>(null);

    const todayIso = useMemo(() => toISODate(new Date()), []);

    const openReschedulePicker = useCallback(() => {
        setPickerDate(todayIso);
        setPickerHasTime(false);
        setPickerTime(null);
        setPickerRepeat(null);
        setShowReschedulePicker(true);
    }, [todayIso]);

    useEffect(() => {
        if (!showReschedulePicker) return;

        const handlePointerDown = (event: MouseEvent) => {
            if (!rescheduleRef.current?.contains(event.target as Node)) {
                setShowReschedulePicker(false);
            }
        };

        document.addEventListener('mousedown', handlePointerDown);
        return () => document.removeEventListener('mousedown', handlePointerDown);
    }, [showReschedulePicker]);

    const applyReschedule = useCallback(async (change: DatePickerChange) => {
        const nextDate = change.date !== undefined ? change.date : pickerDate;
        const nextHasTime = change.hasTime !== undefined ? change.hasTime : pickerHasTime;
        const nextTime = change.time !== undefined ? change.time : pickerTime;
        const nextRepeat = change.repeat !== undefined ? change.repeat : pickerRepeat;

        if (change.date !== undefined) setPickerDate(change.date);
        if (change.hasTime !== undefined) setPickerHasTime(change.hasTime);
        if (change.time !== undefined) setPickerTime(change.time);
        if (change.repeat !== undefined) setPickerRepeat(change.repeat);

        if (nextDate === undefined) return;

        setRescheduling(true);
        try {
            await dispatch(patchTaskDueDateBatch({
                habitIds: habits.map(habit => habit.id),
                dueDate: nextDate,
                dueTime24: nextHasTime ? nextTime : null,
                hasTime: nextHasTime && Boolean(nextTime),
                recurrenceLabel: nextRepeat,
            })).unwrap();
        } catch {
            await showErrorAlert('일정 변경에 실패했습니다.');
        } finally {
            setRescheduling(false);
        }
    }, [
        dispatch,
        habits,
        pickerDate,
        pickerHasTime,
        pickerRepeat,
        pickerTime,
        showErrorAlert,
    ]);

    if (habits.length === 0) return null;

    return (
        <section className="overdue-section">
            <div className="overdue-section-header">
                <button
                    type="button"
                    className={`overdue-section-toggle ${collapsed ? 'collapsed' : ''}`}
                    onClick={() => setCollapsed(v => !v)}
                    aria-expanded={!collapsed}
                >
                    <span className="overdue-section-chevron">
                        <ChevronDownIcon />
                    </span>
                    기한이 지난
                </button>
                <div className="overdue-reschedule-wrap" ref={rescheduleRef}>
                    <button
                        type="button"
                        className={`overdue-reschedule-btn${showReschedulePicker ? ' active' : ''}`}
                        onClick={event => {
                            event.stopPropagation();
                            if (showReschedulePicker) {
                                setShowReschedulePicker(false);
                            } else {
                                openReschedulePicker();
                            }
                        }}
                        disabled={rescheduling || habits.length === 0}
                        aria-expanded={showReschedulePicker}
                    >
                        {rescheduling ? '변경 중…' : '일정 변경'}
                    </button>
                    {showReschedulePicker && (
                        <DatePickerDropdown
                            value={pickerDate}
                            timeValue={pickerHasTime ? pickerTime : null}
                            hasTimeValue={pickerHasTime}
                            repeatValue={pickerRepeat}
                            onChange={change => void applyReschedule(change)}
                        />
                    )}
                </div>
            </div>
            {!collapsed && (
                <>
                    <SortableTaskList
                        habits={habits}
                        layout={layout}
                        variant="overdue"
                        sortable={sortable}
                        onReorder={onReorder}
                        onOpenDetails={onOpenDetails}
                        onOpenProject={onOpenProject}
                        onTaskCompleted={onTaskCompleted}
                        onTaskDeleted={onTaskDeleted}
                    />
                    {overdueHasNext && (
                        <button
                            type="button"
                            className="overdue-load-more"
                            disabled={overdueLoadMoreStatus === 'loading'}
                            onClick={() => void dispatch(fetchMoreOverdue())}
                        >
                            {overdueLoadMoreStatus === 'loading' ? '불러오는 중…' : '더 보기'}
                        </button>
                    )}
                </>
            )}
        </section>
    );
};

export default OverdueTasksSection;
