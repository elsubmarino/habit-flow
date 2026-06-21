import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { searchIntegrated } from '../api/searchApi';
import { INTEGRATED_SEARCH_PAGE_SIZE } from '../api/pagination';
import { useDebouncedValue } from './useDebouncedValue';

const SEARCH_DEBOUNCE_MS = 300;
const SEARCH_STALE_TIME_MS = 5 * 60 * 1000;

export function useIntegratedSearch(keyword: string) {
    const trimmed = keyword.trim();
    const debouncedKeyword = useDebouncedValue(trimmed, SEARCH_DEBOUNCE_MS);

    const query = useQuery({
        queryKey: ['integratedSearch', debouncedKeyword, INTEGRATED_SEARCH_PAGE_SIZE] as const,
        queryFn: ({ signal }) =>
            searchIntegrated(debouncedKeyword, INTEGRATED_SEARCH_PAGE_SIZE, signal),
        enabled: debouncedKeyword.length > 0,
        staleTime: SEARCH_STALE_TIME_MS,
        gcTime: 10 * 60 * 1000,
        placeholderData: keepPreviousData,
    });

    return {
        ...query,
        debouncedKeyword,
        isDebouncing: trimmed.length > 0 && trimmed !== debouncedKeyword,
    };
}
