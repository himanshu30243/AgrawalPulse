import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Alert from '@mui/material/Alert';
import Typography from '@mui/material/Typography';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import SearchIcon from '@mui/icons-material/Search';
import { familiesApi } from '@/api/familiesApi';
import { membershipApi } from '@/api/membershipApi';
import { currentFinancialYear, financialYearOptions, financialYearLabel } from './financialYear';
import type { Family, MembershipTransaction, PaymentMode } from '@/types/domain';

const PAYMENT_MODES: PaymentMode[] = ['CASH', 'UPI', 'CARD', 'BANK_TRANSFER', 'CHEQUE'];

interface Props {
  /** Known up-front - skips the search step. */
  initialFamily?: Family | null;
  /** Set to edit an existing transaction instead of recording a new one. */
  transaction?: MembershipTransaction | null;
  onClose: () => void;
  onSaved: () => void;
}

// Search-then-create for a new transaction, or a direct edit of an existing one - one dialog for
// both since the payment fields are identical (see UpdateTransactionRequest's comment: only
// familyId/financialYear are immutable once recorded). Family search reuses familiesApi's
// headOfFamilyName/mobileNumber/areaLocality filters, scoped server-side to what this admin may
// already see (see family-service's FamilyController#listFamilies).
export function RecordTransactionDialog({ initialFamily, transaction, onClose, onSaved }: Props) {
  const { t } = useTranslation();
  const isEdit = Boolean(transaction);

  const [family, setFamily] = useState<Family | null>(initialFamily ?? null);
  const [searchTerm, setSearchTerm] = useState('');
  const [searchResults, setSearchResults] = useState<Family[] | null>(null);
  const [isSearching, setIsSearching] = useState(false);

  const [financialYear, setFinancialYear] = useState(transaction?.financialYear ?? currentFinancialYear());
  const [amount, setAmount] = useState(transaction ? String(transaction.amount) : '');
  const [paymentDate, setPaymentDate] = useState(transaction?.paymentDate ?? new Date().toISOString().slice(0, 10));
  const [paymentMode, setPaymentMode] = useState<PaymentMode>(transaction?.paymentMode ?? 'CASH');
  const [transactionRef, setTransactionRef] = useState(transaction?.transactionRef ?? '');
  const [remarks, setRemarks] = useState(transaction?.remarks ?? '');

  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Field-level, not just the generic `error` Alert above: a blank/invalid Amount must be called
  // out on the field itself (helperText + red outline) so it's clear which field needs fixing,
  // not just that "something" is wrong. Cleared as soon as the user edits the field again.
  const [amountError, setAmountError] = useState<string | null>(null);

  const handleSearch = async () => {
    setIsSearching(true);
    setError(null);
    try {
      // familiesApi.list's filters are ANDed together server-side (see FamilyServiceImpl's
      // applySearchFilters), so a single search box can't send the same term to all three at
      // once - that would require every field to match simultaneously. A numeric-looking term is
      // treated as a mobile number search, otherwise as a head-of-family name search.
      const trimmed = searchTerm.trim();
      const isNumeric = /^\d+$/.test(trimmed);
      const results = await familiesApi.list(
        isNumeric ? { mobileNumber: trimmed } : { headOfFamilyName: trimmed },
      );
      setSearchResults(results);
    } catch {
      setError(t('common.errorGeneric'));
    } finally {
      setIsSearching(false);
    }
  };

  const handleSave = async () => {
    if (!family) return;

    // Validated on submit, not by silently disabling Save - a disabled button with no visible
    // reason is indistinguishable from a broken one. The backend enforces the same rule
    // (RecordTransactionRequest/UpdateTransactionRequest's @NotNull @DecimalMin("0.01") on
    // amount) - this is a UX shortcut in front of it, not a substitute for it.
    const parsedAmount = Number(amount);
    if (amount.trim() === '' || Number.isNaN(parsedAmount) || parsedAmount <= 0) {
      setAmountError(t('membership.amountRequired'));
      return;
    }
    setAmountError(null);

    setIsSaving(true);
    setError(null);
    try {
      if (isEdit && transaction) {
        await membershipApi.updateTransaction(transaction.id, {
          amount: parsedAmount,
          paymentDate,
          paymentMode,
          transactionRef,
          remarks,
        });
      } else {
        await membershipApi.recordTransaction({
          familyId: family.id,
          financialYear,
          amount: parsedAmount,
          paymentDate,
          paymentMode,
          transactionRef,
          remarks,
        });
      }
      onSaved();
    } catch {
      setError(isEdit ? t('membership.updateTransactionError') : t('membership.recordTransactionError'));
    } finally {
      setIsSaving(false);
    }
  };

  // Amount is deliberately NOT part of this gate - Save must stay clickable when Amount is blank
  // so clicking it can actually show the "Amount is required" validation feedback (see
  // handleSave). A silently-disabled button gives the user no way to discover why nothing happens.
  const canSave = Boolean(family) && paymentDate !== '';

  return (
    <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        {isEdit ? t('membership.editTransaction') : t('membership.recordTransaction')}
      </DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2.5} sx={{ mt: 0.5 }}>
          {error && <Alert severity="error">{error}</Alert>}

          {!family && (
            <Stack spacing={1.5}>
              <Typography variant="subtitle2">{t('membership.searchFamily')}</Typography>
              <Stack direction="row" spacing={1}>
                <TextField
                  size="small"
                  fullWidth
                  placeholder={t('membership.searchFamilyPlaceholder')}
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') void handleSearch();
                  }}
                />
                <Button
                  variant="outlined"
                  startIcon={<SearchIcon />}
                  disabled={isSearching || searchTerm.trim() === ''}
                  onClick={() => void handleSearch()}
                >
                  {t('common.search')}
                </Button>
              </Stack>
              {searchResults && searchResults.length === 0 && (
                <Typography color="text.secondary" variant="body2">
                  {t('membership.noFamiliesFound')}
                </Typography>
              )}
              {searchResults && searchResults.length > 0 && (
                <List dense sx={{ maxHeight: 240, overflow: 'auto' }}>
                  {searchResults.map((result) => (
                    <ListItemButton key={result.id} onClick={() => setFamily(result)}>
                      <ListItemText
                        primary={result.headOfFamilyName}
                        secondary={`${result.familyCode} · ${result.mobileNumber || '—'}`}
                      />
                    </ListItemButton>
                  ))}
                </List>
              )}
            </Stack>
          )}

          {family && (
            <Stack spacing={2}>
              <Alert
                severity="info"
                action={
                  !isEdit && !initialFamily ? (
                    <Button size="small" onClick={() => setFamily(null)}>
                      {t('membership.changeFamily')}
                    </Button>
                  ) : undefined
                }
              >
                {family.headOfFamilyName} ({family.familyCode})
              </Alert>

              <TextField
                select
                label={t('membership.financialYear')}
                value={financialYear}
                onChange={(e) => setFinancialYear(Number(e.target.value))}
                size="small"
                disabled={isEdit}
              >
                {financialYearOptions().map((year) => (
                  <MenuItem key={year} value={year}>
                    {financialYearLabel(year)}
                  </MenuItem>
                ))}
              </TextField>

              <TextField
                label={t('membership.amount')}
                type="number"
                value={amount}
                onChange={(e) => {
                  setAmount(e.target.value);
                  if (amountError) setAmountError(null);
                }}
                size="small"
                required
                error={Boolean(amountError)}
                helperText={amountError ?? undefined}
              />

              <TextField
                label={t('membership.paymentDate')}
                type="date"
                value={paymentDate}
                onChange={(e) => setPaymentDate(e.target.value)}
                size="small"
                InputLabelProps={{ shrink: true }}
                required
              />

              <TextField
                select
                label={t('membership.paymentMode')}
                value={paymentMode}
                onChange={(e) => setPaymentMode(e.target.value as PaymentMode)}
                size="small"
              >
                {PAYMENT_MODES.map((mode) => (
                  <MenuItem key={mode} value={mode}>
                    {t(`membership.paymentMode${mode}`)}
                  </MenuItem>
                ))}
              </TextField>

              <TextField
                label={t('membership.transactionRef')}
                value={transactionRef}
                onChange={(e) => setTransactionRef(e.target.value)}
                size="small"
              />

              <TextField
                label={t('membership.remarks')}
                value={remarks}
                onChange={(e) => setRemarks(e.target.value)}
                size="small"
                multiline
                minRows={2}
              />
            </Stack>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common.cancel')}</Button>
        <Button variant="contained" disabled={!canSave || isSaving} onClick={() => void handleSave()}>
          {isSaving ? t('common.loading') : t('common.save')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
