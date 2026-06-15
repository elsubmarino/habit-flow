import { useState } from 'react';
import type { Label } from '../store/habitSlice';
import { ChevronDownIcon, ChevronUpIcon, PlusIcon } from './icons';
import LabelListRow from './LabelListRow';

interface LabelsBrowseViewProps {
    labels: Label[];
    loading?: boolean;
    onSelectLabel: (labelId: number) => void;
    onAddLabel: () => void;
    onEditLabel: (label: Label) => void;
    onDeleteLabel: (labelId: number) => void;
}

const LabelsBrowseView: React.FC<LabelsBrowseViewProps> = ({
    labels,
    loading = false,
    onSelectLabel,
    onAddLabel,
    onEditLabel,
    onDeleteLabel,
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
                        <ul className="labels-browse-list">
                            {labels.map(label => (
                                <LabelListRow
                                    key={label.id}
                                    label={label}
                                    onSelect={onSelectLabel}
                                    onEdit={onEditLabel}
                                    onDelete={onDeleteLabel}
                                />
                            ))}
                        </ul>
                    )
                )}
            </section>
        </div>
    );
};

export default LabelsBrowseView;
