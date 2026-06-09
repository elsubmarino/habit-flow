import { useState } from 'react';
import type { Project } from '../store/habitSlice';
import { HashIcon } from './icons';
import ProjectMoreMenu from './ProjectMoreMenu';

interface ProjectListRowProps {
    project: Project;
    active?: boolean;
    variant: 'sidebar' | 'browse';
    onSelect: (projectId: number) => void;
    onEdit: (project: Project) => void;
    onDelete: (projectId: number) => void;
    onShare?: (project: Project) => void;
}

const ProjectListRow: React.FC<ProjectListRowProps> = ({
    project,
    active = false,
    variant,
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
            ? `project-row project-row-sidebar ${active ? 'active' : ''}`
            : `project-row project-row-browse`;

    const mainClass =
        variant === 'sidebar' ? 'project-item' : 'projects-browse-item';

    return (
        <li
            className={rowClass}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => {
                if (!menuOpen) setHovered(false);
            }}
        >
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
