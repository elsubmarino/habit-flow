import { useCallback, useState } from 'react';
import type { EntityId } from '../api/types';
import type { Label } from '../store/habitSlice';
import {
    computeLabelSortOrderAfterMove,
    type ReorderLabelRequest,
} from '../utils/labelSortOrder';
import { reorderList } from '../utils/taskSortOrder';
import LabelListRow from './LabelListRow';

interface SortableLabelListProps {
    labels: Label[];
    sortable?: boolean;
    onReorder?: (request: ReorderLabelRequest) => void;
    onSelectLabel: (labelId: EntityId) => void;
    onEditLabel: (label: Label) => void;
    onDeleteLabel: (labelId: EntityId) => void;
}

const SortableLabelList: React.FC<SortableLabelListProps> = ({
    labels,
    sortable = true,
    onReorder,
    onSelectLabel,
    onEditLabel,
    onDeleteLabel,
}) => {
    const [dragIndex, setDragIndex] = useState<number | null>(null);
    const [overIndex, setOverIndex] = useState<number | null>(null);

    const canSort = sortable && labels.length > 1 && !!onReorder;

    const finishDrag = useCallback(() => {
        setDragIndex(null);
        setOverIndex(null);
    }, []);

    const handleDrop = useCallback((toIndex: number) => {
        if (!canSort || dragIndex == null || dragIndex === toIndex) {
            finishDrag();
            return;
        }

        const reordered = reorderList(labels, dragIndex, toIndex);
        const sortOrder = computeLabelSortOrderAfterMove(reordered, toIndex);
        onReorder?.({
            labelId: labels[dragIndex]!.id,
            fromIndex: dragIndex,
            toIndex,
            sortOrder,
            contextList: labels,
        });
        finishDrag();
    }, [canSort, dragIndex, finishDrag, labels, onReorder]);

    return (
        <ul className="labels-browse-list">
            {labels.map((label, index) => (
                <LabelListRow
                    key={label.id}
                    label={label}
                    showDragHandle={canSort}
                    isDragging={dragIndex === index}
                    isDragOver={overIndex === index && dragIndex !== index}
                    onDragHandleStart={event => {
                        event.dataTransfer.effectAllowed = 'move';
                        event.dataTransfer.setData('text/plain', String(label.id));
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
                    onSelect={onSelectLabel}
                    onEdit={onEditLabel}
                    onDelete={onDeleteLabel}
                />
            ))}
        </ul>
    );
};

export default SortableLabelList;
