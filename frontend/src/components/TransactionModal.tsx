import { useState, useEffect } from 'react';
import type { Transaction, Category } from '../types';
import type { CreateTransactionRequest } from '../api/transactions';
import { createTransaction, updateTransaction, deleteTransaction } from '../api/transactions';
import { getCategories } from '../api/categories';

interface TransactionModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  transaction?: Transaction;
}

interface FormState {
  amount: string;
  type: 'income' | 'expense';
  categoryId: string;
  description: string;
  transactionDate: string;
}

function toDateInputValue(dateStr: string): string {
  // transactionDate may be ISO or "YYYY-MM-DD"; take just the date part
  return dateStr.slice(0, 10);
}

export function TransactionModal({ isOpen, onClose, onSuccess, transaction }: TransactionModalProps) {
  const isEdit = transaction !== undefined;

  const [form, setForm] = useState<FormState>({
    amount: '',
    type: 'expense',
    categoryId: '',
    description: '',
    transactionDate: new Date().toISOString().slice(0, 10),
  });
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);

  // Populate form when editing
  useEffect(() => {
    if (transaction) {
      setForm({
        amount: String(transaction.amount),
        type: transaction.type,
        categoryId: transaction.categoryId !== null ? String(transaction.categoryId) : '',
        description: transaction.description,
        transactionDate: toDateInputValue(transaction.transactionDate),
      });
    } else {
      setForm({
        amount: '',
        type: 'expense',
        categoryId: '',
        description: '',
        transactionDate: new Date().toISOString().slice(0, 10),
      });
    }
    setError(null);
    setConfirmDelete(false);
  }, [transaction, isOpen]);

  // Fetch categories whenever the modal opens
  useEffect(() => {
    if (isOpen) {
      getCategories()
        .then(setCategories)
        .catch(() => setCategories([]));
    }
  }, [isOpen]);

  const filteredCategories = categories.filter(c => c.type === form.type);

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) {
    const { name, value } = e.target;
    setForm(prev => {
      // Reset categoryId when type changes to avoid cross-type mismatch
      if (name === 'type') {
        return { ...prev, type: value as 'income' | 'expense', categoryId: '' };
      }
      return { ...prev, [name]: value };
    });
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    const parsed = parseFloat(form.amount);
    if (isNaN(parsed) || parsed <= 0) {
      setError('Amount must be a positive number.');
      return;
    }

    const payload: CreateTransactionRequest = {
      amount: parsed,
      type: form.type,
      description: form.description,
      transactionDate: form.transactionDate,
      ...(form.categoryId !== '' ? { categoryId: Number(form.categoryId) } : {}),
    };

    setLoading(true);
    try {
      if (isEdit) {
        await updateTransaction(transaction.id, payload);
      } else {
        await createTransaction(payload);
      }
      onSuccess();
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'An unexpected error occurred.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    if (!isEdit) return;
    if (!confirmDelete) {
      setConfirmDelete(true);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await deleteTransaction(transaction.id);
      onSuccess();
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to delete transaction.';
      setError(msg);
      setConfirmDelete(false);
    } finally {
      setLoading(false);
    }
  }

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="relative w-full max-w-md rounded-xl bg-white p-6 shadow-xl">
        {/* Header */}
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-800">
            {isEdit ? 'Edit Transaction' : 'New Transaction'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
            aria-label="Close"
          >
            <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
              <path
                fillRule="evenodd"
                d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                clipRule="evenodd"
              />
            </svg>
          </button>
        </div>

        {/* Error */}
        {error && (
          <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Amount */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="amount">
              Amount
            </label>
            <input
              id="amount"
              name="amount"
              type="number"
              min="0.01"
              step="0.01"
              required
              value={form.amount}
              onChange={handleChange}
              placeholder="0.00"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          {/* Type */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="type">
              Type
            </label>
            <select
              id="type"
              name="type"
              value={form.type}
              onChange={handleChange}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            >
              <option value="expense">Expense</option>
              <option value="income">Income</option>
            </select>
          </div>

          {/* Category */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="categoryId">
              Category
            </label>
            <select
              id="categoryId"
              name="categoryId"
              value={form.categoryId}
              onChange={handleChange}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            >
              <option value="">-- None --</option>
              {filteredCategories.map(c => (
                <option key={c.id} value={String(c.id)}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          {/* Description */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="description">
              Description
            </label>
            <input
              id="description"
              name="description"
              type="text"
              value={form.description}
              onChange={handleChange}
              placeholder="Optional note"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          {/* Date */}
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700" htmlFor="transactionDate">
              Date
            </label>
            <input
              id="transactionDate"
              name="transactionDate"
              type="date"
              required
              value={form.transactionDate}
              onChange={handleChange}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
          </div>

          {/* Actions */}
          <div className="flex items-center justify-between pt-2">
            {/* Delete (edit mode only) */}
            {isEdit && (
              <div className="flex items-center gap-2">
                {confirmDelete ? (
                  <>
                    <span className="text-sm text-gray-600">Are you sure?</span>
                    <button
                      type="button"
                      onClick={handleDelete}
                      disabled={loading}
                      className="rounded-lg bg-red-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
                    >
                      Yes, delete
                    </button>
                    <button
                      type="button"
                      onClick={() => setConfirmDelete(false)}
                      disabled={loading}
                      className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-50"
                    >
                      Cancel
                    </button>
                  </>
                ) : (
                  <button
                    type="button"
                    onClick={handleDelete}
                    disabled={loading}
                    className="rounded-lg border border-red-300 px-3 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50 disabled:opacity-50"
                  >
                    Delete
                  </button>
                )}
              </div>
            )}

            {/* Submit / Cancel */}
            <div className={`flex gap-2 ${!isEdit ? 'ml-auto' : ''}`}>
              <button
                type="button"
                onClick={onClose}
                disabled={loading}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading}
                className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                {loading ? 'Saving…' : isEdit ? 'Save Changes' : 'Add Transaction'}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
