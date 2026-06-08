import { useEffect, useRef, useState } from 'react';
import type { CommentItem } from '../store/habitSlice';

interface CommentListItemProps {
    comment: CommentItem;
    canManage: boolean;
    onEdit: (comment: CommentItem, text: string) => Promise<void>;
    onDelete: (comment: CommentItem) => Promise<void>;
}

const CommentListItem: React.FC<CommentListItemProps> = ({
    comment,
    canManage,
    onEdit,
    onDelete,
}) => {
    const [menuOpen, setMenuOpen] = useState(false);
    const [editing, setEditing] = useState(false);
    const [draft, setDraft] = useState(comment.text);
    const [saving, setSaving] = useState(false);
    const menuRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        setDraft(comment.text);
    }, [comment.text]);

    useEffect(() => {
        if (!menuOpen) return;
        const handleClick = (e: MouseEvent) => {
            if (!menuRef.current?.contains(e.target as Node)) {
                setMenuOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClick);
        return () => document.removeEventListener('mousedown', handleClick);
    }, [menuOpen]);

    const handleSave = async () => {
        if (!draft.trim()) return;
        setSaving(true);
        try {
            await onEdit(comment, draft.trim());
            setEditing(false);
        } catch (err) {
            window.alert(err instanceof Error ? err.message : '댓글을 수정할 수 없습니다.');
        } finally {
            setSaving(false);
        }
    };

    if (editing) {
        return (
            <li className="comment-list-item editing">
                <textarea
                    className="comment-edit-textarea"
                    rows={3}
                    value={draft}
                    onChange={e => setDraft(e.target.value)}
                    autoFocus
                />
                <div className="subtask-form-actions">
                    <button
                        type="button"
                        className="quick-cancel"
                        onClick={() => {
                            setDraft(comment.text);
                            setEditing(false);
                        }}
                        disabled={saving}
                    >
                        취소
                    </button>
                    <button
                        type="button"
                        className="quick-submit"
                        onClick={() => void handleSave()}
                        disabled={saving || !draft.trim()}
                    >
                        업데이트
                    </button>
                </div>
            </li>
        );
    }

    return (
        <li
            className="comment-list-item"
            onMouseLeave={() => setMenuOpen(false)}
        >
            <div className="comment-list-body">
                <p>{comment.text}</p>
                <small>{new Date(comment.createdAt).toLocaleString('ko-KR')}</small>
            </div>
            <div className={`comment-list-actions${canManage ? '' : ' disabled'}`} ref={menuRef}>
                <button
                    type="button"
                    className="comment-more-btn"
                    aria-label="댓글 메뉴"
                    onClick={() => setMenuOpen(v => !v)}
                >
                    ···
                </button>
                {menuOpen && (
                    <div className="comment-action-menu">
                        <button
                            type="button"
                            onClick={() => {
                                setMenuOpen(false);
                                setEditing(true);
                            }}
                        >
                            편집
                        </button>
                        <button
                            type="button"
                            className="danger"
                            onClick={() => {
                                setMenuOpen(false);
                                void onDelete(comment).catch(err => {
                                    window.alert(err instanceof Error ? err.message : '댓글을 삭제할 수 없습니다.');
                                });
                            }}
                        >
                            삭제
                        </button>
                    </div>
                )}
            </div>
        </li>
    );
};

export default CommentListItem;
