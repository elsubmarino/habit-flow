import { useEffect } from 'react';
import type { EntityId } from '../api/types';
import { useAppDispatch } from '../store/hooks';
import { addHabit, fetchHabits, fetchNavTaskCounts, fetchProjects, type ApiView } from '../store/habitSlice';
import { getApiErrorMessage } from '../api/apiError';
import { useToast } from '../context/ToastContext';
import { defaultDueDateForView } from '../utils/date';
import type { NavItem } from './Sidebar';
import TaskQuickAddForm, { type TaskQuickAddSubmitPayload } from './TaskQuickAddForm';

interface QuickAddModalProps {
    isOpen: boolean;
    onClose: () => void;
    view: NavItem;
    projectId: EntityId | null;
    labelId: EntityId | null;
}

const QuickAddModal: React.FC<QuickAddModalProps> = ({
    isOpen,
    onClose,
    view,
    projectId,
    labelId,
}) => {
    const dispatch = useAppDispatch();
    const { showErrorToast } = useToast();

    useEffect(() => {
        if (!isOpen) return;
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [isOpen, onClose]);

    const handleSubmit = async (payload: TaskQuickAddSubmitPayload) => {
        try {
            await dispatch(addHabit({
                name: payload.name,
                description: payload.description,
                view,
                projectId: payload.projectId ?? projectId,
                dueDate: payload.dueDate ?? defaultDueDateForView(view),
                dueTime24: payload.dueTime24,
                hasTime: payload.hasTime,
                recurrenceLabel: payload.recurrenceLabel,
                labelIds: payload.labelIds.length > 0
                    ? payload.labelIds
                    : (labelId ? [labelId] : []),
                file: payload.file,
                priority: payload.priority,
            })).unwrap();

            const apiView: ApiView = view === 'filters' ? 'all' : view;
            await dispatch(fetchHabits({
                view: apiView,
                projectId,
                labelId,
            }));
            dispatch(fetchNavTaskCounts());
            dispatch(fetchProjects());
            onClose();
        } catch (err) {
            const message = typeof err === 'string'
                ? err
                : getApiErrorMessage(err, '작업을 추가하지 못했습니다.');
            showErrorToast(message);
        }
    };

    if (!isOpen) return null;

    return (
        <div
            className="quick-add-overlay"
            onClick={onClose}
            role="dialog"
            aria-modal="true"
            aria-label="작업 빠른 추가"
        >
            <div onClick={e => e.stopPropagation()}>
                <TaskQuickAddForm
                    key={`${view}:${projectId ?? ''}:${labelId ?? ''}`}
                    variant="modal"
                    initialProjectId={projectId}
                    initialLabelIds={labelId ? [labelId] : []}
                    initialDueDate={defaultDueDateForView(view)}
                    onCancel={onClose}
                    onSubmit={handleSubmit}
                />
            </div>
        </div>
    );
};

export default QuickAddModal;
