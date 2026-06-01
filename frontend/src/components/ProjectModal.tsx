import React, { useState } from 'react';
import type { Project } from '../store/habitSlice';

interface ProjectModalProps {
    projects: Project[];
    onClose: () => void;
    onAdd: (name: string, color: string) => void;
    onDelete: (id: number) => void;
}

const COLORS = ['#4073ff', '#299438', '#eb8909', '#ad46ff', '#db4c3f', '#808080'];

const ProjectModal: React.FC<ProjectModalProps> = ({ projects, onClose, onAdd, onDelete }) => {
    const [name, setName] = useState('');
    const [color, setColor] = useState(COLORS[0]);

    const handleAdd = () => {
        if (!name.trim()) return;
        onAdd(name.trim(), color);
        setName('');
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-card" onClick={e => e.stopPropagation()}>
                <h2 className="modal-title">프로젝트 관리</h2>

                <div className="modal-add-row">
                    <input
                        className="modal-input"
                        placeholder="새 프로젝트 이름"
                        value={name}
                        onChange={e => setName(e.target.value)}
                        onKeyDown={e => e.key === 'Enter' && handleAdd()}
                    />
                    <div className="color-picker">
                        {COLORS.map(c => (
                            <button
                                key={c}
                                type="button"
                                className={`color-swatch ${color === c ? 'active' : ''}`}
                                style={{ background: c }}
                                onClick={() => setColor(c)}
                                aria-label={`색상 ${c}`}
                            />
                        ))}
                    </div>
                    <button type="button" className="submit-btn" onClick={handleAdd}>추가</button>
                </div>

                <ul className="modal-list">
                    {projects.map(project => (
                        <li key={project.id} className="modal-list-item">
                            <span className="project-dot" style={{ background: project.color }} />
                            <span className="modal-item-name">{project.name}</span>
                            <span className="modal-item-meta">{project.taskCount}개 작업</span>
                            <button
                                type="button"
                                className="delete-btn"
                                onClick={() => {
                                    if (window.confirm(`"${project.name}" 프로젝트를 삭제할까요?\n작업은 프로젝트 없이 유지됩니다.`)) {
                                        onDelete(project.id);
                                    }
                                }}
                            >
                                삭제
                            </button>
                        </li>
                    ))}
                </ul>

                <button type="button" className="cancel-btn modal-close" onClick={onClose}>닫기</button>
            </div>
        </div>
    );
};

export default ProjectModal;
