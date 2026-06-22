import { useState } from 'react';
import type { EntityId } from '../api/types';
import type { Project } from '../store/habitSlice';
import { HashIcon } from './icons';
import ProjectMoreMenu from './ProjectMoreMenu';

interface ProjectListRowProps {
    project: Project;
    active?: boolean;
    variant: 'sidebar' | 'browse';
    showDragHandle?: boolean;
    isDragging?: boolean;
    isDragOver?: boolean;
    onDragHandleStart?: (event: React.DragEvent<HTMLButtonElement>) => void;
    onDragEnter?: (event: React.DragEvent<HTMLLIElement>) => void;
    onDragOver?: (event: React.DragEvent<HTMLLIElement>) => void;
    onDragEnd?: (event: React.DragEvent<HTMLLIElement>) => void;
    onDrop?: (event: React.DragEvent<HTMLLIElement>) => void;
    onSelect: (projectId: EntityId) => void;
    onEdit: (project: Project) => void;
    onDelete: (projectId: EntityId) => void;
    onShare?: (project: Project) => void;
}

const ProjectListRow: React.FC<ProjectListRowProps> = ({
    project,
    active = false,
    variant,
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
    onShare,
}) => {
    const [hovered, setHovered] = useState(false);
    const [menuOpen, setMenuOpen] = useState(false);
    const showActions = hovered || menuOpen;

    const rowClass =
        variant === 'sidebar'
            ? [
                'project-row',
                'project-row-sidebar',
                active ? 'active' : '',
                isDragging ? 'is-dragging' : '',
                isDragOver ? 'is-drag-over' : '',
            ].filter(Boolean).join(' ')
            : 'project-row project-row-browse';

    const mainClass =
        variant === 'sidebar' ? 'project-item' : 'projects-browse-item';

    return (
        <li
            className={rowClass}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => {
                if (!menuOpen) setHovered(false);
            }}
            onDragEnter={onDragEnter}
            onDragOver={onDragOver}
            onDragEnd={onDragEnd}
            onDrop={onDrop}
        >
            {showDragHandle && variant === 'sidebar' && (
                <button
                    type="button"
                    className="project-drag-handle"
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
                className={`${mainClass} ${active ? 'active' : ''}`}
                onClick={() => onSelect(project.id)}
            >
                <span
                    className={variant === 'sidebar' ? 'project-hash' : 'projects-browse-hash'}
                    style={{ color: project.color }}
                >
                    <HashIcon />
                </span>
                <span className={variant === 'sidebar' ? 'project-item-name' : 'projects-browse-name'}>
                    {project.name}
                </span>
                {project.taskCount > 0 && (
                    <span className={variant === 'sidebar' ? 'project-count' : 'projects-browse-meta'}>
                        {project.taskCount}
                    </span>
                )}
            </button>
            {showActions && (
                <ProjectMoreMenu
                    onEdit={() => onEdit(project)}
                    onDelete={() => onDelete(project.id)}
                    onShare={onShare ? () => onShare(project) : undefined}
                    onOpenChange={open => {
                        setMenuOpen(open);
                        if (!open) setHovered(false);
                    }}
                />
            )}
        </li>
    );
};

export default ProjectListRow;
