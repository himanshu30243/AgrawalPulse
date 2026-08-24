import { useTranslation } from 'react-i18next';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import type { SelectChangeEvent } from '@mui/material/Select';
import TranslateIcon from '@mui/icons-material/Translate';
import InputAdornment from '@mui/material/InputAdornment';
import { SUPPORTED_LANGUAGES } from '@/i18n/i18n';

export function LanguageSwitcher() {
  const { i18n } = useTranslation();
  const current = SUPPORTED_LANGUAGES.some((l) => l.code === i18n.language)
    ? i18n.language
    : 'en';

  const handleChange = (event: SelectChangeEvent) => {
    void i18n.changeLanguage(event.target.value);
  };

  return (
    <Select
      value={current}
      onChange={handleChange}
      size="small"
      variant="standard"
      disableUnderline
      aria-label="Select language"
      sx={{
        color: 'inherit',
        '& .MuiSelect-icon': { color: 'inherit' },
      }}
      startAdornment={
        <InputAdornment position="start" sx={{ color: 'inherit' }}>
          <TranslateIcon fontSize="small" />
        </InputAdornment>
      }
    >
      {SUPPORTED_LANGUAGES.map((lang) => (
        <MenuItem key={lang.code} value={lang.code}>
          {lang.label}
        </MenuItem>
      ))}
    </Select>
  );
}
