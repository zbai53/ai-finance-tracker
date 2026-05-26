import { useState, useEffect } from 'react';
import { useAiStream, buildReportUrl } from '../hooks/useAiStream';
import {
  ResponsiveContainer,
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
  PieChart, Pie, Cell,
} from 'recharts';
import { getMonthlySummary, getCategoryStatistics } from '../api/statistics';
import type { MonthlySummary, CategoryStatistics } from '../types';
import ReactMarkdown from 'react-markdown';

// ─── helpers ────────────────────────────────────────────────────────────────

const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                     'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const LONG_MONTH_NAMES = ['January', 'February', 'March', 'April', 'May', 'June',
                          'July', 'August', 'September', 'October', 'November', 'December'];

const PIE_COLORS = ['#6366f1', '#f59e0b', '#10b981', '#ef4444',
                    '#3b82f6', '#ec4899', '#8b5cf6', '#14b8a6'];

function prevMonth(year: number, month: number): [number, number] {
  return month === 1 ? [year - 1, 12] : [year, month - 1];
}
function nextMonth(year: number, month: number): [number, number] {
  return month === 12 ? [year + 1, 1] : [year, month + 1];
}

// ─── types ───────────────────────────────────────────────────────────────────

interface LineDataPoint {
  label: string;
  income: number;
  expense: number;
}

// ─── sub-components ──────────────────────────────────────────────────────────

interface SummaryCardProps {
  title: string;
  value: number;
  color: 'green' | 'red' | 'blue';
  loading: boolean;
}

function SummaryCard({ title, value, color, loading }: SummaryCardProps) {
  const colorMap: Record<SummaryCardProps['color'], string> = {
    green: 'bg-green-50 border-green-200',
    red:   'bg-red-50 border-red-200',
    blue:  'bg-blue-50 border-blue-200',
  };
  const labelColor: Record<SummaryCardProps['color'], string> = {
    green: 'text-green-600',
    red:   'text-red-500',
    blue:  'text-blue-600',
  };

  return (
    <div className={`rounded-xl border p-5 ${colorMap[color]}`}>
      <p className={`mb-1 text-sm font-medium ${labelColor[color]} opacity-80`}>{title}</p>
      {loading ? (
        <div className="h-8 w-24 animate-pulse rounded bg-gray-200" />
      ) : (
        <p className={`text-2xl font-bold ${labelColor[color]}`}>
          ${Math.abs(value).toLocaleString('en-US', { minimumFractionDigits: 2 })}
        </p>
      )}
    </div>
  );
}

// ─── main component ──────────────────────────────────────────────────────────

