import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';

// Started conditionally from main.tsx when VITE_USE_MOCK_DATA=true - lets the whole GUI run in
// a real browser against dummy data with zero backend services running (see npm run dev:mock).
export const worker = setupWorker(...handlers);
