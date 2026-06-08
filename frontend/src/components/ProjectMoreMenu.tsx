import { useEffect, useRef, useState } from 'react';
import { MoreHorizontalIcon } from './icons';

interface ProjectMoreMenuProps {
    onEdit: () => void;
    onDelete: () => void;
    onOpenChange?: (open: boolean) => void;
}

const ProjectMoreMenu: React.FC<ProjectMoreMenuProps> = ({ onEdit, onDelete, onOpenChange }) => {
    const [open, setOpen] = useState(false);
    const rootRef = useRef<HTMLDivElement>(null);

    const setMenuOpen = (next: boolean) => {
        setOpen(next);
        onOpenChange?.(next);
    };

    useEffect(() => {
        if (!open) return;
        const onDocClick = (e: MouseEvent) => {
            if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
                setMenuOpen(false);
            }
        };
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') setMenuOpen(false);
        };
        document.addEventListener('mousedown', onDocClick);
        window.addEventListener('keydown', onKey);
        return () => {
            document.removeEventListener('mousedown', onDocClick);
            window.removeEventListener('keydown', onKey);
        };
    }, [open]);

    return (
        <div className="project-more-menu" ref={rootRef}>
            <button
                type="button"
                className="project-more-btn"
                aria-label="프로젝트 옵션"
                aria-expanded={open}
                aria-haspopup="menu"
                onClick={e => {
                    e.stopPropagation();
                    setMenuOpen(!open);
                }}
            >
                <MoreHorizontalIcon />
            </button>
            {open && (
                <div className="project-menu-dropdown" role="menu">
                    <button
                        type="button"
                        className="project-menu-item"
                        role="menuitem"
                        onClick={e => {
                            e.stopPropagation();
                            setMenuOpen(false);
                            onEdit();
                        }}
                    >
                        편집
                    </button>
                    <button
                        type="button"
                        className="project-menu-item danger"
                        role="menuitem"
                        onClick={e => {
                            e.stopPropagation();
                            setMenuOpen(false);
                            onDelete();
                        }}
                    >
                        삭제
                    </button>
                </div>
            )}
        </div>
    );
};

export default ProjectMoreMenu;
