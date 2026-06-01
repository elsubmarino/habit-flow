export function formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

const MAX_FILE_SIZE = 10 * 1024 * 1024;

export function validateFile(file: File): string | null {
    if (file.size > MAX_FILE_SIZE) {
        return `${file.name}: 10MB 이하만 업로드할 수 있습니다.`;
    }
    const ext = file.name.includes('.') ? file.name.split('.').pop()?.toLowerCase() : '';
    const allowed = ['pdf', 'png', 'jpg', 'jpeg', 'gif', 'webp', 'txt', 'md', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'zip'];
    if (!ext || !allowed.includes(ext)) {
        return `${file.name}: 허용되지 않는 파일 형식입니다.`;
    }
    return null;
}
