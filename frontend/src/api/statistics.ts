import http from './http';
import type { Result, MonthlySummary, CategoryStatistics } from '../types';

export const getMonthlySummary = (year: number, month: number): Promise<MonthlySummary> =>
  http.get<Result<MonthlySummary>>('/api/statistics/monthly', { params: { year, month } })
    .then(r => r.data.data);

export const getCategoryStatistics = (type: 'income' | 'expense'): Promise<CategoryStatistics[]> =>
  http.get<Result<CategoryStatistics[]>>('/api/statistics/by-category', { params: { type } })
    .then(r => r.data.data);
