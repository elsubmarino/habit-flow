import { useEffect, useMemo, useState } from 'react';
import { searchIntegrated } from '../api/searchApi';
import { mapLabel, mapProject, mapTaskToHabit } from '../api/mappers';
import type { Habit, Label, Project } from '../store/habitSlice';

interface SearchModalProps {
    habits: Habit[];
    projects: Project[];
    labels: Label[];
    onClose: () => void;
    onSelectHabit: (habit: Habit) => void;
    onSelectProject: (projectId: number) => void;
    onSelectLabel: (labelId: number) => void;
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
    const [remoteHabits, setRemoteHabits] = useState<Habit[]>([]);
    const [remoteProjects, setRemoteProjects] = useState<Project[]>([]);
    const [remoteLabels, setRemoteLabels] = useState<Label[]>([]);
    const [searching, setSearching] = useState(false);

    const q = query.trim();

    useEffect(() => {
        if (!q) {
            setRemoteHabits([]);
            setRemoteProjects([]);
            setRemoteLabels([]);
            return;
        }
        const timer = window.setTimeout(() => {
            setSearching(true);
            void searchIntegrated(q)
                .then(result => {
                    setRemoteHabits(result.tasks.map(t => mapTaskToHabit(t)));
                    setRemoteProjects(result.projects.map(p => mapProject(p)));
                    setRemoteLabels(result.labels.map(l => mapLabel(l)));
                })
                .catch(() => {
                    setRemoteHabits([]);
                    setRemoteProjects([]);
                    setRemoteLabels([]);
                })
                .finally(() => setSearching(false));
        }, 250);
        return () => window.clearTimeout(timer);
    }, [q]);

    const filtered = useMemo(() => {
        if (!q) {
            return {
                habits: habits.slice(0, 8),
                projects: projects.slice(0, 8),
                labels: labels.slice(0, 8),
            };
        }
        if (remoteHabits.length || remoteProjects.length || remoteLabels.length) {
            return {
                habits: remoteHabits.slice(0, 10),
                projects: remoteProjects.slice(0, 10),
                labels: remoteLabels.slice(0, 10),
            };
        }
        const lower = q.toLowerCase();
        return {
            habits: habits
                .filter(h => h.name.toLowerCase().includes(lower) || h.description.toLowerCase().includes(lower))
                .slice(0, 10),
            projects: projects.filter(p => p.name.toLowerCase().includes(lower)).slice(0, 10),
            labels: labels.filter(l => l.name.toLowerCase().includes(lower)).slice(0, 10),
        };
    }, [habits, labels, projects, q, remoteHabits, remoteLabels, remoteProjects]);

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
                {searching && <p className="search-status">검색 중…</p>}

                <div className="search-section">
                    <p className="search-section-title">할 일</p>
                    <div className="search-results search-results-list">
                        {filtered.habits.length === 0 ? (
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
                        {filtered.projects.length === 0 ? (
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
                        {filtered.labels.length === 0 ? (
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
