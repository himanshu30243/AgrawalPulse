Login Page:-


# Responsive Login Page Specification

## Objective

Create a modern, professional, responsive login page.

Users should be able to login using:

- Email Address OR Mobile Number
- Password

The page should have:

- No left-side branding section
- No company logo required
- Login card centered horizontally and vertically
- Mobile-first responsive design
- Modern enterprise-grade UI

---

# Layout

## Page Structure

The login form must appear in the center of the screen.

### Background

Properties:

- Background Color: #F0F2F5
- Height: 100vh
- Width: 100%
- Use Flexbox for centering
- Horizontally centered
- Vertically centered

---

# Login Card

Create a clean card in the center.

### Card Properties

- Background: White
- Width: 420px
- Max Width: 95%
- Padding: 32px
- Border Radius: 12px
- Box Shadow:
  0px 4px 20px rgba(0,0,0,0.10)
- Smooth hover effect

---

# Header Section

## Title

Text:

```text
Sign In
```

Style:

- Font Size: 30px
- Font Weight: Bold
- Text Align: Center

---

## Subtitle

Text:

```text
Login to your account
```

Style:

- Font Size: 14px
- Color: #65676B
- Text Align: Center
- Margin Bottom: 24px

---

# Login Form

## Field 1

### Email or Mobile Number

Label:

```text
Email or Mobile Number
```

Placeholder:

```text
Enter email address or mobile number
```

Validation:

- Required field
- Accept valid email format
- Accept valid mobile number format
- Show validation message

Examples:

```text
john@example.com
9876543210
+919876543210
```

---

## Field 2

### Password

Label:

```text
Password
```

Placeholder:

```text
Enter password
```

Requirements:

- Required
- Minimum 8 characters
- Maximum 50 characters

Features:

- Show Password Icon
- Hide Password Icon
- Password Toggle Button

---

# Remember Me

Checkbox

Text:

```text
Remember Me
```

Features:

- Optional
- Save login session

---

# Login Button

Text:

```text
Login
```

Properties:

- Full Width
- Height: 50px
- Background: #1877F2
- Color: White
- Border Radius: 8px
- Font Weight: 600
- Cursor Pointer

Hover:

- Slightly darker blue
- Smooth transition

Action:

- Validate Form
- Submit Login Request

---

# Forgot Password

Text:

```text
Forgot Password?
```

Properties:

- Center Aligned
- Blue Text
- Hover Underline

Action:

- Navigate to Forgot Password Page

---

# Divider

Display separator:

```text
──────── OR ────────
```

Center aligned

---

# Create Account Button

Text:

```text
Create Account
```

Style:

- Full Width
- Height: 50px
- Background: #42B72A
- White Text
- Border Radius: 8px
- Font Weight: 600

Hover:

- Darker green

Action:

- Navigate to Registration Page

---

# Input Design

All Inputs Should Have:

- Height: 50px
- Border Radius: 8px
- Border: 1px solid #DADDE1
- Padding: 12px
- Width: 100%

Focus State:

- Border Color: #1877F2
- Blue Focus Ring

Error State:

- Red Border
- Error Message Below Input

---

# Validation Rules

## Email Validation

Must follow standard email format.

Example:

```text
user@example.com
```

---

## Mobile Validation

Requirements:

- Numeric input
- 10 to 15 digits
- Allow country code

Examples:

```text
9876543210
+919876543210
```

---

## Password Validation

Requirements:

- Minimum 8 Characters
- Maximum 50 Characters

Display validation errors when invalid.

---

# Responsive Design

## Desktop (1024px+)

Card Width:

```text
420px - 450px
```

Center aligned.

---

## Tablet (768px - 1023px)

Card Width:

```text
380px
```

Maintain center alignment.

Reduce padding slightly.

---

## Mobile (Below 768px)

Card Width:

```text
95%
```

Max Width:

```text
360px
```

Features:

- Full Width Inputs
- Full Width Buttons
- Touch Friendly Controls
- No Horizontal Scroll
- Proper Content Spacing

---

# Mobile Experience

Requirements:

