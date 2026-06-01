import { useMemo, useState } from 'react';
import type { Habit, Label, Project } from '../store/habitSlice';

interface SearchModalProps {
    habits: Habit[];
    projects: Project[];
    labels: Label[];
    onClose: () => void;
    onSelectHabit: (habitId: number) => void;
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
    const q = query.trim().toLowerCase();

    const filtered = useMemo(() => {
        if (!q) {
            return {
                habits: habits.slice(0, 8),
                projects: projects.slice(0, 8),
                labels: labels.slice(0, 8),
            };
        }
        return {
            habits: habits.filter(h =>
                h.name.toLowerCase().includes(q) || h.description.toLowerCase().includes(q),
            ).slice(0, 10),
            projects: projects.filter(p => p.name.toLowerCase().includes(q)).slice(0, 10),
            labels: labels.filter(l => l.name.toLowerCase().includes(q)).slice(0, 10),
        };
    }, [habits, labels, projects, q]);

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

                <div className="search-section">
                    <p className="search-section-title">최근 할 일</p>
                    {filtered.habits.map(habit => (
                        <button
                            key={habit.id}
                            type="button"
                            className="search-item"
                            onClick={() => {
                                onSelectHabit(habit.id);
                                onClose();
                            }}
                        >
                            ○ {habit.name}
                            <span>{habit.projectName ?? '받은 편지함'}</span>
                        </button>
                    ))}
                </div>

                <div className="search-section">
                    <p className="search-section-title">프로젝트</p>
                    {filtered.projects.map(project => (
                        <button
                            key={project.id}
                            type="button"
                            className="search-item"
                            onClick={() => {
                                onSelectProject(project.id);
                                onClose();
                            }}
                        >
                            # {project.name}
                            <span>{project.taskCount}</span>
                        </button>
                    ))}
                </div>

                <div className="search-section">
                    <p className="search-section-title">라벨</p>
                    {filtered.labels.map(label => (
                        <button
                            key={label.id}
                            type="button"
                            className="search-item"
                            onClick={() => {
                                onSelectLabel(label.id);
                                onClose();
                            }}
                        >
                            <span className="search-label-dot" style={{ background: label.color }} />
                            {label.name}
                            <span>{label.taskCount}</span>
                        </button>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default SearchModal;
