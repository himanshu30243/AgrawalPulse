Here are all three files that make up the complete frontend:

src/styles/fonts.css

@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@500;600;700&family=DM+Sans:ital,opsz,wght@0,9..40,300;0,9..40,400;0,9..40,500;0,9..40,600;1,9..40,400&display=swap');
src/styles/theme.css

@custom-variant dark (&:is(.dark *));

:root {
  --font-size: 16px;
  --background: #FDF8F0;
  --foreground: #1A0800;
  --card: #FFFFFF;
  --card-foreground: #1A0800;
  --popover: #FFFFFF;
  --popover-foreground: #1A0800;
  --primary: #7C1D1D;
  --primary-foreground: #FFFFFF;
  --secondary: #F5EDE0;
  --secondary-foreground: #1A0800;
  --muted: #F0E8D8;
  --muted-foreground: #7A5A44;
  --accent: #C05B0B;
  --accent-foreground: #FFFFFF;
  --destructive: #C0392B;
  --destructive-foreground: #FFFFFF;
  --border: rgba(124, 29, 29, 0.15);
  --input: transparent;
  --input-background: #FFFFFF;
  --switch-background: #C4A882;
  --font-weight-medium: 500;
  --font-weight-normal: 400;
  --ring: #7C1D1D;
  --chart-1: #7C1D1D;
  --chart-2: #C05B0B;
  --chart-3: #B8860B;
  --chart-4: #5D4037;
  --chart-5: #4E342E;
  --radius: 0.75rem;
  --sidebar: #FFFFFF;
  --sidebar-foreground: #1A0800;
  --sidebar-primary: #7C1D1D;
  --sidebar-primary-foreground: #FFFFFF;
  --sidebar-accent: #F5EDE0;
  --sidebar-accent-foreground: #1A0800;
  --sidebar-border: rgba(124, 29, 29, 0.15);
  --sidebar-ring: #7C1D1D;
}

.dark {
  --background: oklch(0.145 0 0);
  --foreground: oklch(0.985 0 0);
  --card: oklch(0.145 0 0);
  --card-foreground: oklch(0.985 0 0);
  --popover: oklch(0.145 0 0);
  --popover-foreground: oklch(0.985 0 0);
  --primary: oklch(0.985 0 0);
  --primary-foreground: oklch(0.205 0 0);
  --secondary: oklch(0.269 0 0);
  --secondary-foreground: oklch(0.985 0 0);
  --muted: oklch(0.269 0 0);
  --muted-foreground: oklch(0.708 0 0);
  --accent: oklch(0.269 0 0);
  --accent-foreground: oklch(0.985 0 0);
  --destructive: oklch(0.396 0.141 25.723);
  --destructive-foreground: oklch(0.637 0.237 25.331);
  --border: oklch(0.269 0 0);
  --input: oklch(0.269 0 0);
  --ring: oklch(0.439 0 0);
  --font-weight-medium: 500;
  --font-weight-normal: 400;
  --chart-1: oklch(0.488 0.243 264.376);
  --chart-2: oklch(0.696 0.17 162.48);
  --chart-3: oklch(0.769 0.188 70.08);
  --chart-4: oklch(0.627 0.265 303.9);
  --chart-5: oklch(0.645 0.246 16.439);
  --sidebar: oklch(0.205 0 0);
  --sidebar-foreground: oklch(0.985 0 0);
  --sidebar-primary: oklch(0.488 0.243 264.376);
  --sidebar-primary-foreground: oklch(0.985 0 0);
  --sidebar-accent: oklch(0.269 0 0);
  --sidebar-accent-foreground: oklch(0.985 0 0);
  --sidebar-border: oklch(0.269 0 0);
  --sidebar-ring: oklch(0.439 0 0);
}

@theme inline {
  --font-sans: 'DM Sans', system-ui, sans-serif;
  --font-display: 'Playfair Display', Georgia, serif;
  --color-background: var(--background);
  --color-foreground: var(--foreground);
  --color-card: var(--card);
  --color-card-foreground: var(--card-foreground);
  --color-popover: var(--popover);
  --color-popover-foreground: var(--popover-foreground);
  --color-primary: var(--primary);
  --color-primary-foreground: var(--primary-foreground);
  --color-secondary: var(--secondary);
  --color-secondary-foreground: var(--secondary-foreground);
  --color-muted: var(--muted);
  --color-muted-foreground: var(--muted-foreground);
  --color-accent: var(--accent);
  --color-accent-foreground: var(--accent-foreground);
  --color-destructive: var(--destructive);
  --color-destructive-foreground: var(--destructive-foreground);
  --color-border: var(--border);
  --color-input: var(--input);
  --color-input-background: var(--input-background);
  --color-switch-background: var(--switch-background);
  --color-ring: var(--ring);
  --color-chart-1: var(--chart-1);
  --color-chart-2: var(--chart-2);
  --color-chart-3: var(--chart-3);
  --color-chart-4: var(--chart-4);
  --color-chart-5: var(--chart-5);
  --radius-sm: calc(var(--radius) - 4px);
  --radius-md: calc(var(--radius) - 2px);
  --radius-lg: var(--radius);
  --radius-xl: calc(var(--radius) + 4px);
  --color-sidebar: var(--sidebar);
  --color-sidebar-foreground: var(--sidebar-foreground);
  --color-sidebar-primary: var(--sidebar-primary);
  --color-sidebar-primary-foreground: var(--sidebar-primary-foreground);
  --color-sidebar-accent: var(--sidebar-accent);
  --color-sidebar-accent-foreground: var(--sidebar-accent-foreground);
  --color-sidebar-border: var(--sidebar-border);
  --color-sidebar-ring: var(--sidebar-ring);
}