- Mobile First Design
- Numeric Keypad for Mobile Number Input
- Soft Keyboard Should Not Hide Inputs
- Smooth Scrolling
- Fast Rendering
- Responsive UI
- Touch Targets Minimum 44px

---

# Accessibility

Must Support:

- Keyboard Navigation
- Tab Navigation
- Screen Readers
- ARIA Attributes
- High Contrast
- Focus Indicators

---

# Performance

Requirements:

- Lightweight UI
- Optimized CSS
- Fast Loading
- Responsive Rendering

Compatible With:

- Chrome
- Edge
- Firefox
- Safari
- Mobile Browsers

---

# Technology Stack

Preferred:

- React
- TypeScript
- Tailwind CSS

Alternative:

- HTML5
- CSS3
- JavaScript

---

# Required Components

Create following components:

1. LoginPage
2. LoginCard
3. LoginForm
4. EmailOrMobileInput
5. PasswordInput
6. RememberMeCheckbox
7. LoginButton
8. ForgotPasswordLink
9. CreateAccountButton

---

# UI Design Style

Design should look:

- Professional
- Enterprise Grade
- Modern
- Clean
- Minimal
- Mobile Friendly
- Production Ready

Use subtle shadows and smooth animations.

Do not include:

- Left side banner
- Hero section
- Marketing content
- Branding content

---

# Expected Output

Generate:

✅ Fully Responsive Login Page

✅ Email Login

✅ Mobile Number Login

✅ Password Login

✅ Remember Me Feature

✅ Forgot Password Functionality

✅ Create Account Button

✅ Form Validation

✅ Show/Hide Password

✅ Desktop Responsive

✅ Tablet Responsive

✅ Mobile Responsive

✅ Accessibility Support

✅ Production Ready UI

✅ React + TypeScript + Tailwind CSS Code

✅ Clean Folder Structure

✅ Enterprise Quality Design





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

import { useState } from "react";
import { Eye, EyeOff, AlertCircle } from "lucide-react";

// ─── Validation ───────────────────────────────────────────────────────────────

function validateIdentifier(v: string): string | undefined {
  if (!v.trim()) return "Email or mobile number is required";
  const isEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim());
  const isMobile = /^\+?[\d]{10,15}$/.test(v.trim().replace(/\s/g, ""));
  if (!isEmail && !isMobile) return "Enter a valid email address or 10–15 digit mobile number";
  return undefined;
}

function validatePassword(v: string): string | undefined {
  if (!v) return "Password is required";
  if (v.length < 8) return "Password must be at least 8 characters";
  if (v.length > 50) return "Password must be under 50 characters";
  return undefined;
}

// ─── Shared primitives ────────────────────────────────────────────────────────

function FieldError({ id, msg }: { id: string; msg: string }) {
  return (
    <p id={id} role="alert" className="mt-1.5 text-xs text-red-500 flex items-center gap-1">
      <AlertCircle className="w-3 h-3 flex-shrink-0" aria-hidden="true" />
      {msg}
    </p>
  );
}

function inputClass(err?: string) {
  return [
    "w-full h-[50px] px-3 border rounded-lg text-sm outline-none transition-all bg-white",
    err
      ? "border-red-400 focus:border-red-400 focus:ring-2 focus:ring-red-100"
      : "border-[#DADDE1] focus:border-[#1877F2] focus:ring-2 focus:ring-blue-100",
  ].join(" ");
}

// ─── EmailOrMobileInput ───────────────────────────────────────────────────────

function EmailOrMobileInput({
  value,
  onChange,
  error,
}: {
  value: string;
  onChange: (v: string) => void;
  error?: string;
}) {
  const looksNumeric = /^[\+\d]/.test(value);
  return (
    <div>
      <label htmlFor="identifier" className="block text-sm font-medium text-gray-700 mb-1.5">
        Email or Mobile Number
      </label>
      <input
        id="identifier"
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Enter email address or mobile number"
        inputMode={looksNumeric ? "numeric" : "email"}
        autoComplete="username"
        aria-invalid={!!error}
        aria-describedby={error ? "identifier-error" : undefined}
        className={inputClass(error)}
      />
      {error && <FieldError id="identifier-error" msg={error} />}
    </div>
  );
}

