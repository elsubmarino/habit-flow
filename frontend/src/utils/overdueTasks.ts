import type { Habit } from '../store/habitSlice';
import { dueDateToTimeInput, toISODate } from './date';

function dateOnly(iso: string): string {
    return iso.slice(0, 10);
}

export function getHabitDueTimestamp(habit: Habit): number | null {
    if (!habit.dueDate) return null;

    const day = dateOnly(habit.dueDate);
    const hasTime = Boolean(habit.dueTime) || habit.dueDate.includes('T');

    if (hasTime) {
        const time24 = habit.dueDate.includes('T')
            ? (habit.dueDate.split('T')[1] ?? '').slice(0, 5)
            : dueDateToTimeInput(habit.dueDate, habit.dueTime);
        return new Date(`${day}T${time24}:00`).getTime();
    }

    return new Date(`${day}T23:59:59.999`).getTime();
}

export function isOverdueHabit(habit: Habit, now = new Date()): boolean {
    if (!habit.dueDate || habit.completedToday) return false;
    const dueAt = getHabitDueTimestamp(habit);
    if (dueAt == null) return false;
    return dueAt < now.getTime();
}

export function formatOverdueDueLabel(habit: Habit): string {
    if (!habit.dueDate) return '';
    const day = dateOnly(habit.dueDate);
    const dateLabel = new Date(`${day}T00:00:00`).toLocaleDateString('ko-KR', {
        month: 'long',
        day: 'numeric',
    });
    if (habit.dueTime) return `${dateLabel} ${habit.dueTime}`;
    return dateLabel;
}

export function splitOverdueTasks(habits: Habit[]): { overdue: Habit[]; rest: Habit[] } {
    const overdue: Habit[] = [];
    const rest: Habit[] = [];

    for (const habit of habits) {
        if (isOverdueHabit(habit)) overdue.push(habit);
        else rest.push(habit);
    }

    overdue.sort((a, b) => (getHabitDueTimestamp(a) ?? 0) - (getHabitDueTimestamp(b) ?? 0));
    return { overdue, rest };
}

export function rescheduleHabitToToday(habit: Habit): string {
    const today = toISODate(new Date());
    if (!habit.dueTime) return today;
    const time24 = dueDateToTimeInput(habit.dueDate, habit.dueTime);
    return `${today}T${time24}:00`;
}
