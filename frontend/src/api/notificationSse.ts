import { ensureAccessToken } from './client';
import type { NotificationDto } from './types';

export interface NotificationSseHandlers {
    onConnect?: () => void;
    onNotification: (notification: NotificationDto) => void;
    onError?: (error: unknown) => void;
}

interface ParsedSseEvent {
    event: string;
    data: string;
}

function parseSseChunk(buffer: string): { events: ParsedSseEvent[]; rest: string } {
    const events: ParsedSseEvent[] = [];
    const blocks = buffer.split('\n\n');

    for (let i = 0; i < blocks.length - 1; i += 1) {
        const block = blocks[i];
        if (!block.trim()) continue;

        let event = 'message';
        const dataLines: string[] = [];

        for (const line of block.split('\n')) {
            if (line.startsWith('event:')) {
                event = line.slice(6).trim();
            } else if (line.startsWith('data:')) {
                dataLines.push(line.slice(5).trim());
            }
        }

        if (dataLines.length > 0) {
            events.push({ event, data: dataLines.join('\n') });
        }
    }

    return { events, rest: blocks[blocks.length - 1] ?? '' };
}

export async function subscribeNotificationStream(
    handlers: NotificationSseHandlers,
    signal: AbortSignal,
): Promise<void> {
    const token = await ensureAccessToken();
    if (!token) {
        throw new Error('인증 토큰이 없습니다.');
    }

    const response = await fetch('/api/notifications/subscribe', {
        method: 'GET',
        headers: {
            Accept: 'text/event-stream',
            Authorization: `Bearer ${token}`,
        },
        signal,
    });

    if (response.status === 401 || response.status === 403) {
        throw new Error(`SSE 연결 실패 (${response.status})`);
    }

    if (!response.ok) {
        throw new Error(`SSE 연결 실패 (${response.status})`);
    }

    const reader = response.body?.getReader();
    if (!reader) {
        throw new Error('SSE 스트림을 읽을 수 없습니다.');
    }

    const decoder = new TextDecoder();
    let buffer = '';

    while (!signal.aborted) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const parsed = parseSseChunk(buffer);
        buffer = parsed.rest;

        for (const { event, data } of parsed.events) {
            if (event === 'connect') {
                handlers.onConnect?.();
                continue;
            }

            if (event === 'notification') {
                try {
                    const notification = JSON.parse(data) as NotificationDto;
                    handlers.onNotification(notification);
                } catch (error) {
                    handlers.onError?.(error);
                }
            }
        }
    }
}
