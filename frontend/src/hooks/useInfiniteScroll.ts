import { type RefObject, useEffect, useRef } from 'react';

export function useInfiniteScroll(
    enabled: boolean,
    hasNext: boolean,
    isLoading: boolean,
    onLoadMore: () => void,
    scrollRootRef?: RefObject<HTMLElement | null>,
) {
    const sentinelRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!enabled || !hasNext) return;

        const el = sentinelRef.current;
        if (!el) return;

        const observer = new IntersectionObserver(
            entries => {
                if (entries[0]?.isIntersecting && !isLoading) {
                    onLoadMore();
                }
            },
            {
                root: scrollRootRef?.current ?? null,
                rootMargin: '240px',
            },
        );

        observer.observe(el);
        return () => observer.disconnect();
    }, [enabled, hasNext, isLoading, onLoadMore, scrollRootRef]);

    return sentinelRef;
}
