import { type RefObject, useEffect, useRef } from 'react';

export function useInfiniteScroll(
    enabled: boolean,
    hasNext: boolean,
    isLoading: boolean,
    onLoadMore: () => void,
    scrollRootRef?: RefObject<HTMLElement | null>,
) {
    const sentinelRef = useRef<HTMLDivElement>(null);
    const onLoadMoreRef = useRef(onLoadMore);
    const isLoadingRef = useRef(isLoading);
    const loadLockRef = useRef(false);
    const wasIntersectingRef = useRef(false);

    useEffect(() => {
        onLoadMoreRef.current = onLoadMore;
    }, [onLoadMore]);

    useEffect(() => {
        isLoadingRef.current = isLoading;
        if (isLoading) return;

        loadLockRef.current = false;

        const el = sentinelRef.current;
        const root = scrollRootRef?.current ?? null;
        if (!el || !enabled || !hasNext) return;

        requestAnimationFrame(() => {
            const rootRect = root?.getBoundingClientRect();
            const viewportBottom = rootRect?.bottom ?? window.innerHeight;
            const elRect = el.getBoundingClientRect();
            wasIntersectingRef.current = elRect.top <= viewportBottom + 240;
        });
    }, [isLoading, enabled, hasNext, scrollRootRef]);

    useEffect(() => {
        if (!enabled || !hasNext) return;

        const el = sentinelRef.current;
        if (!el) return;

        const root = scrollRootRef?.current ?? null;

        const requestLoad = () => {
            if (loadLockRef.current || isLoadingRef.current) return;
            loadLockRef.current = true;
            onLoadMoreRef.current();
        };

        let skipInitial = true;

        const observer = new IntersectionObserver(
            entries => {
                const intersecting = entries[0]?.isIntersecting ?? false;
                if (skipInitial) {
                    skipInitial = false;
                    wasIntersectingRef.current = intersecting;
                    return;
                }
                if (intersecting && !wasIntersectingRef.current) {
                    requestLoad();
                }
                wasIntersectingRef.current = intersecting;
            },
            { root, rootMargin: '240px', threshold: 0 },
        );

        observer.observe(el);
        return () => observer.disconnect();
    }, [enabled, hasNext, scrollRootRef]);

    return sentinelRef;
}