@layer base {
  * {
    @apply border-border outline-ring/50;
  }

  body {
    @apply bg-background text-foreground;
  }

  html {
    font-size: var(--font-size);
  }

  h1 {
    font-size: var(--text-2xl);
    font-weight: var(--font-weight-medium);
    line-height: 1.5;
  }

  h2 {
    font-size: var(--text-xl);
    font-weight: var(--font-weight-medium);
    line-height: 1.5;
  }

  h3 {
    font-size: var(--text-lg);
    font-weight: var(--font-weight-medium);
    line-height: 1.5;
  }

  h4 {
    font-size: var(--text-base);
    font-weight: var(--font-weight-medium);
    line-height: 1.5;
  }

  label {
    font-size: var(--text-base);
    font-weight: var(--font-weight-medium);
    line-height: 1.5;
  }

  button {
    font-size: var(--text-base);
    font-weight: var(--font-weight-medium);
    line-height: 1.5;
  }

  input {
    font-size: var(--text-base);
    font-weight: var(--font-weight-normal);
    line-height: 1.5;
  }
}
src/app/App.tsx

import { useState, useRef, useCallback } from "react";
import {
  User, MapPin, Users, Briefcase, CheckCircle2,
  ChevronRight, ChevronLeft, Save, Upload, X,
  AlertCircle, Check, Flame
} from "lucide-react";

// ─── Constants ───────────────────────────────────────────────────────────────

const STEPS = [
  { id: 1, label: "Basic Information",     short: "Basic Info",  icon: User },
  { id: 2, label: "Address Information",   short: "Address",     icon: MapPin },
  { id: 3, label: "Community Information", short: "Community",   icon: Users },
  { id: 4, label: "Financial Details",     short: "Financial",   icon: Briefcase },
  { id: 5, label: "Member Registration",   short: "Members",     icon: CheckCircle2, external: true },
];

const COUNTRIES = ["India", "United States", "United Kingdom", "Canada", "Australia", "UAE", "Singapore"];

const STATES: Record<string, string[]> = {
  India: ["Delhi", "Gujarat", "Haryana", "Karnataka", "Madhya Pradesh", "Maharashtra", "Punjab", "Rajasthan", "Tamil Nadu", "Uttar Pradesh"],
  "United States": ["California", "Florida", "New York", "Texas"],
  "United Kingdom": ["England", "Scotland", "Wales"],
  Canada: ["British Columbia", "Ontario", "Quebec"],
  Australia: ["New South Wales", "Queensland", "Victoria"],
  UAE: ["Abu Dhabi", "Dubai", "Sharjah"],
  Singapore: ["Central Region", "West Region"],
};

const DISTRICTS: Record<string, string[]> = {
  Rajasthan: ["Ajmer", "Bikaner", "Jaipur", "Jodhpur", "Kota", "Sikar", "Udaipur"],
  "Uttar Pradesh": ["Agra", "Kanpur", "Lucknow", "Mathura", "Prayagraj", "Varanasi"],
  "Madhya Pradesh": ["Bhopal", "Gwalior", "Indore", "Jabalpur", "Ujjain"],
  Gujarat: ["Ahmedabad", "Gandhinagar", "Rajkot", "Surat", "Vadodara"],
  Maharashtra: ["Aurangabad", "Mumbai", "Nagpur", "Nashik", "Pune"],
  Delhi: ["Central Delhi", "East Delhi", "New Delhi", "North Delhi", "South Delhi", "West Delhi"],
  Haryana: ["Faridabad", "Gurugram", "Hisar", "Karnal", "Rohtak"],
  Punjab: ["Amritsar", "Chandigarh", "Jalandhar", "Ludhiana", "Patiala"],
  Karnataka: ["Bengaluru", "Hubli", "Mangaluru", "Mysuru"],
  "Tamil Nadu": ["Chennai", "Coimbatore", "Madurai", "Tiruchirappalli"],
};

