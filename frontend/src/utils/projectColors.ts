export interface ProjectColorOption {
    hex: string;
    name: string;
}

export const PROJECT_COLORS: ProjectColorOption[] = [
    { hex: '#b8256f', name: '베리' },
    { hex: '#db4c3f', name: '빨강' },
    { hex: '#eb8909', name: '오렌지' },
    { hex: '#fad000', name: '노랑' },
    { hex: '#299438', name: '라임 그린' },
    { hex: '#4073ff', name: '블루' },
    { hex: '#ad46ff', name: '그레이프' },
    { hex: '#808080', name: '회색' },
];

export function getColorName(hex: string): string {
    return PROJECT_COLORS.find(c => c.hex.toLowerCase() === hex.toLowerCase())?.name ?? '사용자 지정';
}

export function normalizeProjectColor(hex: string): string {
    const match = PROJECT_COLORS.find(c => c.hex.toLowerCase() === hex.toLowerCase());
    return match?.hex ?? hex;
}
