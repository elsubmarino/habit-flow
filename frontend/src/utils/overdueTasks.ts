import type { Habit } from '../store/habitSlice';
import { formatTime12From24, getRelativeDueDayLabel, toISODate } from './date';

function dateOnly(iso: string): string {
    return iso.slice(0, 10);
}

export function getHabitDueTimestamp(habit: Habit): number | null {
    if (!habit.dueDate) return null;

    const day = dateOnly(habit.dueDate);
    if (habit.hasTime && habit.dueTime24) {
        return new Date(`${day}T${habit.dueTime24}:00`).getTime();
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
    const relative = getRelativeDueDayLabel(day);
    const dateLabel = relative ?? new Date(`${day}T00:00:00`).toLocaleDateString('ko-KR', {
        month: 'long',
        day: 'numeric',
    });
    if (habit.hasTime && habit.dueTime24) return `${dateLabel} ${formatTime12From24(habit.dueTime24)}`;
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

export function rescheduleHabitToToday(habit: Habit): { dueDate: string; hasTime: boolean; dueTime24: string | null } {
    const today = toISODate(new Date());
    if (!habit.hasTime || !habit.dueTime24) {
        return { dueDate: today, hasTime: false, dueTime24: null };
    }
    return { dueDate: today, hasTime: true, dueTime24: habit.dueTime24 };
}