// ─── PasswordInput ────────────────────────────────────────────────────────────

function PasswordInput({
  value,
  onChange,
  error,
}: {
  value: string;
  onChange: (v: string) => void;
  error?: string;
}) {
  const [show, setShow] = useState(false);
  return (
    <div>
      <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1.5">
        Password
      </label>
      <div className="relative">
        <input
          id="password"
          type={show ? "text" : "password"}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder="Enter password"
          maxLength={50}
          autoComplete="current-password"
          aria-invalid={!!error}
          aria-describedby={error ? "password-error" : undefined}
          className={`${inputClass(error)} pr-11`}
        />
        <button
          type="button"
          onClick={() => setShow((s) => !s)}
          aria-label={show ? "Hide password" : "Show password"}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors p-0.5"
        >
          {show
            ? <EyeOff className="w-5 h-5" aria-hidden="true" />
            : <Eye className="w-5 h-5" aria-hidden="true" />}
        </button>
      </div>
      {error && <FieldError id="password-error" msg={error} />}
    </div>
  );
}

// ─── RememberMeCheckbox ───────────────────────────────────────────────────────

function RememberMeCheckbox({
  checked,
  onChange,
}: {
  checked: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <label className="flex items-center gap-2 cursor-pointer select-none min-h-[44px]">
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        className="w-4 h-4 rounded border-gray-300 cursor-pointer accent-[#1877F2]"
      />
      <span className="text-sm text-gray-600">Remember Me</span>
    </label>
  );
}

// ─── LoginButton ──────────────────────────────────────────────────────────────

function LoginButton({ loading }: { loading: boolean }) {
  return (
    <button
      type="submit"
      disabled={loading}
      className="w-full h-[50px] bg-[#1877F2] hover:bg-[#166FE5] active:bg-[#1469D5] disabled:opacity-60 disabled:cursor-not-allowed text-white font-semibold text-[15px] rounded-lg transition-colors duration-200 cursor-pointer"
    >
      {loading ? (
        <span className="flex items-center justify-center gap-2">
          <svg
            className="w-4 h-4 animate-spin"
            viewBox="0 0 24 24"
            fill="none"
            aria-hidden="true"
          >
            <circle
              className="opacity-25"
              cx="12" cy="12" r="10"
              stroke="currentColor" strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
            />
          </svg>
          Signing In…
        </span>
      ) : (
        "Login"
      )}
    </button>
  );
}

// ─── ForgotPasswordLink ───────────────────────────────────────────────────────

function ForgotPasswordLink() {
  return (
    <div className="text-center">
      <button
        type="button"
        className="text-sm text-[#1877F2] hover:underline font-medium transition-colors min-h-[44px] px-2"
        onClick={() => {/* navigate to /forgot-password */}}
      >
        Forgot Password?
      </button>
    </div>
  );
}

// ─── OrDivider ────────────────────────────────────────────────────────────────

function OrDivider() {
  return (
    <div className="flex items-center gap-3" role="separator" aria-label="or">
      <div className="flex-1 h-px bg-[#DADDE1]" />
      <span className="text-xs font-semibold text-gray-400 tracking-wide">OR</span>
      <div className="flex-1 h-px bg-[#DADDE1]" />
    </div>
  );
}

// ─── CreateAccountButton ──────────────────────────────────────────────────────

function CreateAccountButton() {
  return (
    <button
      type="button"
      className="w-full h-[50px] bg-[#42B72A] hover:bg-[#36A420] active:bg-[#2E8F1B] text-white font-semibold text-[15px] rounded-lg transition-colors duration-200 cursor-pointer"
      onClick={() => {/* navigate to /family-registration */}}
    >
      Create Account
    </button>
  );
}

// ─── Success State ────────────────────────────────────────────────────────────

