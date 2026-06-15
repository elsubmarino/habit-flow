import { type RefObject, useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';

const BOTTOM_THRESHOLD_PX = 64;

function isSentinelNearBottom(root: HTMLElement, sentinel: HTMLElement): boolean {
    const rootRect = root.getBoundingClientRect();
    const elRect = sentinel.getBoundingClientRect();
    return elRect.top <= rootRect.bottom + BOTTOM_THRESHOLD_PX;
}

export function useInfiniteScroll(
    enabled: boolean,
    hasNext: boolean,
    isLoading: boolean,
    onLoadMore: () => void,
    scrollRootRef?: RefObject<HTMLElement | null>,
) {
    const onLoadMoreRef = useRef(onLoadMore);
    const isLoadingRef = useRef(isLoading);
    const loadLockRef = useRef(false);
    const userScrolledRef = useRef(false);
    const [sentinelNode, setSentinelNode] = useState<HTMLDivElement | null>(null);

    const sentinelRef = useCallback((node: HTMLDivElement | null) => {
        setSentinelNode(node);
    }, []);

    useEffect(() => {
        onLoadMoreRef.current = onLoadMore;
    }, [onLoadMore]);

    useEffect(() => {
        isLoadingRef.current = isLoading;
        if (!isLoading) {
            loadLockRef.current = false;
        }
    }, [isLoading]);

    useEffect(() => {
        if (!enabled) {
            userScrolledRef.current = false;
        }
    }, [enabled]);

    const tryLoadMore = useCallback(() => {
        if (!enabled || !hasNext || !sentinelNode) return;
        if (!userScrolledRef.current) return;
        if (loadLockRef.current || isLoadingRef.current) return;

        const root = scrollRootRef?.current ?? null;
        if (!root || !isSentinelNearBottom(root, sentinelNode)) return;

        loadLockRef.current = true;
        onLoadMoreRef.current();
    }, [enabled, hasNext, sentinelNode, scrollRootRef]);

    useLayoutEffect(() => {
        if (!enabled || !hasNext || !sentinelNode) return;

        let disposed = false;
        let cleanup: (() => void) | undefined;

        const attach = () => {
            if (disposed) return;

            const root = scrollRootRef?.current ?? null;
            if (!root) {
                requestAnimationFrame(attach);
                return;
            }

            const onScroll = () => {
                userScrolledRef.current = true;
                tryLoadMore();
            };

            root.addEventListener('scroll', onScroll, { passive: true });
            cleanup = () => root.removeEventListener('scroll', onScroll);
        };

        attach();

        return () => {
            disposed = true;
            cleanup?.();
        };
    }, [enabled, hasNext, sentinelNode, scrollRootRef, tryLoadMore]);

    useEffect(() => {
        if (isLoading || !enabled || !hasNext) return;
        const frame = requestAnimationFrame(() => {
            tryLoadMore();
        });
        return () => cancelAnimationFrame(frame);
    }, [isLoading, enabled, hasNext, sentinelNode, tryLoadMore]);

    return sentinelRef;
}
