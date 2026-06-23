import { useEffect, useMemo, useRef, useState } from 'react';

export interface CheckboxFilterOption<T extends string> {
    value: T;
    label: string;
}

interface ActivityLogCheckboxFilterProps<T extends string> {
    icon: string;
    allLabel: string;
    options: CheckboxFilterOption<T>[];
    selected: Set<T>;
    onChange: (selected: Set<T>) => void;
}

function formatTriggerLabel<T extends string>(
    allLabel: string,
    options: CheckboxFilterOption<T>[],
    selected: Set<T>,
): string {
    if (selected.size === 0) return allLabel;
    if (selected.size === 1) {
        const value = [...selected][0]!;
        return options.find(option => option.value === value)?.label ?? allLabel;
    }
    return `${selected.size}개 선택`;
}

function ActivityLogCheckboxFilter<T extends string>({
    icon,
    allLabel,
    options,
    selected,
    onChange,
}: ActivityLogCheckboxFilterProps<T>) {
    const rootRef = useRef<HTMLDivElement>(null);
    const [open, setOpen] = useState(false);
    const triggerLabel = useMemo(
        () => formatTriggerLabel(allLabel, options, selected),
        [allLabel, options, selected],
    );

    useEffect(() => {
        if (!open) return;
        const onDocClick = (event: MouseEvent) => {
            if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
                setOpen(false);
            }
        };
        document.addEventListener('mousedown', onDocClick);
        return () => document.removeEventListener('mousedown', onDocClick);
    }, [open]);

    const allSelected = selected.size === 0;

    const handleAllChange = () => {
        onChange(new Set());
    };

    const handleOptionChange = (value: T, checked: boolean) => {
        const next = new Set(selected);
        if (checked) {
            next.add(value);
        } else {
            next.delete(value);
        }
        onChange(next);
    };

    return (
        <div className="activity-filter activity-checkbox-filter" ref={rootRef}>
            <button
                type="button"
                className="activity-checkbox-filter-btn"
                onClick={() => setOpen(prev => !prev)}
                aria-expanded={open}
            >
                <span className="activity-filter-icon" aria-hidden>{icon}</span>
                <span className="activity-checkbox-filter-label">{triggerLabel}</span>
            </button>

            {open && (
                <div
                    className="activity-checkbox-dropdown"
                    role="listbox"
                    aria-multiselectable="true"
                    onClick={e => e.stopPropagation()}
                >
                    <label className="activity-checkbox-option">
                        <input
                            type="checkbox"
                            checked={allSelected}
                            onChange={handleAllChange}
                        />
                        <span>{allLabel}</span>
                    </label>
                    <div className="activity-checkbox-divider" role="separator" />
                    {options.map(option => (
                        <label key={option.value} className="activity-checkbox-option">
                            <input
                                type="checkbox"
                                checked={selected.has(option.value)}
                                onChange={e => handleOptionChange(option.value, e.target.checked)}
                            />
                            <span>{option.label}</span>
                        </label>
                    ))}
                </div>
            )}
        </div>
    );
}

export default ActivityLogCheckboxFilter;
