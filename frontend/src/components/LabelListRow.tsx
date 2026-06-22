import { useState } from 'react';
import { displayLabelName } from '../api/labelMappers';
import type { EntityId } from '../api/types';
import type { Label } from '../store/habitSlice';
import { TagIcon } from './icons';
import ProjectMoreMenu from './ProjectMoreMenu';

interface LabelListRowProps {
    label: Label;
    active?: boolean;
    showDragHandle?: boolean;
    isDragging?: boolean;
    isDragOver?: boolean;
    onDragHandleStart?: (event: React.DragEvent<HTMLButtonElement>) => void;
    onDragEnter?: (event: React.DragEvent<HTMLLIElement>) => void;
    onDragOver?: (event: React.DragEvent<HTMLLIElement>) => void;
    onDragEnd?: (event: React.DragEvent<HTMLLIElement>) => void;
    onDrop?: (event: React.DragEvent<HTMLLIElement>) => void;
    onSelect: (labelId: EntityId) => void;
    onEdit: (label: Label) => void;
    onDelete: (labelId: EntityId) => void;
}

const LabelListRow: React.FC<LabelListRowProps> = ({
    label,
    active = false,
    showDragHandle = false,
    isDragging = false,
    isDragOver = false,
    onDragHandleStart,
    onDragEnter,
    onDragOver,
    onDragEnd,
    onDrop,
    onSelect,
    onEdit,
    onDelete,
}) => {
    const [hovered, setHovered] = useState(false);
    const [menuOpen, setMenuOpen] = useState(false);
    const showActions = hovered || menuOpen;

    return (
        <li
            className={[
                'label-row',
                active ? 'active' : '',
                isDragging ? 'is-dragging' : '',
                isDragOver ? 'is-drag-over' : '',
            ].filter(Boolean).join(' ')}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => {
                if (!menuOpen) setHovered(false);
            }}
            onDragEnter={onDragEnter}
            onDragOver={onDragOver}
            onDragEnd={onDragEnd}
            onDrop={onDrop}
        >
            {showDragHandle && (
                <button
                    type="button"
                    className="label-drag-handle"
                    draggable
                    onDragStart={onDragHandleStart}
                    onClick={event => event.stopPropagation()}
                    aria-label="순서 변경"
                >
                    <span aria-hidden>⋮⋮</span>
                </button>
            )}
            <button
                type="button"
                className={`label-row-main ${active ? 'active' : ''}`}
                onClick={() => onSelect(label.id)}
            >
                <span className="label-row-icon" style={{ color: label.color }}>
                    <TagIcon />
                </span>
                <span className="label-row-name">{displayLabelName(label.name)}</span>
                {label.taskCount > 0 && (
                    <span className="label-row-count">{label.taskCount}</span>
                )}
            </button>
            {showActions && (
                <ProjectMoreMenu
                    onEdit={() => onEdit(label)}
                    onDelete={() => onDelete(label.id)}
                    onOpenChange={open => {
                        setMenuOpen(open);
                        if (!open) setHovered(false);
                    }}
                />
            )}
        </li>
    );
};

export default LabelListRow;
