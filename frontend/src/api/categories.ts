import http from './http';
import type { Result, Category } from '../types';

export const getCategories = (): Promise<Category[]> =>
  http.get<Result<Category[]>>('/api/categories')
    .then(r => r.data.data);

export const createCategory = (data: { name: string; type: string }): Promise<Category> =>
  http.post<Result<Category>>('/api/categories', data)
    .then(r => r.data.data);

export const deleteCategory = (id: number): Promise<void> =>
  http.delete(`/api/categories/${id}`)
    .then(() => undefined);
