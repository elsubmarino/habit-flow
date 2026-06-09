import { useEffect, useRef, useState } from 'react';
import { attachmentDownloadUrl, type CommentItem } from '../store/habitSlice';
import { getUserProfile } from '../utils/userProfile';

interface CommentListItemProps {
    comment: CommentItem;
    authorName?: string | null;
    canManage: boolean;
    onEdit: (comment: CommentItem, text: string) => Promise<void>;
    onDelete: (comment: CommentItem) => Promise<void>;
}

function attachmentIconClass(fileName: string): string {
    const ext = fileName.includes('.') ? fileName.split('.').pop()?.toLowerCase() : '';
    if (ext === 'pdf') return 'comment-attachment-icon pdf';
    if (['png', 'jpg', 'jpeg', 'gif', 'webp'].includes(ext ?? '')) return 'comment-attachment-icon image';
    return 'comment-attachment-icon file';
}

const CommentListItem: React.FC<CommentListItemProps> = ({
    comment,
    authorName,
    canManage,
    onEdit,
    onDelete,
}) => {
    const [menuOpen, setMenuOpen] = useState(false);
    const [editing, setEditing] = useState(false);
    const [draft, setDraft] = useState(comment.text);
    const [saving, setSaving] = useState(false);
    const menuRef = useRef<HTMLDivElement>(null);
    const displayName = authorName ?? getUserProfile().displayName;
    const canEdit = Boolean(comment.text.trim());

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
                <div className="comment-list-meta">
                    <span className="comment-author">{displayName}</span>
                    <small>{new Date(comment.createdAt).toLocaleString('ko-KR')}</small>
                </div>
                {comment.text && <p>{comment.text}</p>}
                {comment.attachments.length > 0 && (
                    <ul className="comment-attachment-list">
                        {comment.attachments.map(att => (
                            <li key={`${att.id}-${att.originalFileName}`}>
                                <a
                                    className="comment-attachment-card"
                                    href={attachmentDownloadUrl(att.downloadUrl)}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                >
                                    <span className={attachmentIconClass(att.originalFileName)}>
                                        {att.originalFileName.split('.').pop()?.toUpperCase() ?? 'FILE'}
                                    </span>
                                    <span className="comment-attachment-name">{att.originalFileName}</span>
                                </a>
                            </li>
                        ))}
                    </ul>
                )}
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
                        {canEdit && (
                            <button
                                type="button"
                                onClick={() => {
                                    setMenuOpen(false);
                                    setEditing(true);
                                }}
                            >
                                편집
                            </button>
                        )}
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
