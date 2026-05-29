import { useState, useRef } from 'react';

const BASE_URL = import.meta.env.VITE_API_BASE_URL as string;

interface UseAiStreamResult {
  content: string;
  isStreaming: boolean;
  error: string | null;
  /** Pass the full URL including all query params (token, prompt/year/month, etc.) */
  start: (url: string) => void;
  reset: () => void;
}

export function useAiStream(): UseAiStreamResult {
  const [content, setContent] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const sourceRef = useRef<EventSource | null>(null);

  function closeSource() {
    if (sourceRef.current) {
      sourceRef.current.close();
      sourceRef.current = null;
    }
  }

  function start(url: string) {
    closeSource();
    setContent('');
    setError(null);
    setIsStreaming(true);

    const source = new EventSource(url);
    sourceRef.current = source;

    source.onmessage = (event: MessageEvent<string>) => {
      setContent(prev => prev + event.data);
    };

    // onerror fires both on real errors AND when the server closes the stream
    // normally (browsers can't distinguish). We treat it as "done" generically.
    source.onerror = () => {
      setIsStreaming(false);
      closeSource();
    };

    // Explicit clean-close signal sent by Spring's SseEmitter
    source.addEventListener('complete', () => {
      setIsStreaming(false);
      closeSource();
    });
  }

  function reset() {
    closeSource();
    setContent('');
    setError(null);
    setIsStreaming(false);
  }

  return { content, isStreaming, error, start, reset };
}

/** Build the URL for the free-form stream endpoint */
export function buildStreamUrl(prompt: string): string {
  const token = localStorage.getItem('token') ?? '';
  return `${BASE_URL}/api/ai/stream?prompt=${encodeURIComponent(prompt)}&token=${encodeURIComponent(token)}`;
}

/** Build the URL for the monthly report endpoint */
export function buildReportUrl(year: number, month: number): string {
  const token = localStorage.getItem('token') ?? '';
  return `${BASE_URL}/api/ai/report?year=${year}&month=${month}&token=${encodeURIComponent(token)}`;
}

/** Build the URL for the natural-language query endpoint */
export function buildQueryUrl(question: string): string {
  const token = localStorage.getItem('token') ?? '';
  return `${BASE_URL}/api/ai/query?question=${encodeURIComponent(question)}&token=${encodeURIComponent(token)}`;
}

/** Build the URL for the spending anomaly detection endpoint */
export function buildAnomalyUrl(year: number, month: number): string {
  const token = localStorage.getItem('token') ?? '';
  return `${BASE_URL}/api/ai/anomalies?year=${year}&month=${month}&token=${encodeURIComponent(token)}`;
}