const GOTRAS = [
  "Airan", "Bansal", "Bhandal", "Bindal", "Dharan", "Garg", "Goenka",
  "Goyal", "Jindal", "Kansal", "Kuchhal", "Madhukul", "Mangal", "Mittal",
  "Nangal", "Singhal", "Tayal", "Tingle", "Other",
];

// ─── Types ────────────────────────────────────────────────────────────────────

interface FormData {
  firstName: string; middleName: string; lastName: string;
  gender: string; dateOfBirth: string; mobileNumber: string;
  email: string; aadhaarNumber: string; profilePhoto: File | null;
  address: string; country: string; state: string; district: string;
  area: string; pinCode: string;
  samaj: string; gotra: string; otherGotra: string; nativePlace: string; region: string;
  occupation: string; annualIncome: string; familyCategory: string; willingToContribute: string;
}

type Errors = Partial<Record<keyof FormData, string>>;

const blank: FormData = {
  firstName: "", middleName: "", lastName: "",
  gender: "", dateOfBirth: "", mobileNumber: "",
  email: "", aadhaarNumber: "", profilePhoto: null,
  address: "", country: "India", state: "", district: "", area: "", pinCode: "",
  samaj: "", gotra: "", otherGotra: "", nativePlace: "", region: "",
  occupation: "", annualIncome: "", familyCategory: "", willingToContribute: "",
};

function validate(step: number, d: FormData): Errors {
  const e: Errors = {};
  const today = new Date().toISOString().split("T")[0];
  const alpha = /^[a-zA-Z\s]+$/;

  if (step === 1) {
    if (!d.firstName.trim()) e.firstName = "First name is required";
    else if (!alpha.test(d.firstName)) e.firstName = "Alphabets only";
    if (!d.lastName.trim()) e.lastName = "Last name is required";
    else if (!alpha.test(d.lastName)) e.lastName = "Alphabets only";
    if (d.middleName && !alpha.test(d.middleName)) e.middleName = "Alphabets only";
    if (!d.gender) e.gender = "Gender is required";
    if (!d.dateOfBirth) e.dateOfBirth = "Date of birth is required";
    else if (d.dateOfBirth >= today) e.dateOfBirth = "Cannot be a future date";
    if (!d.mobileNumber) e.mobileNumber = "Mobile number is required";
    else if (!/^\d{10}$/.test(d.mobileNumber)) e.mobileNumber = "Must be exactly 10 digits";
    if (d.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(d.email)) e.email = "Invalid email format";
    if (d.aadhaarNumber && !/^\d{12}$/.test(d.aadhaarNumber)) e.aadhaarNumber = "Must be exactly 12 digits";
  }
  if (step === 2) {
    if (!d.address.trim()) e.address = "Address is required";
    if (!d.country) e.country = "Country is required";
    if (!d.state) e.state = "State is required";
    if (!d.district) e.district = "District is required";
    if (!d.area.trim()) e.area = "Area / Locality is required";
    if (!d.pinCode) e.pinCode = "PIN Code is required";
    else if (!/^\d{6}$/.test(d.pinCode)) e.pinCode = "Must be exactly 6 digits";
  }
  if (step === 3) {
    if (!d.samaj) e.samaj = "Samaj is required";
    if (!d.gotra) e.gotra = "Gotra is required";
    if (d.gotra === "Other" && !d.otherGotra.trim()) e.otherGotra = "Please specify your Gotra";
    if (!d.nativePlace.trim()) e.nativePlace = "Native place is required";
  }
  return e;
}

function genFamilyId() {
  const yr = new Date().getFullYear();
  const n = Math.floor(10000 + Math.random() * 90000);
  return `AGR-${yr}-${n}`;
}

// ─── Primitive components ─────────────────────────────────────────────────────

function Label({ text, required }: { text: string; required?: boolean }) {
  return (
    <label className="block text-xs font-semibold text-foreground/60 uppercase tracking-wide mb-1.5">
      {text}{required && <span className="text-destructive ml-0.5 normal-case tracking-normal text-sm">*</span>}
    </label>
  );
}

function Err({ msg }: { msg?: string }) {
  if (!msg) return null;
  return (
    <p className="mt-1.5 text-xs text-destructive flex items-center gap-1">
      <AlertCircle className="w-3 h-3 flex-shrink-0" />{msg}
    </p>
  );
}

function inputCls(err?: string) {
  return `w-full px-3.5 py-2.5 text-sm bg-white border rounded-xl outline-none transition-all placeholder:text-muted-foreground
    ${err
      ? "border-destructive focus:ring-2 focus:ring-destructive/20"
      : "border-border focus:border-primary focus:ring-2 focus:ring-primary/10"}`;
}

