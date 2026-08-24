import { setupServer } from 'msw/node';
import { handlers } from './handlers';

// Used by src/test/setup.ts for every component test - same handlers/fixtures as browser mock
// mode, so a component that works in a test works the same way in `npm run dev:mock`.
export const server = setupServer(...handlers);
