export interface ChapterOption {
  id: string;
  name: string;
}

// No Chapters API exists yet (chapter admin is out of this slice's scope);
// hardcoded so Family Registration and Membership pages have a real,
// working chapter picker rather than a free-text field. Swap for
// `chaptersApi.list()` once that endpoint lands.
export const CHAPTERS: ChapterOption[] = [
  { id: 'chp-delhi', name: 'Delhi NCR' },
  { id: 'chp-jaipur', name: 'Jaipur' },
  { id: 'chp-mumbai', name: 'Mumbai' },
  { id: 'chp-kolkata', name: 'Kolkata' },
  { id: 'chp-lucknow', name: 'Lucknow' },
  { id: 'chp-indore', name: 'Indore' },
];
