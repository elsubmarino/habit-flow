export interface LabelColorOption {
    hex: string;
    name: string;
}

export const LABEL_COLORS: LabelColorOption[] = [
    { hex: '#808080', name: '차콜' },
    { hex: '#b8256f', name: '베리' },
    { hex: '#db4c3f', name: '빨강' },
    { hex: '#ff9933', name: '오렌지' },
    { hex: '#fad000', name: '노랑' },
    { hex: '#299438', name: '라임 그린' },
    { hex: '#4073ff', name: '블루' },
    { hex: '#ad46ff', name: '바이올렛' },
];

export function getLabelColorName(hex: string): string {
    return LABEL_COLORS.find(c => c.hex.toLowerCase() === hex.toLowerCase())?.name ?? '사용자 지정';
}

export function normalizeLabelColor(hex: string): string {
    const match = LABEL_COLORS.find(c => c.hex.toLowerCase() === hex.toLowerCase());
    return match?.hex ?? hex;
}
