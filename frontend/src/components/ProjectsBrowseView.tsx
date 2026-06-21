import { useMemo, useState } from 'react';
import type { EntityId } from '../api/types';
import type { Project } from '../store/habitSlice';
import ProjectListRow from './ProjectListRow';

interface ProjectsBrowseViewProps {
    projects: Project[];
    onSelectProject: (projectId: EntityId) => void;
    onAddProject: () => void;
    onEditProject: (project: Project) => void;
    onDeleteProject: (projectId: EntityId) => void;
}

const ProjectsBrowseView: React.FC<ProjectsBrowseViewProps> = ({
    projects,
    onSelectProject,
    onAddProject,
    onEditProject,
    onDeleteProject,
}) => {
    const [query, setQuery] = useState('');
    const [showArchived, setShowArchived] = useState(false);

    const filtered = useMemo(() => {
        const q = query.trim().toLowerCase();
        if (!q) return projects;
        return projects.filter(p => p.name.toLowerCase().includes(q));
    }, [projects, query]);

    return (
        <div className="projects-browse-page">
            <header className="projects-browse-header">
                <div>
                    <h1 className="projects-browse-title">프로젝트</h1>
                    <span className="projects-browse-badge">무료</span>
                </div>
                <button type="button" className="projects-browse-add-btn" onClick={onAddProject}>
                    + 추가
                </button>
            </header>

            <div className="projects-browse-toolbar">
                <input
                    className="projects-browse-search"
                    placeholder="프로젝트 검색"
                    value={query}
                    onChange={e => setQuery(e.target.value)}
                />
                <label className="projects-browse-archive">
                    <input
                        type="checkbox"
                        checked={showArchived}
                        onChange={e => setShowArchived(e.target.checked)}
                    />
                    <span>보관된 프로젝트</span>
                </label>
            </div>

            {projects.length >= 5 && (
                <div className="projects-upgrade-banner">
                    <span>프로젝트를 더 추가하려면 업그레이드가 필요합니다.</span>
                    <button type="button" className="projects-upgrade-btn">
                        업그레이드
                    </button>
                </div>
            )}

            <section className="projects-browse-list-section">
                <h2 className="projects-browse-count">
                    {filtered.length} 프로젝트
                </h2>
                {showArchived ? (
                    <p className="projects-browse-empty">보관된 프로젝트가 없습니다.</p>
                ) : filtered.length === 0 ? (
                    <p className="projects-browse-empty">프로젝트가 없습니다.</p>
                ) : (
                    <ul className="projects-browse-list">
                        {filtered.map(project => (
                            <ProjectListRow
                                key={project.id}
                                project={project}
                                variant="browse"
                                onSelect={onSelectProject}
                                onEdit={onEditProject}
                                onDelete={onDeleteProject}
                            />
                        ))}
                    </ul>
                )}
            </section>
        </div>
    );
};

export default ProjectsBrowseView;
