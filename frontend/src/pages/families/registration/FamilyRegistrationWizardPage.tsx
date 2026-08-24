import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import axios from 'axios';
import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import Stepper from '@mui/material/Stepper';
import Step from '@mui/material/Step';
import StepLabel from '@mui/material/StepLabel';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Alert from '@mui/material/Alert';
import Snackbar from '@mui/material/Snackbar';
import { familiesApi } from '@/api/familiesApi';
import { branchesApi } from '@/api/branchesApi';
import { useAuth } from '@/auth/useAuth';
import { useFamilyPermissions } from '@/auth/useFamilyPermissions';
import { isAlphabeticName, isValidAadhaar, isValidEmail, isValidIndianPhone, isValidPinCode } from '@/utils/validators';
import { BasicInfoStep } from './steps/BasicInfoStep';
import { AddressStep } from './steps/AddressStep';
import { CommunityStep } from './steps/CommunityStep';
import { FinancialStep } from './steps/FinancialStep';
import { emptyWizardState } from './types';
import type { WizardFormErrors, WizardFormState } from './types';
import { clearDraft, loadDraft, saveDraft } from './useFamilyRegistrationDraft';
import type { BranchSummary, CreateFamilyMemberRequest, CreateFamilyRequest, Family, Gender } from '@/types/domain';

const STEP_KEYS = ['stepBasicInfo', 'stepAddress', 'stepCommunity', 'stepFinancial', 'stepMemberRegistration'];
const LAST_FORM_STEP = 3;
// Module-level constant so the permissions hook's memo isn't invalidated on every render.
const EMPTY_FAMILIES: Family[] = [];

function validateStep(step: number, values: WizardFormState, t: (key: string) => string): WizardFormErrors {
  const errors: WizardFormErrors = {};
  if (step === 0) {
    if (!values.headFirstName.trim()) errors.headFirstName = t('common.required');
    else if (!isAlphabeticName(values.headFirstName)) errors.headFirstName = t('families.alphabetsOnly');
    if (values.headMiddleName && !isAlphabeticName(values.headMiddleName)) {
      errors.headMiddleName = t('families.alphabetsOnly');
    }
    if (!values.headLastName.trim()) errors.headLastName = t('common.required');
    else if (!isAlphabeticName(values.headLastName)) errors.headLastName = t('families.alphabetsOnly');
    if (!values.headGender) errors.headGender = t('common.required');
    if (!values.headDateOfBirth) errors.headDateOfBirth = t('common.required');
    if (!values.mobileNumber) errors.mobileNumber = t('common.required');
    else if (!isValidIndianPhone(values.mobileNumber)) errors.mobileNumber = t('families.invalidMobile');
    if (values.email && !isValidEmail(values.email)) errors.email = t('families.invalidEmail');
    if (values.aadhaarNumber && !isValidAadhaar(values.aadhaarNumber)) errors.aadhaarNumber = t('families.invalidAadhaar');
    if (values.photo) {
      if (!['image/jpeg', 'image/png'].includes(values.photo.type)) errors.photo = t('families.invalidPhotoType');
      else if (values.photo.size > 2 * 1024 * 1024) errors.photo = t('families.photoTooLarge');
    }
  } else if (step === 1) {
    if (!values.address.trim()) errors.address = t('common.required');
    if (!values.country) errors.country = t('common.required');
    if (!values.state) errors.state = t('common.required');
    if (!values.district) errors.district = t('common.required');
    if (!values.areaLocality.trim()) errors.areaLocality = t('common.required');
    if (!values.pinCode) errors.pinCode = t('common.required');
    else if (!isValidPinCode(values.pinCode)) errors.pinCode = t('families.invalidPinCode');
  } else if (step === 2) {
    if (!values.samaj) errors.samaj = t('common.required');
    if (!values.gotra) errors.gotra = t('common.required');
    if (values.gotra === 'Other' && !values.otherGotra.trim()) errors.otherGotra = t('common.required');
    if (!values.nativePlace.trim()) errors.nativePlace = t('common.required');
  }
  // Step 3 (Financial) is entirely optional per the spec - nothing to validate.
  return errors;
}