export function DashboardPage() {
  const { content: reportContent, isStreaming: reportStreaming, start: startReport, reset: resetReport } = useAiStream();
  const now = new Date();

  const [selectedYear, setSelectedYear] = useState(now.getFullYear());
  const [selectedMonth, setSelectedMonth] = useState(now.getMonth() + 1); // 1-based

  const [summary, setSummary] = useState<MonthlySummary | null>(null);
  const [summaryLoading, setSummaryLoading] = useState(false);

  const [lineData, setLineData] = useState<LineDataPoint[]>([]);
  const [lineLoading, setLineLoading] = useState(false);

  const [pieData, setPieData] = useState<CategoryStatistics[]>([]);
  const [pieLoading, setPieLoading] = useState(false);

  // Fetch summary for selected month
  useEffect(() => {
    setSummaryLoading(true);
    getMonthlySummary(selectedYear, selectedMonth)
      .then(setSummary)
      .catch(() => setSummary(null))
      .finally(() => setSummaryLoading(false));
  }, [selectedYear, selectedMonth]);

  // Fetch last-6-months line chart data
  useEffect(() => {
    setLineLoading(true);
    const points: Array<{ year: number; month: number }> = [];
    let y = selectedYear;
    let m = selectedMonth;
    for (let i = 0; i < 6; i++) {
      points.unshift({ year: y, month: m });
      [y, m] = prevMonth(y, m);
    }

    Promise.all(
      points.map(({ year, month }) =>
        getMonthlySummary(year, month).then(s => ({
          label: MONTH_NAMES[month - 1],
          income: s.totalIncome,
          expense: s.totalExpense,
        }))
      )
    )
      .then(setLineData)
      .catch(() => setLineData([]))
      .finally(() => setLineLoading(false));
  }, [selectedYear, selectedMonth]);

  // Fetch expense pie chart for selected month
  useEffect(() => {
    setPieLoading(true);
    getCategoryStatistics('expense')
      .then(setPieData)
      .catch(() => setPieData([]))
      .finally(() => setPieLoading(false));
  }, [selectedYear, selectedMonth]);

  function handlePrev() {
    const [y, m] = prevMonth(selectedYear, selectedMonth);
    setSelectedYear(y);
    setSelectedMonth(m);
  }
  function handleNext() {
    const [y, m] = nextMonth(selectedYear, selectedMonth);
    setSelectedYear(y);
    setSelectedMonth(m);
  }

  const netColor: 'blue' | 'red' =
    summary !== null && summary.net < 0 ? 'red' : 'blue';

  return (
    <div className="p-6">
      <h1 className="mb-6 text-2xl font-bold text-gray-800">Dashboard</h1>

      {/* ── Month selector + Report button ── */}
      <div className="mb-6 flex items-center gap-3">
        <button
          onClick={handlePrev}
          className="rounded-lg border border-gray-300 bg-white p-2 hover:bg-gray-50"
          aria-label="Previous month"
        >
          <svg className="h-4 w-4 text-gray-600" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clipRule="evenodd" />
          </svg>
        </button>
        <span className="min-w-[120px] text-center text-base font-semibold text-gray-700">
          {LONG_MONTH_NAMES[selectedMonth - 1]} {selectedYear}
        </span>
        <button
          onClick={handleNext}
          className="rounded-lg border border-gray-300 bg-white p-2 hover:bg-gray-50"
          aria-label="Next month"
        >
          <svg className="h-4 w-4 text-gray-600" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
          </svg>
        </button>

        <button
          onClick={() => startReport(buildReportUrl(selectedYear, selectedMonth))}
          disabled={reportStreaming}
          className="ml-2 rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white hover:bg-violet-700 disabled:opacity-60"
        >
          {reportStreaming ? 'Generating…' : '✦ Generate AI Report'}
        </button>
      </div>

      {/* ── Summary cards ── */}
      <div className="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <SummaryCard
          title="Total Income"
          value={summary?.totalIncome ?? 0}
          color="green"
          loading={summaryLoading}
        />
        <SummaryCard
          title="Total Expense"
          value={summary?.totalExpense ?? 0}
          color="red"
          loading={summaryLoading}
        />
        <SummaryCard
          title="Net Savings"
          value={summary?.net ?? 0}
          color={netColor}
          loading={summaryLoading}
        />
      </div>

      {/* ── Charts row ── */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">

        {/* Line chart — last 6 months */}
        <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
          <h2 className="mb-4 text-sm font-semibold text-gray-700">Income vs Expense — Last 6 Months</h2>
          {lineLoading ? (
            <div className="flex h-56 items-center justify-center text-sm text-gray-400">
              Loading chart…
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={224}>
              <LineChart data={lineData} margin={{ top: 4, right: 16, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="label" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} width={55} tickFormatter={(v: number) => `$${v.toLocaleString()}`} />
                <Tooltip formatter={(v: number) => `$${v.toLocaleString('en-US', { minimumFractionDigits: 2 })}`} />
                <Legend />
                <Line type="monotone" dataKey="income" stroke="#16a34a" strokeWidth={2} dot={false} name="Income" />
                <Line type="monotone" dataKey="expense" stroke="#dc2626" strokeWidth={2} dot={false} name="Expense" />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Pie chart — expense by category */}
        <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
          <h2 className="mb-4 text-sm font-semibold text-gray-700">
            Expense by Category — {LONG_MONTH_NAMES[selectedMonth - 1]} {selectedYear}
          </h2>
          {pieLoading ? (
            <div className="flex h-56 items-center justify-center text-sm text-gray-400">
              Loading chart…
            </div>
          ) : pieData.length === 0 ? (
            <div className="flex h-56 items-center justify-center text-sm text-gray-400">
              No expense data for this month.
            </div>
          ) : (
            <div className="flex items-center gap-4">
              <ResponsiveContainer width="60%" height={224}>
                <PieChart>
                  <Pie
                    data={pieData}
                    dataKey="total"
                    nameKey="categoryName"
                    cx="50%"
                    cy="50%"
                    outerRadius={88}
                    innerRadius={44}
                  >
                    {pieData.map((_, index) => (
                      <Cell key={index} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip
                    formatter={(value: number, name: string) => [
                      `$${value.toLocaleString('en-US', { minimumFractionDigits: 2 })}`,
                      name,
                    ]}
                  />
                </PieChart>
              </ResponsiveContainer>

              {/* Legend */}
              <ul className="flex-1 space-y-1.5 overflow-hidden">
                {pieData.map((item, index) => (
                  <li key={item.categoryId ?? index} className="flex items-center gap-2 text-xs text-gray-600 truncate">
                    <span
                      className="h-2.5 w-2.5 flex-shrink-0 rounded-full"
                      style={{ backgroundColor: PIE_COLORS[index % PIE_COLORS.length] }}
                    />
                    <span className="truncate">{item.categoryName ?? 'Uncategorized'}</span>
                    <span className="ml-auto flex-shrink-0 font-medium">
                      ${item.total.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>

      {/* ── AI Report card ── */}
      {(reportStreaming || reportContent) && (
        <div className="mt-6 rounded-xl border border-violet-200 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-violet-100 px-5 py-3">
            <div className="flex items-center gap-2">
              <span className="text-violet-600">✦</span>
              <h2 className="text-sm font-semibold text-gray-700">
                AI Financial Report — {LONG_MONTH_NAMES[selectedMonth - 1]} {selectedYear}
              </h2>
              {reportStreaming && (
                <span className="inline-block h-2 w-2 animate-pulse rounded-full bg-violet-500" />
              )}
            </div>
            <button
              onClick={resetReport}
              className="text-xs text-gray-400 hover:text-gray-600"
            >
              Clear
            </button>
          </div>

          <div className="px-5 py-4">
            {reportStreaming && !reportContent ? (
              <div className="flex items-center gap-2 text-sm text-gray-400">
                <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-violet-400 border-t-transparent" />
                Claude is thinking…
              </div>
            ) : (
              <div className="prose prose-sm max-w-none text-gray-700">
                <ReactMarkdown>{reportContent}</ReactMarkdown>
                {reportStreaming && <span className="animate-pulse">▌</span>}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
