import { useCallback, useState } from 'react';
import type { EntityId } from '../api/types';
import { habitRowKey, type Habit } from '../store/habitSlice';
import { computeSortOrderAfterMove, reorderList, type ReorderHabitRequest } from '../utils/taskSortOrder';
import HabitItem, { type TaskRowLayout } from './HabitItem';

interface SortableTaskListProps {
    habits: Habit[];
    layout?: TaskRowLayout;
    variant?: 'default' | 'overdue';
    sortable?: boolean;
    onReorder?: (request: ReorderHabitRequest) => void;
    onOpenDetails?: (habit: Habit) => void;
    onOpenProject?: (projectId: EntityId) => void;
    onTaskCompleted?: (habit: Habit) => void;
    onTaskDeleted?: (habitId: EntityId) => void;
}

const SortableTaskList: React.FC<SortableTaskListProps> = ({
    habits,
    layout = 'list',
    variant = 'default',
    sortable = true,
    onReorder,
    onOpenDetails,
    onOpenProject,
    onTaskCompleted,
    onTaskDeleted,
}) => {
    const [dragIndex, setDragIndex] = useState<number | null>(null);
    const [overIndex, setOverIndex] = useState<number | null>(null);

    const canSort = sortable && habits.length > 1 && !!onReorder;

    const finishDrag = useCallback(() => {
        setDragIndex(null);
        setOverIndex(null);
    }, []);

    const handleDrop = useCallback((toIndex: number) => {
        if (!canSort || dragIndex == null || dragIndex === toIndex) {
            finishDrag();
            return;
        }

        const reordered = reorderList(habits, dragIndex, toIndex);
        const sortOrder = computeSortOrderAfterMove(reordered, toIndex);
        onReorder?.({
            habitId: habits[dragIndex]!.id,
            fromIndex: dragIndex,
            toIndex,
            sortOrder,
            contextList: habits,
        });
        finishDrag();
    }, [canSort, dragIndex, finishDrag, habits, onReorder]);

    return (
        <ul className="task-list">
            {habits.map((habit, index) => (
                <HabitItem
                    key={habitRowKey(habit)}
                    habit={habit}
                    layout={layout}
                    variant={variant}
                    showDragHandle={canSort}
                    isDragging={dragIndex === index}
                    isDragOver={overIndex === index && dragIndex !== index}
                    onDragHandleStart={event => {
                        event.dataTransfer.effectAllowed = 'move';
                        event.dataTransfer.setData('text/plain', String(habit.id));
                        setDragIndex(index);
                    }}
                    onDragEnter={event => {
                        event.preventDefault();
                        if (dragIndex != null) setOverIndex(index);
                    }}
                    onDragOver={event => {
                        event.preventDefault();
                        event.dataTransfer.dropEffect = 'move';
                        if (dragIndex != null) setOverIndex(index);
                    }}
                    onDragEnd={finishDrag}
                    onDrop={event => {
                        event.preventDefault();
                        handleDrop(index);
                    }}
                    onOpenDetails={onOpenDetails}
                    onOpenProject={onOpenProject}
                    onTaskCompleted={onTaskCompleted}
                    onTaskDeleted={onTaskDeleted}
                />
            ))}
        </ul>
    );
};

export default SortableTaskList;
