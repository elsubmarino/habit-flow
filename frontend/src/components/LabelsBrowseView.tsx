import { useState } from 'react';
import type { EntityId } from '../api/types';
import type { Label } from '../store/habitSlice';
import type { ReorderLabelRequest } from '../utils/labelSortOrder';
import { ChevronDownIcon, ChevronUpIcon, PlusIcon } from './icons';
import SortableLabelList from './SortableLabelList';

interface LabelsBrowseViewProps {
    labels: Label[];
    loading?: boolean;
    onSelectLabel: (labelId: EntityId) => void;
    onAddLabel: () => void;
    onEditLabel: (label: Label) => void;
    onDeleteLabel: (labelId: EntityId) => void;
    onReorderLabel?: (request: ReorderLabelRequest) => void;
}

const LabelsBrowseView: React.FC<LabelsBrowseViewProps> = ({
    labels,
    loading = false,
    onSelectLabel,
    onAddLabel,
    onEditLabel,
    onDeleteLabel,
    onReorderLabel,
}) => {
    const [sectionExpanded, setSectionExpanded] = useState(true);

    return (
        <div className="labels-browse-page">
            <h1 className="labels-browse-title">라벨</h1>

            <section className="labels-browse-section">
                <div className="labels-section-header">
                    <button
                        type="button"
                        className="labels-section-toggle"
                        aria-label={sectionExpanded ? '라벨 목록 접기' : '라벨 목록 펼치기'}
                        onClick={() => setSectionExpanded(prev => !prev)}
                    >
                        {sectionExpanded ? <ChevronDownIcon /> : <ChevronUpIcon />}
                    </button>
                    <span className="labels-section-title">라벨</span>
                    <button
                        type="button"
                        className="labels-section-add-btn"
                        aria-label="라벨 추가"
                        onClick={onAddLabel}
                    >
                        <PlusIcon />
                    </button>
                </div>

                {sectionExpanded && (
                    loading ? (
                        <p className="labels-browse-empty">불러오는 중…</p>
                    ) : labels.length === 0 ? (
                        <p className="labels-browse-empty">라벨이 없습니다.</p>
                    ) : (
                        <SortableLabelList
                            labels={labels}
                            sortable={!!onReorderLabel}
                            onReorder={onReorderLabel}
                            onSelectLabel={onSelectLabel}
                            onEditLabel={onEditLabel}
                            onDeleteLabel={onDeleteLabel}
                        />
                    )
                )}
            </section>
        </div>
    );
};

export default LabelsBrowseView;