function FInput({
  value, onChange, placeholder, type = "text", err, maxLength, readOnly = false,
}: {
  value: string; onChange?: (v: string) => void; placeholder?: string;
  type?: string; err?: string; maxLength?: number; readOnly?: boolean;
}) {
  return (
    <input
      type={type} value={value} placeholder={placeholder} maxLength={maxLength}
      readOnly={readOnly}
      onChange={onChange ? (e) => onChange(e.target.value) : undefined}
      className={`${inputCls(err)} ${readOnly ? "bg-muted cursor-not-allowed opacity-60" : ""}`}
    />
  );
}

function FSelect({
  value, onChange, options, placeholder, err,
}: {
  value: string; onChange: (v: string) => void;
  options: string[]; placeholder?: string; err?: string;
}) {
  return (
    <div className="relative">
      <select
        value={value} onChange={(e) => onChange(e.target.value)}
        className={`${inputCls(err)} appearance-none cursor-pointer pr-8 ${!value ? "text-muted-foreground" : "text-foreground"}`}
      >
        <option value="" disabled>{placeholder ?? "Select…"}</option>
        {options.map((o) => <option key={o} value={o}>{o}</option>)}
      </select>
      <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground">
        <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
          <path d="M2 4l4 4 4-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      </span>
    </div>
  );
}

function FTextarea({ value, onChange, placeholder, err }: {
  value: string; onChange: (v: string) => void; placeholder?: string; err?: string;
}) {
  return (
    <textarea
      value={value} onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder} rows={3}
      className={`${inputCls(err)} resize-none`}
    />
  );
}

// ─── Step 1: Basic Information ────────────────────────────────────────────────

function Step1({
  d, update, errors, photoPreview, onPhoto, onRemovePhoto, fileRef,
}: {
  d: FormData; update: (k: keyof FormData, v: string | File | null) => void;
  errors: Errors; photoPreview: string | null;
  onPhoto: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onRemovePhoto: () => void; fileRef: React.RefObject<HTMLInputElement>;
}) {
  return (
    <div className="space-y-7">
      <div>
        <p className="text-xs font-semibold text-primary/70 uppercase tracking-widest mb-4 pb-2 border-b border-border">
          Head of Family Details
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <Label text="First Name" required />
            <FInput value={d.firstName} onChange={(v) => update("firstName", v)} placeholder="Rajesh" err={errors.firstName} />
            <Err msg={errors.firstName} />
          </div>
          <div>
            <Label text="Middle Name" />
            <FInput value={d.middleName} onChange={(v) => update("middleName", v)} placeholder="Kumar" err={errors.middleName} />
            <Err msg={errors.middleName} />
          </div>
          <div>
            <Label text="Last Name" required />
            <FInput value={d.lastName} onChange={(v) => update("lastName", v)} placeholder="Agrawal" err={errors.lastName} />
            <Err msg={errors.lastName} />
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <Label text="Gender" required />
          <FSelect value={d.gender} onChange={(v) => update("gender", v)} options={["Male", "Female", "Other"]} placeholder="Select gender" err={errors.gender} />
          <Err msg={errors.gender} />
        </div>
        <div>
          <Label text="Date of Birth" required />
          <FInput type="date" value={d.dateOfBirth} onChange={(v) => update("dateOfBirth", v)} err={errors.dateOfBirth} />
          <Err msg={errors.dateOfBirth} />
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <Label text="Mobile Number" required />
          <div className="flex gap-2">
            <span className="px-3 py-2.5 bg-muted border border-border rounded-xl text-sm text-muted-foreground flex-shrink-0 font-medium">
              +91
            </span>
            <div className="flex-1">
              <FInput
                value={d.mobileNumber}
                onChange={(v) => update("mobileNumber", v.replace(/\D/g, "").slice(0, 10))}
                placeholder="9876543210" maxLength={10} err={errors.mobileNumber}
              />
            </div>
          </div>
          <Err msg={errors.mobileNumber} />
        </div>
        <div>
          <Label text="Email Address" />
          <FInput type="email" value={d.email} onChange={(v) => update("email", v)} placeholder="rajesh.agrawal@gmail.com" err={errors.email} />
          <Err msg={errors.email} />
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <Label text="Aadhaar Number" />
          <FInput
            value={d.aadhaarNumber}
            onChange={(v) => update("aadhaarNumber", v.replace(/\D/g, "").slice(0, 12))}
            placeholder="xxxx xxxx xxxx" maxLength={12} err={errors.aadhaarNumber}
          />
          <p className="mt-1 text-xs text-muted-foreground">12-digit Aadhaar number (optional)</p>
          <Err msg={errors.aadhaarNumber} />
        </div>
        <div>
          <Label text="Profile Photo" />
          <input ref={fileRef} type="file" accept=".jpg,.jpeg,.png" onChange={onPhoto} className="hidden" />
          {photoPreview ? (
            <div className="flex items-center gap-3 px-3.5 py-2.5 bg-white border border-border rounded-xl">
              <img src={photoPreview} alt="Profile preview" className="w-10 h-10 rounded-full object-cover border-2 border-primary/30" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-foreground truncate">{d.profilePhoto?.name ?? "Photo uploaded"}</p>
                <p className="text-xs text-muted-foreground">
                  {d.profilePhoto ? `${(d.profilePhoto.size / 1024).toFixed(0)} KB` : ""}
                </p>
              </div>
              <button onClick={onRemovePhoto} className="text-muted-foreground hover:text-destructive transition-colors">
                <X className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <button
              onClick={() => fileRef.current?.click()}
              className={`w-full px-3.5 py-2.5 text-sm bg-white border-2 border-dashed rounded-xl flex items-center gap-2 text-muted-foreground hover:border-primary hover:text-primary transition-colors ${errors.profilePhoto ? "border-destructive" : "border-border"}`}
            >
              <Upload className="w-4 h-4 flex-shrink-0" />
              <span>Upload photo (JPG, PNG · max 2 MB)</span>
            </button>
          )}
          <Err msg={errors.profilePhoto} />
        </div>
      </div>
    </div>
  );
}

