import { forwardRef, useImperativeHandle, useState } from 'react';
import QuickAddModal from './QuickAddModal';
import type { NavItem } from './Sidebar';

export interface AddHabitFormHandle {
    open: () => void;
}

interface AddHabitFormProps {
    view: NavItem;
    projectId: number | null;
    labelId: number | null;
}

const AddHabitForm = forwardRef<AddHabitFormHandle, AddHabitFormProps>(
    ({ view, projectId, labelId }, ref) => {
        const [isOpen, setIsOpen] = useState(false);

        useImperativeHandle(ref, () => ({
            open: () => setIsOpen(true),
        }));

        return (
            <>
                <button type="button" className="add-task-btn" onClick={() => setIsOpen(true)}>
                    <span className="add-task-icon">+</span>
                    <span>작업 추가</span>
                    <span className="add-task-shortcut">Q</span>
                </button>
                <QuickAddModal
                    isOpen={isOpen}
                    onClose={() => setIsOpen(false)}
                    view={view}
                    projectId={projectId}
                    labelId={labelId}
                />
            </>
        );
    },
);

AddHabitForm.displayName = 'AddHabitForm';
export default AddHabitForm;
