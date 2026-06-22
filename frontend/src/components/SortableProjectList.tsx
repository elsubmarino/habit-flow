import { useCallback, useState } from 'react';
import type { EntityId } from '../api/types';
import type { Project } from '../store/habitSlice';
import {
    computeProjectSortOrderAfterMove,
    type ReorderProjectRequest,
} from '../utils/projectSortOrder';
import { reorderList } from '../utils/taskSortOrder';
import ProjectListRow from './ProjectListRow';

interface SortableProjectListProps {
    projects: Project[];
    sortable?: boolean;
    onReorder?: (request: ReorderProjectRequest) => void;
    onSelect: (projectId: EntityId) => void;
    onEdit: (project: Project) => void;
    onShare: (project: Project) => void;
    onDelete: (projectId: EntityId) => void;
}

const SortableProjectList: React.FC<SortableProjectListProps> = ({
    projects,
    sortable = true,
    onReorder,
    onSelect,
    onEdit,
    onShare,
    onDelete,
}) => {
    const [dragIndex, setDragIndex] = useState<number | null>(null);
    const [overIndex, setOverIndex] = useState<number | null>(null);

    const canSort = sortable && projects.length > 1 && !!onReorder;

    const finishDrag = useCallback(() => {
        setDragIndex(null);
        setOverIndex(null);
    }, []);

    const handleDrop = useCallback((toIndex: number) => {
        if (!canSort || dragIndex == null || dragIndex === toIndex) {
            finishDrag();
            return;
        }

        const reordered = reorderList(projects, dragIndex, toIndex);
        const sortOrder = computeProjectSortOrderAfterMove(reordered, toIndex);
        onReorder?.({
            projectId: projects[dragIndex]!.id,
            fromIndex: dragIndex,
            toIndex,
            sortOrder,
            contextList: projects,
        });
        finishDrag();
    }, [canSort, dragIndex, finishDrag, onReorder, projects]);

    return (
        <ul className="project-list">
            {projects.map((project, index) => (
                <ProjectListRow
                    key={project.id}
                    project={project}
                    variant="sidebar"
                    showDragHandle={canSort}
                    isDragging={dragIndex === index}
                    isDragOver={overIndex === index && dragIndex !== index}
                    onDragHandleStart={event => {
                        event.dataTransfer.effectAllowed = 'move';
                        event.dataTransfer.setData('text/plain', String(project.id));
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
                    onSelect={onSelect}
                    onEdit={onEdit}
                    onShare={onShare}
                    onDelete={onDelete}
                />
            ))}
        </ul>
    );
};

export default SortableProjectList;