function buildCreateFamilyRequest(values: WizardFormState): CreateFamilyRequest {
  return {
    headFirstName: values.headFirstName.trim(),
    headMiddleName: values.headMiddleName.trim(),
    headLastName: values.headLastName.trim(),
    familyName: values.familyName.trim(),
    headGender: (values.headGender || null) as Gender | null,
    headDateOfBirth: values.headDateOfBirth,
    mobileNumber: values.mobileNumber,
    email: values.email,
    aadhaarNumber: values.aadhaarNumber,
    address: values.address,
    country: values.country,
    state: values.state,
    district: values.district,
    areaLocality: values.areaLocality,
    pinCode: values.pinCode,
    samaj: values.samaj || null,
    gotra: values.gotra === 'Other' ? values.otherGotra.trim() : values.gotra,
    nativePlace: values.nativePlace,
    occupationBusinessType: values.occupationBusinessType,
    annualIncomeRange: values.annualIncomeRange,
    familyCategory: values.familyCategory || null,
    ownTwoWheeler: values.ownTwoWheeler,
    ownFourWheeler: values.ownFourWheeler,
    ownHome: values.ownHome,
    ownPlot: values.ownPlot,
    willingToContribute: values.willingToContribute,
  };
}

function buildHeadOfFamilyMemberRequest(values: WizardFormState): CreateFamilyMemberRequest {
  return {
    name: [values.headFirstName, values.headMiddleName, values.headLastName].filter(Boolean).join(' ').trim(),
    relationshipToHead: 'SELF',
    dateOfBirth: values.headDateOfBirth,
    gender: values.headGender as Gender,
    maritalStatus: null,
    education: '',
    instituteName: '',
    profession: values.occupationBusinessType,
    companyName: '',
    mobileNumber: values.mobileNumber,
    email: values.email,
    bloodGroup: null,
  };
}

export default function FamilyRegistrationWizardPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { user, isProfileLoaded } = useAuth();

  const [activeStep, setActiveStep] = useState(0);
  const [values, setValues] = useState<WizardFormState>(() => loadDraft() ?? emptyWizardState());
  const [errors, setErrors] = useState<WizardFormErrors>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [draftSaved, setDraftSaved] = useState(false);
  const [myBranch, setMyBranch] = useState<BranchSummary | null>(null);
  // null = still checking. The per-user creation cap depends on how many families the user
  // already owns, which the route guard can't know (it only sees roles), so it's checked here -
  // otherwise typing /families/register directly would bypass the limit entirely.
  const [existingFamilies, setExistingFamilies] = useState<Family[] | null>(null);

  useEffect(() => {
    if (!user) return;
    branchesApi
      .list()
      .then((branches) => setMyBranch(branches.find((b) => b.id === user.chapterId) ?? null))
      .catch(() => setMyBranch(null));
  }, [user]);

  useEffect(() => {
    familiesApi
      .list()
      .then(setExistingFamilies)
      // On a read failure, fall back to an empty list rather than blocking registration: the
      // backend re-checks authorization on submit, so a false "allow" here is recoverable while
      // a false "deny" would strand a legitimate user.
      .catch(() => setExistingFamilies([]));
  }, []);

  const { createDenial } = useFamilyPermissions(existingFamilies ?? EMPTY_FAMILIES);

  const setField = <K extends keyof WizardFormState>(key: K, value: WizardFormState[K]) => {
    setValues((prev) => ({ ...prev, [key]: value }));
  };

  const handleNext = () => {
    const stepErrors = validateStep(activeStep, values, t);
    setErrors(stepErrors);
    if (Object.keys(stepErrors).length === 0) {
      setActiveStep((step) => Math.min(step + 1, LAST_FORM_STEP));
    }
  };

  const handleBack = () => {
    setErrors({});
    setActiveStep((step) => Math.max(step - 1, 0));
  };

  const handleSaveDraft = () => {
    saveDraft(values);
    setDraftSaved(true);
  };

  const handleSubmit = async () => {
    const allErrors: WizardFormErrors = {};
    let firstInvalidStep: number | null = null;
    for (let step = 0; step <= LAST_FORM_STEP; step++) {
      const stepErrors = validateStep(step, values, t);
      Object.assign(allErrors, stepErrors);
      if (firstInvalidStep === null && Object.keys(stepErrors).length > 0) firstInvalidStep = step;
    }
    if (firstInvalidStep !== null) {
      setErrors(allErrors);
      setActiveStep(firstInvalidStep);
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);
    try {
      const family = await familiesApi.register(buildCreateFamilyRequest(values));
      if (values.photo) {
        await familiesApi.uploadPhoto(family.id, values.photo);
      }
      await familiesApi.addMember(family.id, buildHeadOfFamilyMemberRequest(values));
      clearDraft();
      navigate(`/member-registration?familyId=${family.id}`, {
        state: {
          familyId: family.id,
          familyHeadName: family.headOfFamilyName,
          gotra: family.gotra,
          moolGaon: family.nativePlace,
        },
      });
    } catch (error) {
      // family-service answers 409 when the caller already owns a family (its own copy of the cap
      // this page enforces up front). Surfacing that distinctly matters: the generic "check the
      // form" message would send the user hunting for a validation error that doesn't exist.
      // Reachable despite the pre-check whenever the count changed under us - a second tab, or an
      // admin registering on the user's behalf between page load and submit.
      const isRegistrationLimit = axios.isAxiosError(error) && error.response?.status === 409;
      setSubmitError(
        isRegistrationLimit
          ? t('families.registrationLimitReached')
          : t('families.registerError'),
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const steps = [
    <BasicInfoStep key="basic" values={values} errors={errors} setField={setField} />,
    <AddressStep key="address" values={values} errors={errors} setField={setField} />,
    <CommunityStep key="community" values={values} errors={errors} setField={setField} />,
    <FinancialStep key="financial" values={values} errors={errors} setField={setField} />,
  ];

  const stepDescriptions = [
    t('families.stepBasicDesc') || 'Enter the head of family\'s personal and contact details',
    t('families.stepAddressDesc') || 'Provide the family\'s complete residential address',
    t('families.stepCommunityDesc') || 'Share your community background and cultural roots',
    t('families.stepFinancialDesc') || 'Optional financial and social participation details',
  ];

  const progressPercent = ((activeStep) / LAST_FORM_STEP) * 100;

  // Hold the wizard back until BOTH the owned-family count and the user's permissions are known.
  // Rendering before permissions arrive would briefly compute "no permission" for everyone and
  // flash the denial message at legitimate users.
  if (existingFamilies === null || !isProfileLoaded) {
    return null;
  }

  if (createDenial) {
    return (
      <Stack spacing={2} sx={{ maxWidth: 640 }}>
        <Typography variant="h5" fontWeight={600}>
          {t('families.registerTitle')}
        </Typography>
        <Alert severity="info">
          {createDenial === 'limit-reached'
            ? t('families.registrationLimitReached')
            : t('families.registrationNotPermitted')}
        </Alert>
        <Box>
          <Button variant="outlined" onClick={() => navigate('/families')}>
            {t('families.title')}
          </Button>
        </Box>
      </Stack>
    );
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', py: { xs: 2, sm: 3 } }}>
      {/* Header with gradient */}
      <Box sx={{
        bgcolor: 'primary.main',
        color: 'primary.contrastText',
        p: { xs: 2, sm: 3 },
        mb: 4,
        borderRadius: 0,
        backgroundImage: 'linear-gradient(135deg, #7C1D1D 0%, #A84444 100%)',
        boxShadow: '0 2px 8px rgba(124, 29, 29, 0.15)',
      }}>
        <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }}>
          <Box sx={{
            width: 48,
            height: 48,
            borderRadius: '50%',
            bgcolor: 'rgba(255, 255, 255, 0.2)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '24px',
            fontWeight: 700,
            fontFamily: '"Playfair Display", Georgia, serif',
          }}>
            A
          </Box>
          <Box>
            <Typography variant="h5" fontWeight={600}>
              AgrawalPulse
            </Typography>
            <Typography variant="caption" sx={{ opacity: 0.8 }}>
              {t('common.communityPortal') || 'Community Management Portal'}
            </Typography>
          </Box>
        </Stack>
      </Box>

      <Box sx={{ maxWidth: '900px', mx: 'auto', px: { xs: 2, sm: 0 } }}>
        {/* Progress bar */}
        <Box sx={{ mb: 4 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
            <Typography variant="caption" fontWeight={500} color="text.secondary">
              {t('common.step')} {activeStep + 1} {t('common.of')} {LAST_FORM_STEP + 1}
            </Typography>
            <Typography variant="caption" fontWeight={600} color="primary.main">
              {STEP_KEYS[activeStep] ? t(`families.${STEP_KEYS[activeStep]}`) : ''}
            </Typography>
          </Box>
          <Box sx={{
            height: 6,
            bgcolor: '#F0E8D8',
            borderRadius: '12px',
            overflow: 'hidden',
            mb: 2,
          }}>
            <Box sx={{
              height: '100%',
              width: `${progressPercent}%`,
              background: 'linear-gradient(90deg, #7C1D1D 0%, #C05B0B 100%)',
              transition: 'width 0.5s ease-out',
              borderRadius: '12px',
            }} />
          </Box>
        </Box>

        {/* Step indicator */}
        <Stepper activeStep={activeStep} sx={{ mb: 4, bgcolor: 'transparent' }} alternativeLabel>
          {STEP_KEYS.map((key, idx) => (
            <Step key={key} sx={{ cursor: 'default' }}>
              <StepLabel
                sx={{
                  '& .MuiStepLabel-label': {
                    fontWeight: idx === activeStep ? 600 : 500,
                    color: idx === activeStep ? 'primary.main' : idx < activeStep ? 'primary.main' : 'text.secondary',
                    mt: 1,
                  },
                }}
              >
                {t(`families.${key}`)}
              </StepLabel>
            </Step>
          ))}
        </Stepper>

        {/* Branch info */}
        {myBranch && (
          <Alert severity="info" variant="outlined" sx={{ mb: 3 }}>
            {t('families.branchNote', { branch: `${myBranch.name} (${myBranch.city}, ${myBranch.state})` })}
          </Alert>
        )}

        {/* Form card with gradient header */}
        <Paper elevation={1} sx={{
          border: '1px solid rgba(124, 29, 29, 0.15)',
          borderRadius: 2,
          overflow: 'hidden',
        }}>
          {/* Card header with gradient */}
          <Box sx={{
            p: { xs: 2, sm: 3 },
            background: 'linear-gradient(135deg, rgba(124, 29, 29, 0.05) 0%, rgba(192, 91, 11, 0.05) 100%)',
            borderBottom: '1px solid rgba(124, 29, 29, 0.15)',
          }}>
            <Typography
              variant="h6"
              sx={{
                fontFamily: '"Playfair Display", Georgia, serif',
                fontWeight: 600,
                color: 'primary.main',
                mb: 0.5,
              }}
            >
              {STEP_KEYS[activeStep] ? t(`families.${STEP_KEYS[activeStep]}`) : ''}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {stepDescriptions[activeStep]}
            </Typography>
          </Box>

          {/* Card content */}
          <Box sx={{ p: { xs: 2, sm: 3 } }}>
            <Stack spacing={3}>
              {submitError && <Alert severity="error">{submitError}</Alert>}
              {steps[activeStep]}
            </Stack>
          </Box>

          {/* Card footer with buttons */}
          <Box sx={{
            p: { xs: 2, sm: 3 },
            bgcolor: 'rgba(240, 232, 216, 0.2)',
            borderTop: '1px solid rgba(124, 29, 29, 0.15)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: 2,
          }}>
            <Button
              onClick={() => navigate('/families')}
              disabled={isSubmitting}
              sx={{
                textTransform: 'none',
                fontWeight: 500,
              }}
            >
              {t('common.cancel')}
            </Button>

            <Stack direction="row" spacing={2}>
              {activeStep > 0 && (
                <Button
                  onClick={handleBack}
                  disabled={isSubmitting}
                  variant="outlined"
                  sx={{
                    textTransform: 'none',
                    borderColor: 'primary.main',
                    color: 'primary.main',
                    fontWeight: 500,
                    '&:hover': {
                      bgcolor: 'primary.main',
                      color: 'primary.contrastText',
                    },
                  }}
                >
                  {t('common.back')}
                </Button>
              )}

              <Button
                onClick={handleSaveDraft}
                disabled={isSubmitting}
                variant="outlined"
                sx={{
                  textTransform: 'none',
                  borderColor: 'text.secondary',
                  color: 'text.secondary',
                  fontWeight: 500,
                }}
              >
                {t('families.saveDraft')}
              </Button>

              {activeStep < LAST_FORM_STEP && (
                <Button
                  variant="contained"
                  onClick={handleNext}
                  disabled={isSubmitting}
                  sx={{
                    textTransform: 'none',
                    fontWeight: 600,
                  }}
                >
                  {t('common.next')}
                </Button>
              )}

              {activeStep === LAST_FORM_STEP && (
                <Button
                  variant="contained"
                  color="secondary"
                  onClick={handleSubmit}
                  disabled={isSubmitting}
                  sx={{
                    textTransform: 'none',
                    fontWeight: 600,
                  }}
                >
                  {t('common.submit')}
                </Button>
              )}
            </Stack>
          </Box>
        </Paper>

        {/* Footer info */}
        <Box sx={{ mt: 4, textAlign: 'center' }}>
          <Typography variant="caption" color="text.secondary">
            {t('families.requiredFieldsNote') || 'Fields marked with * are mandatory. Data is encrypted and stored securely.'}
          </Typography>
        </Box>
      </Box>

      <Snackbar
        open={draftSaved}
        autoHideDuration={3000}
        onClose={() => setDraftSaved(false)}
        message={t('families.draftSaved')}
      />
    </Box>
  );
}
