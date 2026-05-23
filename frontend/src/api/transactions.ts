import http from './http';
import type { Result, Transaction, PageResult } from '../types';

export interface TransactionQuery {
  page?: number;
  size?: number;
  type?: string;
  categoryId?: number;
  startDate?: string;
  endDate?: string;
}

export interface CreateTransactionRequest {
  amount: number;
  type: 'income' | 'expense';
  description: string;
  transactionDate: string;
  categoryId?: number;
}

export const getTransactions = (params: TransactionQuery): Promise<PageResult<Transaction>> =>
  http.get<Result<PageResult<Transaction>>>('/api/transactions', { params })
    .then(r => r.data.data);

export const createTransaction = (data: CreateTransactionRequest): Promise<Transaction> =>
  http.post<Result<Transaction>>('/api/transactions', data)
    .then(r => r.data.data);

export const updateTransaction = (id: number, data: Partial<CreateTransactionRequest>): Promise<Transaction> =>
  http.put<Result<Transaction>>(`/api/transactions/${id}`, data)
    .then(r => r.data.data);

export const deleteTransaction = (id: number): Promise<void> =>
  http.delete(`/api/transactions/${id}`)
    .then(() => undefined);
