import { useState, useRef } from 'react';

const BASE_URL = import.meta.env.VITE_API_BASE_URL as string;

interface UseAiStreamResult {
  content: string;
  isStreaming: boolean;
  error: string | null;
  start: (prompt: string) => void;
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

  function start(prompt: string) {
    // Close any previous connection
    closeSource();
    setContent('');
    setError(null);
    setIsStreaming(true);

    const token = localStorage.getItem('token') ?? '';
    const url = `${BASE_URL}/api/ai/stream?prompt=${encodeURIComponent(prompt)}&token=${encodeURIComponent(token)}`;

    const source = new EventSource(url);
    sourceRef.current = source;

    source.onmessage = (event: MessageEvent<string>) => {
      setContent(prev => prev + event.data);
    };

    source.onerror = () => {
      setError('Stream connection failed or ended unexpectedly.');
      setIsStreaming(false);
      closeSource();
    };

    // Spring's SseEmitter sends a final "complete" event when done;
    // browsers also fire onerror when the server closes the connection normally,
    // so onerror doubles as the "done" handler. For an explicit done signal:
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