// ─── Step 2: Address Information ──────────────────────────────────────────────

function Step2({
  d, update, updateCountry, updateState, updateDistrict, errors,
}: {
  d: FormData;
  update: (k: keyof FormData, v: string) => void;
  updateCountry: (v: string) => void;
  updateState: (v: string) => void;
  updateDistrict: (v: string) => void;
  errors: Errors;
}) {
  const states = STATES[d.country] ?? [];
  const districts = DISTRICTS[d.state] ?? [];

  return (
    <div className="space-y-7">
      <div>
        <p className="text-xs font-semibold text-primary/70 uppercase tracking-widest mb-4 pb-2 border-b border-border">
          Residential Address
        </p>
        <Label text="Address" required />
        <FTextarea
          value={d.address} onChange={(v) => update("address", v)}
          placeholder="House No., Street Name, Colony / Society…" err={errors.address}
        />
        <Err msg={errors.address} />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <Label text="Country" required />
          <FSelect value={d.country} onChange={updateCountry} options={COUNTRIES} placeholder="Select country" err={errors.country} />
          <Err msg={errors.country} />
        </div>
        <div>
          <Label text="State" required />
          <FSelect
            value={d.state} onChange={updateState} options={states}
            placeholder={d.country ? "Select state" : "Select country first"} err={errors.state}
          />
          <Err msg={errors.state} />
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <Label text="District" required />
          <FSelect
            value={d.district} onChange={updateDistrict} options={districts}
            placeholder={d.state ? "Select district" : "Select state first"} err={errors.district}
          />
          <Err msg={errors.district} />
        </div>
        <div>
          <Label text="Area / Locality" required />
          <FInput value={d.area} onChange={(v) => update("area", v)} placeholder="e.g., Vaishali Nagar, Sector 12" err={errors.area} />
          <Err msg={errors.area} />
        </div>
      </div>

      <div className="max-w-xs">
        <Label text="PIN Code" required />
        <FInput
          value={d.pinCode}
          onChange={(v) => update("pinCode", v.replace(/\D/g, "").slice(0, 6))}
          placeholder="302001" maxLength={6} err={errors.pinCode}
        />
        <Err msg={errors.pinCode} />
      </div>
    </div>
  );
}

// ─── Step 3: Community Information ───────────────────────────────────────────

