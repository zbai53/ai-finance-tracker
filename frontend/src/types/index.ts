//Harmonize response format (corresponds to back-end Result<T>)
export interface Result<T> {
    code: number;
    message: string;
    data: T;
  }

  // user
  export interface User {
    id: number;
    username: string;
    email: string;
  }

  // Transaction
  export interface Transaction {
    id: number;
    userId: number;
    categoryId: number | null;
    amount: number;
    type: 'income' | 'expense';
    description: string;
    transactionDate: string;
    aiCategory: string | null;
    createdAt: string | null;
    updatedAt: string | null;
  }

  // Category
  export interface Category {
    id: number;
    userId: number;
    name: string;
    type: 'income' | 'expense';
    createdAt: string | null;
  }

  // Pagination results (corresponds to back-end PageInfo)
  export interface PageResult<T> {
    total: number;
    list: T[];
    pageNum: number;
    pageSize: number;
    hasNextPage: boolean;
    hasPreviousPage: boolean;
  }

  // statistic
  export interface CategoryStatistics {
    categoryId: number | null;
    categoryName: string | null;
    total: number;
    count: number;
  }

  export interface MonthlySummary {
    totalIncome: number;
    totalExpense: number;
    net: number;
  }
