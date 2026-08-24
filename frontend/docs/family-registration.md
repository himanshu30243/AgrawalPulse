# AgrawalPulse - Family Registration Page

## Page Information

- Page Name: Family Registration
- Route: `/v1/families`
- Module: Family Management
- Next Page: Member Registration (`v1/families/{{familyId}}/members`)
- Form Type: Multi-Step Wizard
- Progress Indicator: Enabled

---

# Step 1: Basic Information

## Section: Head of Family

### Head of Family Full Name *
Type: Text

Fields:
- First Name *
- Middle Name
- Last Name *

Validation:
- Required
- Alphabets only

---

### Head's Gender *

Type: Dropdown

Options:
- Male
- Female
- Other

Validation:
- Required

---

### Head's Date of Birth *

Type: User Friendly DOB Picker

UI:
- Day Dropdown
- Month Dropdown
- Year Dropdown

Example:
Day [01 ▼]
Month [January ▼]
Year [1985 ▼]
 
Features:
- Mobile Friendly
- Easy for Senior Citizens
- Quick Year Selection
- No Calendar Navigation Required
 
Validation:
- Required
- Cannot be Future Date

---

### Mobile Number *

Type: Mobile Input

Validation:
- Required
- 10 Digits
- Unique

---

### Email Address

Type: Email

Validation:
- Valid Email Format

---

### Aadhaar Number

Type: Text

Validation:
- 12 Digits
- Optional

---

### Profile Photo

Type: File Upload

Accepted Formats:
- JPG
- JPEG
- PNG

Max Size:
- 2 MB

---

# Step 2: Address Information

## Address Details

### Address *

Type: Text Area

Validation:
- Required

---

### Country *

Type: Dropdown

Source:
- Master Country Table

Default:
- India

---

### State *

Type: Dropdown

Dependency:
- Selected Country

Source:
- Master State Table

---

### District *

Type: Dropdown

Dependency:
- Selected State

Source:
- Master District Table

---

### Area / Locality *

Type: Text

Validation:
- Required

---

### PIN Code *

Type: Numeric

Validation:
- 6 Digits

---

# Step 3: Community Information

## Community Details

### Samaj *

Type: Dropdown

Options:
- Agrawal
- Other

Validation:
- Required

---

### Gotra *

Type: Dropdown

Options:

- Airan
- Bansal
- Bindal
- Garg
- Goyal
- Goenka
- Jindal
- Kansal
- Mittal
- Mangal
- Singhal
- Tayal
- Dharan
- Kuchhal
- Madhukul
- Nangal
- Tingle
- Bhandal
- Other

Validation:
- Required

---

### Other Gotra

Display Condition:
- Show only when Gotra = Other

Type:
- Text

---

### Native Place (Mool Gaon) *

Type: Text

Validation:
- Required

---

### Region / City *

Type: Auto Populate

Source:
- Selected District

Read Only:
- Yes

---

# Step 4: Financial & Social Details

## Optional Section

### Occupation / Business Type

Type: Text

Examples:
- Software Engineer
- Business Owner
- Doctor
- CA

---

### Annual Income Range

Type: Dropdown

Options:
- Below ₹5 Lakh
- ₹5-10 Lakh
- ₹10-25 Lakh
- ₹25-50 Lakh
- ₹50 Lakh-1 Crore
- Above ₹1 Crore

---

### Family Category

Type: Dropdown

Options:
- Business
- Salaried
- Professional
- Retired
- Agriculture
- Other

---

### Own Two Wheeler
 
Type: Radio Button

Options:
- Yes
- No
 
Default:
- No
 
---
 
### Own Four Wheeler

Type: Radio Button

Options:
- Yes
- No

Default:
- No
 
---

### Own Home

Type: Radio Button

Options:
- Yes
- No

Default:
- No
 
---

### Own Plot

Type: Radio Button
 
Options:
- Yes
- No

Default:
- No

---

### Willing to Contribute to Community Activities?

Type: Radio Button

Options:
- Yes
- No

---

# System Generated Fields

| Field | Description |
|---------|------------|
| Family ID | Auto Generated |
| Registration Date | System Date |
| Status | Active |
| Total Members | Default 1 |

---

# Navigation Logic

## On Save Draft

Action:
- Save Current Data
- Remain on Same Page

---

## On Submit

Validation:
- Complete All Mandatory Fields

Actions:

1. Create Family Record
2. Generate Family ID
3. Save Photo
4. Create Initial Head of Family Member Record
5. Store Registration Details

After Successful Save:

Navigate To:
`/member-registration?familyId={generatedFamilyId}`

---

# Member Registration Page Launch

Pass Parameters:

```json
{
  "familyId": "AUTO_GENERATED",
  "familyHeadName": "HEAD_OF_FAMILY_NAME",
  "gotra": "SELECTED_GOTRA",
  "moolGaon": "NATIVE_PLACE"
}
```

Purpose:

- Register Spouse
- Register Children
- Register Parents
- Register Other Family Members

---

# Buttons

## Bottom Navigation

Previous Button:
- Visible From Step 2 Onwards

Next Button:
- Visible Until Last Step

Save Draft Button:
- Always Visible

Submit Button:
- Visible On Final Step

---

# Success Message

Title:
Family Registration Successful

Message:
Your Family Registration has been completed successfully.

Generated Family ID:
{FamilyId}

Redirecting to Member Registration Page...

---

# UI Layout Recommendation

Desktop:
- Two Column Layout

Mobile:
- Single Column Layout

Theme:
- Community Portal
- Orange + Maroon Accent
- Responsive Design

Progress Bar:

Step 1 → Basic Information
Step 2 → Address Information
Step 3 → Community Information
Step 4 → Financial Details
Step 5 → Member Registration