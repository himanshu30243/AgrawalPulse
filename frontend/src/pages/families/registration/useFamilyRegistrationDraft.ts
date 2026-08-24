import { emptyWizardState } from './types';
import type { WizardFormState } from './types';

const DRAFT_KEY = 'agrawalpulse:family-registration-draft';

// `photo` (a File) can't survive JSON serialization, so it's simply dropped from what's
// persisted - restoring a draft never restores a previously-selected photo, the user re-attaches
// it if they still want one. Everything else round-trips as plain JSON.
type PersistableDraft = Omit<WizardFormState, 'photo'>;

export function loadDraft(): WizardFormState | null {
  const raw = localStorage.getItem(DRAFT_KEY);
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as PersistableDraft;
    return { ...emptyWizardState(), ...parsed, photo: null };
  } catch {
    return null;
  }
}

export function saveDraft(values: WizardFormState): void {
  const { photo: _photo, ...persistable } = values;
  localStorage.setItem(DRAFT_KEY, JSON.stringify(persistable));
}

export function clearDraft(): void {
  localStorage.removeItem(DRAFT_KEY);
}

export function hasDraft(): boolean {
  return localStorage.getItem(DRAFT_KEY) !== null;
}
