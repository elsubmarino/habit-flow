import { useMemo, useState } from 'react';
import { mapLabel, mapProject, mapTaskToHabit } from '../api/mappers';
import { INTEGRATED_SEARCH_PAGE_SIZE } from '../api/pagination';
import type { EntityId } from '../api/types';
import { useIntegratedSearch } from '../hooks/useIntegratedSearch';
import type { Habit, Label, Project } from '../store/habitSlice';

interface SearchModalProps {
    habits: Habit[];
    projects: Project[];
    labels: Label[];
    onClose: () => void;
    onSelectHabit: (habit: Habit) => void;
    onSelectProject: (projectId: EntityId) => void;
    onSelectLabel: (labelId: EntityId) => void;
}

const DEFAULT_RESULT_LIMIT = INTEGRATED_SEARCH_PAGE_SIZE;

function SearchResultSkeleton({ variant }: { variant: 'list' | 'chips' }) {
    const count = variant === 'list' ? 3 : 2;
    return (
        <>
            {Array.from({ length: count }, (_, index) => (
                <div
                    key={index}
                    className={`search-skeleton ${variant === 'list' ? 'search-skeleton-row' : 'search-skeleton-chip'}`}
                    aria-hidden
                />
            ))}
        </>
    );
}

const SearchModal: React.FC<SearchModalProps> = ({
    habits,
    projects,
    labels,
    onClose,
    onSelectHabit,
    onSelectProject,
    onSelectLabel,
}) => {
    const [query, setQuery] = useState('');
    const {
        data: searchResult,
        isDebouncing,
        isFetching,
        isError,
    } = useIntegratedSearch(query);

    const q = query.trim();
    const showSkeleton = q.length > 0 && !searchResult && (isDebouncing || isFetching);

    const filtered = useMemo(() => {
        if (!q) {
            return {
                habits: habits.slice(0, DEFAULT_RESULT_LIMIT),
                projects: projects.slice(0, DEFAULT_RESULT_LIMIT),
                labels: labels.slice(0, DEFAULT_RESULT_LIMIT),
            };
        }

        if (!searchResult) {
            return { habits: [], projects: [], labels: [] };
        }

        return {
            habits: searchResult.tasks.map(t => mapTaskToHabit(t)),
            projects: searchResult.projects.map(p => mapProject(p)),
            labels: searchResult.labels.map(l => mapLabel(l)),
        };
    }, [habits, labels, projects, q, searchResult]);

    return (
        <div className="search-overlay" onClick={onClose}>
            <div className="search-modal" onClick={e => e.stopPropagation()}>
                <div className="search-head">
                    <input
                        className="search-input"
                        placeholder="검색하거나 타이핑하여 이동하세요..."
                        value={query}
                        onChange={e => setQuery(e.target.value)}
                        autoFocus
                    />
                    <kbd>Ctrl K</kbd>
                </div>
                {isError && q.length > 0 && (
                    <p className="search-status search-status-error">검색에 실패했습니다.</p>
                )}

                <div className="search-section">
                    <p className="search-section-title">할 일</p>
                    <div className="search-results search-results-list">
                        {showSkeleton ? (
                            <SearchResultSkeleton variant="list" />
                        ) : filtered.habits.length === 0 ? (
                            <p className="search-empty">표시할 작업이 없습니다.</p>
                        ) : filtered.habits.map(habit => (
                            <button
                                key={habit.id}
                                type="button"
                                className="search-result"
                                onClick={() => {
                                    onSelectHabit(habit);
                                    onClose();
                                }}
                            >
                                {habit.name}
                            </button>
                        ))}
                    </div>
                </div>

                <div className="search-section">
                    <p className="search-section-title">프로젝트</p>
                    <div className="search-results search-results-chips">
                        {showSkeleton ? (
                            <SearchResultSkeleton variant="chips" />
                        ) : filtered.projects.length === 0 ? (
                            <p className="search-empty">표시할 프로젝트가 없습니다.</p>
                        ) : filtered.projects.map(project => (
                            <button
                                key={project.id}
                                type="button"
                                className="search-result search-result-chip"
                                onClick={() => {
                                    onSelectProject(project.id);
                                    onClose();
                                }}
                            >
                                <span
                                    className="search-project-dot"
                                    style={{ backgroundColor: project.color ?? '#808080' }}
                                    aria-hidden
                                />
                                {project.name}
                            </button>
                        ))}
                    </div>
                </div>

                <div className="search-section">
                    <p className="search-section-title">라벨</p>
                    <div className="search-results search-results-chips">
                        {showSkeleton ? (
                            <SearchResultSkeleton variant="chips" />
                        ) : filtered.labels.length === 0 ? (
                            <p className="search-empty">표시할 라벨이 없습니다.</p>
                        ) : filtered.labels.map(label => (
                            <button
                                key={label.id}
                                type="button"
                                className="search-result search-result-chip"
                                style={{ borderColor: label.color, color: label.color }}
                                onClick={() => {
                                    onSelectLabel(label.id);
                                    onClose();
                                }}
                            >
                                @{label.name}
                            </button>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SearchModal;
