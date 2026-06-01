import { useState, useEffect } from 'react';
import type { Transaction, Category } from '../types';
import { getTransactions } from '../api/transactions';
import { getCategories } from '../api/categories';
import { TransactionModal } from '../components/TransactionModal';
import { CategoryModal } from '../components/CategoryModal';

type TypeFilter = '' | 'income' | 'expense';

export function TransactionsPage() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const size = 10;
  const [total, setTotal] = useState(0);
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [refreshKey, setRefreshKey] = useState(0);

  // Modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState<Transaction | undefined>(undefined);
  const [categoryModalOpen, setCategoryModalOpen] = useState(false);

  const totalPages = Math.ceil(total / size);
  const refresh = () => setRefreshKey(k => k + 1);

  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch(() => setCategories([]));
  }, []);

  useEffect(() => {
    setLoading(true);
    const params = {
      page,
      size,
      ...(typeFilter && { type: typeFilter }),
      ...(startDate && { startDate }),
      ...(endDate && { endDate }),
    };
    getTransactions(params)
      .then(result => {
        setTransactions(result.list);
        setTotal(result.total);
      })
      .catch(() => {
        setTransactions([]);
        setTotal(0);
      })
      .finally(() => setLoading(false));
  }, [page, typeFilter, startDate, endDate, refreshKey]);

  function handleTypeChange(value: TypeFilter) {
    setTypeFilter(value);
    setPage(1);
  }

  function handleStartDate(value: string) {
    setStartDate(value);
    setPage(1);
  }

  function handleEndDate(value: string) {
    setEndDate(value);
    setPage(1);
  }

  function getCategoryName(categoryId: number | null): string {
    if (categoryId === null) return '—';
    const cat = categories.find(c => c.id === categoryId);
    return cat ? cat.name : '—';
  }

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold">Transactions</h1>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setCategoryModalOpen(true)}
            className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50"
          >
            ⚙ Categories
          </button>
          <button
            onClick={() => { setEditingTransaction(undefined); setModalOpen(true); }}
            className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
          >
            + New Transaction
          </button>
        </div>
      </div>

      {/* Filter bar */}
      <div className="flex flex-wrap gap-3 mb-4">
        <select
          className="border border-gray-300 rounded px-3 py-1.5 text-sm"
          value={typeFilter}
          onChange={e => handleTypeChange(e.target.value as TypeFilter)}
        >
          <option value="">All types</option>
          <option value="income">Income</option>
          <option value="expense">Expense</option>
        </select>

        <input
          type="date"
          className="border border-gray-300 rounded px-3 py-1.5 text-sm"
          value={startDate}
          onChange={e => handleStartDate(e.target.value)}
        />
        <input
          type="date"
          className="border border-gray-300 rounded px-3 py-1.5 text-sm"
          value={endDate}
          onChange={e => handleEndDate(e.target.value)}
        />
      </div>

      {/* Table */}
      {loading ? (
        <div className="flex justify-center items-center h-40">
          <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : transactions.length === 0 ? (
        <div className="flex flex-col justify-center items-center h-40 gap-3 text-gray-400 text-sm">
          <p>No transactions found.</p>
          <button
            onClick={() => { setEditingTransaction(undefined); setModalOpen(true); }}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            Add your first transaction
          </button>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-sm border-collapse">
            <thead>
              <tr className="bg-gray-100 text-left">
                <th className="px-4 py-2 border-b">ID</th>
                <th className="px-4 py-2 border-b">Date</th>
                <th className="px-4 py-2 border-b">Description</th>
                <th className="px-4 py-2 border-b">Category</th>
                <th className="px-4 py-2 border-b text-right">Amount</th>
                <th className="px-4 py-2 border-b"></th>
              </tr>
            </thead>
            <tbody>
              {transactions.map(tx => (
                <tr key={tx.id} className="hover:bg-gray-50 border-b last:border-b-0">
                  <td className="px-4 py-2 text-gray-500">{tx.id}</td>
                  <td className="px-4 py-2">{tx.transactionDate}</td>
                  <td className="px-4 py-2">{tx.description}</td>
                  <td className="px-4 py-2">{getCategoryName(tx.categoryId)}</td>
                  <td className={`px-4 py-2 text-right font-medium ${tx.type === 'income' ? 'text-green-600' : 'text-red-500'}`}>
                    {tx.type === 'income' ? '+' : '-'}
                    {tx.amount.toFixed(2)}
                  </td>
                  <td className="px-4 py-2">
                    <button
                      onClick={() => { setEditingTransaction(tx); setModalOpen(true); }}
                      className="text-blue-600 hover:underline text-sm"
                    >
                      Edit
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {!loading && totalPages > 0 && (
        <div className="flex items-center gap-3 mt-4 text-sm">
          <button
            className="px-3 py-1 border rounded disabled:opacity-40"
            onClick={() => setPage(p => p - 1)}
            disabled={page <= 1}
          >
            Previous
          </button>
          <span className="text-gray-600">
            Page {page} of {totalPages}
          </span>
          <button
            className="px-3 py-1 border rounded disabled:opacity-40"
            onClick={() => setPage(p => p + 1)}
            disabled={page >= totalPages}
          >
            Next
          </button>
          <span className="text-gray-400 ml-2">{total} total</span>
        </div>
      )}

      {/* Category modal */}
      <CategoryModal
        isOpen={categoryModalOpen}
        onClose={() => setCategoryModalOpen(false)}
        onSuccess={() => {
          getCategories().then(setCategories).catch(() => setCategories([]));
        }}
      />

      {/* Transaction modal */}
      <TransactionModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        onSuccess={() => { setModalOpen(false); refresh(); }}
        transaction={editingTransaction}
      />
    </div>
  );
}
