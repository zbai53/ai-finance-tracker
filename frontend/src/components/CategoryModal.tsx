import { useState, useEffect } from 'react';
import type { Category } from '../types';
import { getCategories, createCategory, deleteCategory } from '../api/categories';

interface CategoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

interface AddForm {
  name: string;
  type: 'income' | 'expense';
}

export function CategoryModal({ isOpen, onClose, onSuccess }: CategoryModalProps) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [listLoading, setListLoading] = useState(false);
  const [form, setForm] = useState<AddForm>({ name: '', type: 'expense' });
  const [adding, setAdding] = useState(false);
  const [addError, setAddError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [confirmId, setConfirmId] = useState<number | null>(null);

  function fetchCategories() {
    setListLoading(true);
    getCategories()
      .then(setCategories)
      .catch(() => setCategories([]))
      .finally(() => setListLoading(false));
  }

  useEffect(() => {
    if (isOpen) {
      fetchCategories();
      setForm({ name: '', type: 'expense' });
      setAddError(null);
      setConfirmId(null);
    }
  }, [isOpen]);

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    const name = form.name.trim();
    if (!name) return;
    setAdding(true);
    setAddError(null);
    try {
      await createCategory({ name, type: form.type });
      setForm(prev => ({ ...prev, name: '' }));
      fetchCategories();
      onSuccess();
    } catch (err: unknown) {
      setAddError(err instanceof Error ? err.message : 'Failed to add category.');
    } finally {
      setAdding(false);
    }
  }

  async function handleDelete(id: number) {
    if (confirmId !== id) {
      setConfirmId(id);
      return;
    }
    setDeletingId(id);
    setConfirmId(null);
    try {
      await deleteCategory(id);
      fetchCategories();
      onSuccess();
    } catch {
      // silently restore — could show a toast here
    } finally {
      setDeletingId(null);
    }
  }

  if (!isOpen) return null;

  const income  = categories.filter(c => c.type === 'income');
  const expense = categories.filter(c => c.type === 'expense');

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      onClick={e => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="relative w-full max-w-md rounded-xl bg-white p-6 shadow-xl">

        {/* Header */}
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-800">Categories</h2>
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

        {/* Category list */}
        {listLoading ? (
          <div className="flex h-32 items-center justify-center">
            <div className="h-6 w-6 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
          </div>
        ) : (
          <div className="mb-5 max-h-72 overflow-y-auto space-y-4 pr-1">
            {(['income', 'expense'] as const).map(type => {
              const group: Category[] = type === 'income' ? income : expense;
              return (
                <div key={type}>
                  <p className={`mb-1.5 text-xs font-semibold uppercase tracking-wide ${
                    type === 'income' ? 'text-green-600' : 'text-red-500'
                  }`}>
                    {type === 'income' ? 'Income' : 'Expense'}
                  </p>
                  {group.length === 0 ? (
                    <p className="text-sm text-gray-400 italic">No categories yet.</p>
                  ) : (
                    <ul className="space-y-1">
                      {group.map(cat => (
                        <li
                          key={cat.id}
                          className="flex items-center justify-between rounded-lg border border-gray-100 bg-gray-50 px-3 py-2"
                        >
                          <span className="text-sm text-gray-800">{cat.name}</span>

                          {confirmId === cat.id ? (
                            <div className="flex items-center gap-1.5">
                              <span className="text-xs text-gray-500">Delete?</span>
                              <button
                                type="button"
                                onClick={() => handleDelete(cat.id)}
                                disabled={deletingId === cat.id}
                                className="rounded bg-red-600 px-2 py-0.5 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50"
                              >
                                Yes
                              </button>
                              <button
                                type="button"
                                onClick={() => setConfirmId(null)}
                                className="rounded border border-gray-300 px-2 py-0.5 text-xs font-medium text-gray-600 hover:bg-gray-100"
                              >
                                No
                              </button>
                            </div>
                          ) : (
                            <button
                              type="button"
                              onClick={() => handleDelete(cat.id)}
                              disabled={deletingId === cat.id}
                              className="rounded border border-red-200 px-2 py-0.5 text-xs font-medium text-red-500 hover:bg-red-50 disabled:opacity-40"
                            >
                              {deletingId === cat.id ? '…' : 'Delete'}
                            </button>
                          )}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {/* Divider */}
        <div className="mb-4 border-t border-gray-200" />

        {/* Add form */}
        <form onSubmit={handleAdd} className="space-y-3">
          <p className="text-sm font-medium text-gray-700">Add Category</p>

          {addError && (
            <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {addError}
            </div>
          )}

          <div className="flex gap-2">
            <input
              type="text"
              value={form.name}
              onChange={e => setForm(prev => ({ ...prev, name: e.target.value }))}
              placeholder="Category name"
              required
              className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
            <select
              value={form.type}
              onChange={e => setForm(prev => ({ ...prev, type: e.target.value as 'income' | 'expense' }))}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            >
              <option value="expense">Expense</option>
              <option value="income">Income</option>
            </select>
          </div>

          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50"
            >
              Close
            </button>
            <button
              type="submit"
              disabled={adding}
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {adding ? 'Adding…' : 'Add'}
            </button>
          </div>
        </form>

      </div>
    </div>
  );
}