function SuccessState({ identifier }: { identifier: string }) {
  return (
    <div className="text-center py-4" role="status" aria-live="polite">
      <div className="w-16 h-16 rounded-full bg-green-50 border-2 border-green-200 flex items-center justify-center mx-auto mb-4">
        <svg
          className="w-8 h-8 text-green-500"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={2.5}
          aria-hidden="true"
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
        </svg>
      </div>
      <p className="text-lg font-semibold text-gray-800">Login Successful!</p>
      <p className="text-sm text-gray-500 mt-1 mb-1">Signed in as</p>
      <p className="text-sm font-medium text-[#1877F2] truncate max-w-xs mx-auto">{identifier}</p>
      <p className="text-xs text-gray-400 mt-3">Redirecting to your dashboard…</p>
    </div>
  );
}

// ─── LoginForm ────────────────────────────────────────────────────────────────

function LoginForm() {
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(false);
  const [errors, setErrors] = useState<{ identifier?: string; password?: string }>({});
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  if (success) return <SuccessState identifier={identifier} />;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const idErr = validateIdentifier(identifier);
    const pwErr = validatePassword(password);
    if (idErr || pwErr) {
      setErrors({ identifier: idErr, password: pwErr });
      return;
    }
    setErrors({});
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setSuccess(true);
    }, 1600);
  };

  return (
    <form onSubmit={handleSubmit} noValidate aria-label="Login form">
      <div className="space-y-4">
        <EmailOrMobileInput
          value={identifier}
          onChange={(v) => {
            setIdentifier(v);
            setErrors((p) => ({ ...p, identifier: undefined }));
          }}
          error={errors.identifier}
        />

        <PasswordInput
          value={password}
          onChange={(v) => {
            setPassword(v);
            setErrors((p) => ({ ...p, password: undefined }));
          }}
          error={errors.password}
        />

        <RememberMeCheckbox checked={remember} onChange={setRemember} />

        <LoginButton loading={loading} />

        <ForgotPasswordLink />

        <OrDivider />

        <CreateAccountButton />
      </div>
    </form>
  );
}

// ─── LoginCard ────────────────────────────────────────────────────────────────

function LoginCard() {
  return (
    <div
      className="bg-white w-full rounded-xl p-8 transition-shadow duration-300"
      style={{
        maxWidth: "420px",
        boxShadow: "0px 4px 20px rgba(0,0,0,0.10)",
      }}
      onMouseEnter={(e) => {
        (e.currentTarget as HTMLDivElement).style.boxShadow =
          "0px 8px 32px rgba(0,0,0,0.14)";
      }}
      onMouseLeave={(e) => {
        (e.currentTarget as HTMLDivElement).style.boxShadow =
          "0px 4px 20px rgba(0,0,0,0.10)";
      }}
    >
      {/* Brand mark */}
      <div className="flex items-center justify-center gap-2 mb-6">
        <div className="w-8 h-8 rounded-full bg-[#7C1D1D] flex items-center justify-center flex-shrink-0">
          <span
            className="text-white font-bold text-sm leading-none"
            style={{ fontFamily: "'Playfair Display', Georgia, serif" }}
          >
            A
          </span>
        </div>
        <span className="text-sm font-semibold text-gray-600 tracking-wide">
          AgrawalPulse
        </span>
      </div>

      {/* Heading */}
      <h1 className="text-[30px] font-bold text-gray-900 text-center leading-tight mb-1">
        Sign In
      </h1>
      <p className="text-sm text-[#65676B] text-center mb-6">
        Login to your account
      </p>

      <LoginForm />
    </div>
  );
}

// ─── LoginPage ────────────────────────────────────────────────────────────────

export default function App() {
  return (
    <div
      className="min-h-screen w-full flex items-center justify-center px-4 py-8 font-sans"
      style={{ backgroundColor: "#F0F2F5" }}
    >
      <main className="w-full flex justify-center" role="main">
        <LoginCard />
      </main>
    </div>
  );
}
Quick reference for the agent:

Element	Value
Page background	#F0F2F5
Card	White, 420px, border-radius: 12px, box-shadow: 0px 4px 20px rgba(0,0,0,0.10)
Login button	#1877F2 → #166FE5 hover
Create Account button	#42B72A → #36A420 hover
Input focus border	#1877F2 with blue ring
Input error border	red-400 with red ring
Brand primary	#7C1D1D (maroon)
Fonts	Playfair Display (display), DM Sans (body)
Stack	React 18 + TypeScript + Tailwind CSS v4