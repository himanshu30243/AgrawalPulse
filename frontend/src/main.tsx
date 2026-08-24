import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './i18n/i18n';
import App from './App';

function renderApp() {
  createRoot(document.getElementById('root') as HTMLElement).render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
}

// VITE_USE_MOCK_DATA=true (see .env.mock.example / npm run dev:mock) runs the entire GUI
// against MSW's in-memory fake backend - no family-service/user-service/etc. needs to be
// running at all. Same handlers/fixtures the component tests use (src/mocks/handlers.ts), so
// what you click through here is what the tests are actually asserting on.
if (import.meta.env.VITE_USE_MOCK_DATA === 'true') {
  const { worker } = await import('./mocks/browser');
  await worker.start({ onUnhandledRequest: 'bypass' });
}

renderApp();
