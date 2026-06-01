import { useEffect, useRef, useState } from 'react';
import type { Label } from '../store/habitSlice';
import type { ViewPreferences } from '../utils/viewPreferences';
import { DisplayIcon } from './icons';
import ViewMenuDropdown from './ViewMenuDropdown';

interface ViewMenuButtonProps {
    preferences: ViewPreferences;
    labels: Label[];
    onChange: (prefs: ViewPreferences) => void;
}

const ViewMenuButton: React.FC<ViewMenuButtonProps> = ({ preferences, labels, onChange }) => {
    const [open, setOpen] = useState(false);
    const wrapRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!open) return;
        const onDoc = (e: MouseEvent) => {
            if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) {
                setOpen(false);
            }
        };
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') setOpen(false);
        };
        document.addEventListener('mousedown', onDoc);
        window.addEventListener('keydown', onKey);
        return () => {
            document.removeEventListener('mousedown', onDoc);
            window.removeEventListener('keydown', onKey);
        };
    }, [open]);

    const patch = (partial: Partial<ViewPreferences>) => {
        onChange({ ...preferences, ...partial });
    };

    return (
        <div className="view-menu-wrap" ref={wrapRef}>
            <button
                type="button"
                className={`view-menu-trigger ${open ? 'open' : ''}`}
                aria-expanded={open}
                aria-haspopup="dialog"
                onClick={() => setOpen(prev => !prev)}
            >
                <DisplayIcon />
                <span>표시</span>
            </button>
            {open && (
                <ViewMenuDropdown
                    preferences={preferences}
                    labels={labels}
                    onChange={partial => {
                        patch(partial);
                    }}
                    onClose={() => setOpen(false)}
                />
            )}
        </div>
    );
};

export default ViewMenuButton;