function Step3({
  d, update, errors,
}: {
  d: FormData; update: (k: keyof FormData, v: string) => void; errors: Errors;
}) {
  return (
    <div className="space-y-7">
      <div>
        <p className="text-xs font-semibold text-primary/70 uppercase tracking-widest mb-4 pb-2 border-b border-border">
          Community & Cultural Background
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <Label text="Samaj" required />
            <FSelect value={d.samaj} onChange={(v) => update("samaj", v)} options={["Agrawal", "Other"]} placeholder="Select Samaj" err={errors.samaj} />
            <Err msg={errors.samaj} />
          </div>
          <div>
            <Label text="Gotra" required />
            <FSelect value={d.gotra} onChange={(v) => update("gotra", v)} options={GOTRAS} placeholder="Select Gotra" err={errors.gotra} />
            <Err msg={errors.gotra} />
          </div>
        </div>
      </div>

      {d.gotra === "Other" && (
        <div className="max-w-sm">
          <Label text="Specify Other Gotra" required />
          <FInput value={d.otherGotra} onChange={(v) => update("otherGotra", v)} placeholder="Enter your Gotra" err={errors.otherGotra} />
          <Err msg={errors.otherGotra} />
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <Label text="Native Place (Mool Gaon)" required />
          <FInput value={d.nativePlace} onChange={(v) => update("nativePlace", v)} placeholder="e.g., Agroha, Haryana" err={errors.nativePlace} />
          <Err msg={errors.nativePlace} />
        </div>
        <div>
          <Label text="Region / City" />
          <FInput value={d.region || d.district || ""} readOnly placeholder="Auto-populated from district" />
          <p className="mt-1.5 text-xs text-muted-foreground">Populated automatically from your selected district</p>
        </div>
      </div>
    </div>
  );
}

// ─── Step 4: Financial Details ────────────────────────────────────────────────

function Step4({
  d, update,
}: {
  d: FormData; update: (k: keyof FormData, v: string) => void;
}) {
  return (
    <div className="space-y-7">
      <div className="flex items-start gap-3 bg-accent/10 border border-accent/20 rounded-xl px-4 py-3">
        <Flame className="w-4 h-4 text-accent mt-0.5 flex-shrink-0" />
        <p className="text-sm text-foreground/70">
          This section is <strong>optional</strong>. These details help us serve the community better — share only what you are comfortable with.
        </p>
      </div>

      <div>
        <p className="text-xs font-semibold text-primary/70 uppercase tracking-widest mb-4 pb-2 border-b border-border">
          Financial & Social Information
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <Label text="Occupation / Business Type" />
            <FInput value={d.occupation} onChange={(v) => update("occupation", v)} placeholder="e.g., Business Owner, Doctor, CA" />
            <p className="mt-1.5 text-xs text-muted-foreground">Describe your primary occupation or business</p>
          </div>
          <div>
            <Label text="Annual Income Range" />
            <FSelect
              value={d.annualIncome} onChange={(v) => update("annualIncome", v)}
              options={["Below ₹5 Lakh", "₹5–10 Lakh", "₹10–25 Lakh", "₹25–50 Lakh", "₹50 Lakh–1 Crore", "Above ₹1 Crore"]}
              placeholder="Select range"
            />
          </div>
        </div>
      </div>

      <div className="max-w-sm">
        <Label text="Family Category" />
        <FSelect
          value={d.familyCategory} onChange={(v) => update("familyCategory", v)}
          options={["Business", "Salaried", "Professional", "Retired", "Agriculture", "Other"]}
          placeholder="Select category"
        />
      </div>

      <div>
        <Label text="Willing to Contribute to Community Activities?" />
        <div className="flex gap-6 mt-2">
          {["Yes", "No"].map((opt) => (
            <label key={opt} className="flex items-center gap-2.5 cursor-pointer group">
              <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-all
                ${d.willingToContribute === opt
                  ? "border-primary bg-primary"
                  : "border-border group-hover:border-primary/50"}`}>
                {d.willingToContribute === opt && <div className="w-2 h-2 rounded-full bg-white" />}
              </div>
              <input
                type="radio" name="willingToContribute" value={opt}
                checked={d.willingToContribute === opt}
                onChange={() => update("willingToContribute", opt)}
                className="sr-only"
              />
              <span className="text-sm font-medium">{opt}</span>
            </label>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── Success Modal ────────────────────────────────────────────────────────────

function SuccessModal({ familyId, name, onClose }: { familyId: string; name: string; onClose: () => void }) {
  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 px-4">
      <div className="bg-white rounded-3xl shadow-2xl max-w-md w-full overflow-hidden">
        <div className="bg-primary px-8 py-6 text-center">
          <div className="w-16 h-16 rounded-full bg-white/20 flex items-center justify-center mx-auto mb-3">
            <Check className="w-8 h-8 text-white" strokeWidth={2.5} />
          </div>
          <h2 className="font-display text-2xl font-semibold text-white">Registration Successful!</h2>
          <p className="text-primary-foreground/70 text-sm mt-1">Family record created in AgrawalPulse</p>
        </div>

        <div className="px-8 py-6">
          <p className="text-sm text-muted-foreground text-center mb-5">
            Welcome, <strong className="text-foreground">{name}</strong>! Your family has been successfully registered.
          </p>

          <div className="bg-primary/5 border border-primary/15 rounded-2xl px-6 py-4 text-center mb-5">
            <p className="text-xs font-semibold text-primary/60 uppercase tracking-widest mb-1">Generated Family ID</p>
            <p className="font-mono text-2xl font-bold text-primary tracking-wider">{familyId}</p>
            <p className="text-xs text-muted-foreground mt-2">
              Registration Date: {new Date().toLocaleDateString("en-IN", { day: "2-digit", month: "long", year: "numeric" })}
            </p>
            <div className="flex justify-center gap-4 mt-3 text-xs text-muted-foreground">
              <span>Status: <strong className="text-green-600">Active</strong></span>
              <span>·</span>
              <span>Members: <strong className="text-foreground">1</strong></span>
            </div>
          </div>

          <div className="text-center mb-6">
            <div className="flex items-center justify-center gap-1.5 text-xs text-muted-foreground">
              <div className="w-1.5 h-1.5 rounded-full bg-accent animate-pulse" />
              Redirecting to Member Registration…
            </div>
          </div>

          <button
            onClick={onClose}
            className="w-full py-3 bg-accent text-white rounded-2xl font-semibold text-sm hover:bg-accent/90 transition-colors"
          >
            Proceed to Member Registration →
          </button>
          <p className="text-center text-xs text-muted-foreground mt-3">
            You will be taken to{" "}
            <code className="bg-muted px-1 rounded">/member-registration?familyId={familyId}</code>
          </p>
        </div>
      </div>
    </div>
  );
}

// ─── Main App ─────────────────────────────────────────────────────────────────

export default function App() {
  const [step, setStep] = useState(1);
  const [form, setForm] = useState<FormData>(blank);
  const [errors, setErrors] = useState<Errors>({});
  const [photoPreview, setPhotoPreview] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);
  const [familyId, setFamilyId] = useState("");
  const [draftSaved, setDraftSaved] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const update = useCallback((k: keyof FormData, v: string | File | null) => {
    setForm((p) => ({ ...p, [k]: v }));
    setErrors((p) => ({ ...p, [k]: undefined }));
  }, []);

  const updateCountry = (v: string) =>
    setForm((p) => ({ ...p, country: v, state: "", district: "", region: "" }));

  const updateState = (v: string) =>
    setForm((p) => ({ ...p, state: v, district: "", region: "" }));

  const updateDistrict = (v: string) =>
    setForm((p) => ({ ...p, district: v, region: v }));

  const handlePhoto = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      setErrors((p) => ({ ...p, profilePhoto: "File must be under 2 MB" }));
      return;
    }
    if (!["image/jpeg", "image/jpg", "image/png"].includes(file.type)) {
      setErrors((p) => ({ ...p, profilePhoto: "Only JPG, JPEG, PNG allowed" }));
      return;
    }
    setErrors((p) => ({ ...p, profilePhoto: undefined }));
    update("profilePhoto", file);
    const reader = new FileReader();
    reader.onload = (ev) => setPhotoPreview(ev.target?.result as string);
    reader.readAsDataURL(file);
  };

  const removePhoto = () => {
    setPhotoPreview(null);
    update("profilePhoto", null);
    if (fileRef.current) fileRef.current.value = "";
  };

  const goNext = () => {
    const e = validate(step, form);
    if (Object.keys(e).length) { setErrors(e); return; }
    setErrors({});
    setStep((s) => Math.min(s + 1, 4));
  };

  const goPrev = () => {
    setErrors({});
    setStep((s) => Math.max(s - 1, 1));
  };

  const saveDraft = () => {
    setDraftSaved(true);
    setTimeout(() => setDraftSaved(false), 2500);
  };

  const handleSubmit = () => {
    const e = validate(4, form);
    if (Object.keys(e).length) { setErrors(e); return; }
    const id = genFamilyId();
    setFamilyId(id);
    setSubmitted(true);
  };

  const progressPct = ((step - 1) / 4) * 100;
  const headName = [form.firstName, form.lastName].filter(Boolean).join(" ") || "Family Head";

  return (
    <div className="min-h-screen bg-background font-sans">

      {/* ── Header ── */}
      <header className="bg-primary text-primary-foreground sticky top-0 z-30 shadow-sm">
        <div className="max-w-4xl mx-auto px-4 sm:px-8 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-accent flex items-center justify-center flex-shrink-0">
              <span className="font-display font-bold text-white text-lg leading-none">A</span>
            </div>
            <div>
              <h1 className="font-display text-lg font-semibold leading-tight">AgrawalPulse</h1>
              <p className="text-xs text-primary-foreground/60 leading-tight">Community Management Portal</p>
            </div>
          </div>
          <div className="hidden sm:flex items-center gap-2 text-xs text-primary-foreground/60">
            <span className="w-2 h-2 rounded-full bg-green-400 inline-block" />
            Family Registration
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 sm:px-8 py-8 pb-16">

        {/* ── Progress ── */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-medium text-muted-foreground">Step {step} of 4</span>
            <span className="text-xs font-medium text-primary">{STEPS[step - 1].label}</span>
          </div>
          <div className="h-1.5 bg-muted rounded-full overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-primary to-accent rounded-full transition-all duration-500 ease-out"
              style={{ width: `${progressPct}%` }}
            />
          </div>

          <div className="flex items-start justify-between mt-4">
            {STEPS.map((s) => {
              const Icon = s.icon;
              const done = step > s.id;
              const active = step === s.id;
              const ext = s.id === 5;
              return (
                <div key={s.id} className="flex flex-col items-center gap-1.5 flex-1">
                  <div className={`w-9 h-9 rounded-full flex items-center justify-center border-2 transition-all duration-300
                    ${done
                      ? "bg-primary border-primary text-white shadow-sm"
                      : active
                        ? "bg-white border-primary text-primary shadow-md ring-4 ring-primary/10"
                        : ext
                          ? "bg-muted border-dashed border-muted-foreground/40 text-muted-foreground/50"
                          : "bg-white border-border text-muted-foreground"}`}
                  >
                    {done ? <Check className="w-4 h-4" strokeWidth={2.5} /> : <Icon className="w-3.5 h-3.5" />}
                  </div>
                  <span className={`hidden sm:block text-xs text-center leading-tight font-medium max-w-[72px]
                    ${active ? "text-primary" : done ? "text-primary/60" : "text-muted-foreground/60"}`}>
                    {s.label}
                  </span>
                  <span className={`sm:hidden text-[10px] text-center font-medium
                    ${active ? "text-primary" : "text-muted-foreground/60"}`}>
                    {s.short}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* ── Form card ── */}
        <div className="bg-card rounded-2xl border border-border shadow-sm overflow-hidden">
          <div className="px-6 sm:px-8 py-5 bg-gradient-to-br from-primary/5 via-transparent to-accent/5 border-b border-border">
            <h2 className="font-display text-xl font-semibold text-primary">{STEPS[step - 1].label}</h2>
            <p className="text-sm text-muted-foreground mt-0.5">
              {step === 1 && "Enter the head of family's personal and contact details"}
              {step === 2 && "Provide the family's complete residential address"}
              {step === 3 && "Share your community background and cultural roots"}
              {step === 4 && "Optional financial and social participation details"}
            </p>
          </div>

          <div className="px-6 sm:px-8 py-7">
            {step === 1 && (
              <Step1
                d={form} update={update} errors={errors}
                photoPreview={photoPreview} onPhoto={handlePhoto}
                onRemovePhoto={removePhoto} fileRef={fileRef}
              />
            )}
            {step === 2 && (
              <Step2
                d={form} update={(k, v) => update(k, v)}
                updateCountry={updateCountry}
                updateState={updateState}
                updateDistrict={updateDistrict}
                errors={errors}
              />
            )}
            {step === 3 && (
              <Step3 d={form} update={(k, v) => update(k, v)} errors={errors} />
            )}
            {step === 4 && (
              <Step4 d={form} update={(k, v) => update(k, v)} />
            )}
          </div>

          {/* ── Bottom navigation ── */}
          <div className="px-6 sm:px-8 py-4 border-t border-border bg-muted/20 flex items-center justify-between gap-3 flex-wrap">
            <div className="flex items-center gap-2">
              {step > 1 && (
                <button
                  onClick={goPrev}
                  className="flex items-center gap-1.5 px-4 py-2.5 text-sm font-medium border border-border rounded-xl hover:bg-white transition-colors text-foreground/80"
                >
                  <ChevronLeft className="w-4 h-4" />Previous
                </button>
              )}
              <button
                onClick={saveDraft}
                className="flex items-center gap-1.5 px-4 py-2.5 text-sm font-medium border border-border rounded-xl hover:bg-white transition-colors text-muted-foreground"
              >
                <Save className="w-4 h-4" />
                {draftSaved
                  ? <span className="text-green-600 font-semibold">Saved!</span>
                  : "Save Draft"}
              </button>
            </div>

            <div>
              {step < 4 ? (
                <button
                  onClick={goNext}
                  className="flex items-center gap-1.5 px-6 py-2.5 text-sm font-semibold bg-primary text-primary-foreground rounded-xl hover:bg-primary/90 transition-colors shadow-sm"
                >
                  Next Step<ChevronRight className="w-4 h-4" />
                </button>
              ) : (
                <button
                  onClick={handleSubmit}
                  className="flex items-center gap-1.5 px-6 py-2.5 text-sm font-semibold bg-accent text-white rounded-xl hover:bg-accent/90 transition-colors shadow-sm"
                >
                  <CheckCircle2 className="w-4 h-4" />Submit Registration
                </button>
              )}
            </div>
          </div>
        </div>

        {/* ── System-generated field info ── */}
        <div className="mt-5 grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            { label: "Family ID",          value: "Auto-generated" },
            { label: "Registration Date",  value: new Date().toLocaleDateString("en-IN") },
            { label: "Status",             value: "Active" },
            { label: "Total Members",      value: "1 (default)" },
          ].map((item) => (
            <div key={item.label} className="bg-muted/60 rounded-xl px-4 py-3 text-center border border-border/50">
              <p className="text-xs text-muted-foreground mb-0.5">{item.label}</p>
              <p className="text-sm font-semibold text-foreground/70">{item.value}</p>
            </div>
          ))}
        </div>

        <p className="text-center text-xs text-muted-foreground mt-4">
          Fields marked <span className="text-destructive font-bold">*</span> are mandatory.
          Data is encrypted and stored securely.
        </p>
      </main>

      {/* ── Success Modal ── */}
      {submitted && (
        <SuccessModal
          familyId={familyId}
          name={headName}
          onClose={() => setSubmitted(false)}
        />
      )}
    </div>
  );
}