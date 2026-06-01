import type { Label } from '../store/habitSlice';
import type { ViewPreferences } from '../utils/viewPreferences';
import {
    type FilterLabel,
    type FilterPriority,
    type ViewGrouping,
    type ViewLayout,
    type ViewSorting,
} from '../utils/viewPreferences';
import { BoardLayoutIcon, CalendarLayoutIcon, ListLayoutIcon } from './icons';

interface ViewMenuDropdownProps {
    preferences: ViewPreferences;
    labels: Label[];
    onChange: (patch: Partial<ViewPreferences>) => void;
    onClose: () => void;
}

const GROUPING_OPTIONS: { value: ViewGrouping; label: string }[] = [
    { value: 'none', label: '없음' },
    { value: 'date', label: '날짜' },
    { value: 'project', label: '프로젝트' },
    { value: 'priority', label: '우선 순위' },
    { value: 'label', label: '라벨' },
];

const SORTING_OPTIONS: { value: ViewSorting; label: string }[] = [
    { value: 'smart', label: '스마트' },
    { value: 'date', label: '날짜' },
    { value: 'priority', label: '우선 순위' },
    { value: 'name', label: '이름' },
];

const PRIORITY_OPTIONS: { value: FilterPriority; label: string }[] = [
    { value: 'all', label: '전체' },
    { value: 1, label: '우선순위 1' },
    { value: 2, label: '우선순위 2' },
    { value: 3, label: '우선순위 3' },
    { value: 4, label: '우선순위 4' },
];

const ViewMenuDropdown: React.FC<ViewMenuDropdownProps> = ({
    preferences,
    labels,
    onChange,
    onClose,
}) => {
    const layouts: { id: ViewLayout; label: string; icon: React.ReactNode }[] = [
        { id: 'list', label: '목록', icon: <ListLayoutIcon /> },
        { id: 'board', label: '보드', icon: <BoardLayoutIcon /> },
        { id: 'calendar', label: '캘린더', icon: <CalendarLayoutIcon /> },
    ];

    return (
        <div className="view-menu" role="dialog" aria-label="표시 옵션">
            <div className="view-menu-top">
                <span className="view-menu-top-spacer" />
                <button type="button" className="view-menu-help" aria-label="도움말" onClick={onClose}>
                    ?
                </button>
            </div>

            <section className="view-menu-section">
                <h3 className="view-menu-section-title">레이아웃</h3>
                <div className="view-layout-picker">
                    {layouts.map(layout => (
                        <button
                            key={layout.id}
                            type="button"
                            className={`view-layout-btn ${preferences.layout === layout.id ? 'active' : ''}`}
                            onClick={() => onChange({ layout: layout.id })}
                        >
                            <span className="view-layout-icon">{layout.icon}</span>
                            <span>{layout.label}</span>
                        </button>
                    ))}
                </div>
            </section>

            <div className="view-menu-row">
                <span>완료한 작업</span>
                <label className="view-toggle">
                    <input
                        type="checkbox"
                        checked={preferences.showCompleted}
                        onChange={e => onChange({ showCompleted: e.target.checked })}
                    />
                    <span className="view-toggle-track" />
                </label>
            </div>

            <section className="view-menu-section">
                <h3 className="view-menu-section-title">정렬</h3>
                <div className="view-menu-row">
                    <span>그룹핑</span>
                    <select
                        className="view-menu-select"
                        value={preferences.grouping}
                        onChange={e => onChange({ grouping: e.target.value as ViewGrouping })}
                    >
                        {GROUPING_OPTIONS.map(opt => (
                            <option key={opt.value} value={opt.value}>
                                {opt.label}
                            </option>
                        ))}
                    </select>
                </div>
                <div className="view-menu-row">
                    <span>정렬</span>
                    <select
                        className="view-menu-select"
                        value={preferences.sorting}
                        onChange={e => onChange({ sorting: e.target.value as ViewSorting })}
                    >
                        {SORTING_OPTIONS.map(opt => (
                            <option key={opt.value} value={opt.value}>
                                {opt.label}
                            </option>
                        ))}
                    </select>
                </div>
            </section>

            <section className="view-menu-section">
                <h3 className="view-menu-section-title">필터</h3>
                <div className="view-menu-row">
                    <span>할당된 사람</span>
                    <select
                        className="view-menu-select"
                        value={preferences.filterAssignee}
                        onChange={e =>
                            onChange({
                                filterAssignee: e.target.value as ViewPreferences['filterAssignee'],
                            })
                        }
                    >
                        <option value="me-unassigned">나 그리고 미할당됨</option>
                        <option value="all">전체</option>
                    </select>
                </div>
                <div className="view-menu-row">
                    <span>우선 순위</span>
                    <select
                        className="view-menu-select"
                        value={String(preferences.filterPriority)}
                        onChange={e => {
                            const v = e.target.value;
                            onChange({
                                filterPriority: v === 'all' ? 'all' : (Number(v) as FilterPriority),
                            });
                        }}
                    >
                        {PRIORITY_OPTIONS.map(opt => (
                            <option key={String(opt.value)} value={String(opt.value)}>
                                {opt.label}
                            </option>
                        ))}
                    </select>
                </div>
                <div className="view-menu-row">
                    <span>라벨</span>
                    <select
                        className="view-menu-select"
                        value={String(preferences.filterLabel)}
                        onChange={e => {
                            const v = e.target.value;
                            onChange({
                                filterLabel: v === 'all' ? 'all' : (Number(v) as FilterLabel),
                            });
                        }}
                    >
                        <option value="all">전체</option>
                        {labels.map(label => (
                            <option key={label.id} value={String(label.id)}>
                                {label.name}
                            </option>
                        ))}
                    </select>
                </div>
            </section>
        </div>
    );
};

export default ViewMenuDropdown;
