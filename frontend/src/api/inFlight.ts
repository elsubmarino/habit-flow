const inFlightRequests = new Map<string, Promise<unknown>>();

/** 동일 key의 동시 요청을 하나로 합칩니다 (React StrictMode 이중 effect 등). */
export function dedupeInFlight<T>(key: string, request: () => Promise<T>): Promise<T> {
    const existing = inFlightRequests.get(key);
    if (existing) {
        return existing as Promise<T>;
    }

    const promise = request().finally(() => {
        if (inFlightRequests.get(key) === promise) {
            inFlightRequests.delete(key);
        }
    });

    inFlightRequests.set(key, promise);
    return promise;
}
