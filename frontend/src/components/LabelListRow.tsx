import { useState } from 'react';
import { displayLabelName } from '../api/labelMappers';
import type { Label } from '../store/habitSlice';
import { TagIcon } from './icons';
import ProjectMoreMenu from './ProjectMoreMenu';

interface LabelListRowProps {
    label: Label;
    active?: boolean;
    onSelect: (labelId: number) => void;
    onEdit: (label: Label) => void;
    onDelete: (labelId: number) => void;
}

const LabelListRow: React.FC<LabelListRowProps> = ({
    label,
    active = false,
    onSelect,
    onEdit,
    onDelete,
}) => {
    const [hovered, setHovered] = useState(false);
    const [menuOpen, setMenuOpen] = useState(false);
    const showActions = hovered || menuOpen;

    return (
        <li
            className={`label-row ${active ? 'active' : ''}`}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => {
                if (!menuOpen) setHovered(false);
            }}
        >
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
