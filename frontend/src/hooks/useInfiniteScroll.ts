import { type RefObject, useCallback, useEffect, useRef, useState } from 'react';

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
    const [sentinelNode, setSentinelNode] = useState<HTMLDivElement | null>(null);
    const [scrollRoot, setScrollRoot] = useState<HTMLElement | null>(null);

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
        const root = scrollRootRef?.current ?? null;
        setScrollRoot(root);
    }, [scrollRootRef, sentinelNode, enabled, hasNext]);

    useEffect(() => {
        if (!enabled || !hasNext || !sentinelNode) return;

        const requestLoad = () => {
            if (loadLockRef.current || isLoadingRef.current) return;
            loadLockRef.current = true;
            onLoadMoreRef.current();
        };

        const observer = new IntersectionObserver(
            entries => {
                if (entries[0]?.isIntersecting) {
                    requestLoad();
                }
            },
            { root: scrollRoot, rootMargin: '240px', threshold: 0 },
        );

        observer.observe(sentinelNode);
        return () => observer.disconnect();
    }, [enabled, hasNext, scrollRoot, sentinelNode]);

    return sentinelRef;
}
