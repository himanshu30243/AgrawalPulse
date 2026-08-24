This is the member registration page final page 2

# AgrawalPulse - Family Member Registration Page

## Page Information

- Page Name: Family Member Registration
- Route: `/member-registration`
- Module: Family Management
- Parent Module: Family Registration
- Form Type: Single Page Form / Multi Member Entry
- Progress Indicator: Enabled

---

# Family Information (Auto Populated)

These details are received from Family Registration Page.

### Family ID

Type: Read Only

Source:
- Family Registration

---

### Family Head Name

Type: Read Only

Source:
- Family Registration

---

### Gotra

Type: Read Only

Source:
- Family Registration

---

### Native Place (Mool Gaon)

Type: Read Only

Source:
- Family Registration

---

# Member Information

## Full Name *

Type: Text

Fields:
- First Name *
- Middle Name
- Last Name *

Validation:
- Required
- Alphabets Only

---

## Relationship to Head *

Type: Dropdown

Options:
- Self
- Spouse
- Son
- Daughter
- Father
- Mother
- Grandfather
- Grandmother
- Brother
- Sister
- Uncle
- Aunt
- Nephew
- Niece
- Cousin
- Other

Validation:
- Required

---

## Gender *

Type: Dropdown

Options:
- Male
- Female
- Other

Validation:
- Required

---

## Date of Birth *

Type: User Friendly DOB Picker

UI:
- Day Dropdown
- Month Dropdown
- Year Dropdown

Example:

Day   [01 ▼]
Month [January ▼]
Year  [2000 ▼]

Features:
- Easy for Senior Citizens
- Mobile Friendly
- Quick Year Selection
- Searchable Year List
- No Calendar Navigation Required

Validation:
- Required
- Cannot be Future Date

---

## Marital Status

Type: Dropdown

Options:
- Single
- Married
- Divorced
- Widowed
- Separated

---

## Education

Type: Dropdown

Options:
- Primary School
- Secondary School
- Higher Secondary
- Diploma
- Graduate
- Post Graduate
- Doctorate (PhD)
- Professional Degree
- Other

---

## Profession

Type: Text

Examples:
- Student
- Software Engineer
- Doctor
- Chartered Accountant
- Business Owner
- Government Employee
- Teacher
- Advocate

---

## Current City

Type: Text

Validation:
- Required

---

## Annual Income

Type: Dropdown

Options:
- No Income
- Below ₹5 Lakh
- ₹5-10 Lakh
- ₹10-25 Lakh
- ₹25-50 Lakh
- ₹50 Lakh-1 Crore
- Above ₹1 Crore

Optional:
- Yes

---

## Mobile Number

Type: Mobile Input

Validation:
- 10 Digits

Display Condition:
- Applicable for members above 18 years

Optional:
- Yes

---

## Blood Group

Type: Dropdown

Options:
- A+
- A-
- B+
- B-
- AB+
- AB-
- O+
- O-

Optional:
- Yes

---

## Profile Photo

Type: File Upload

Accepted Formats:
- JPG
- JPEG
- PNG

Max Size:
- 2 MB

Optional:
- Yes

---

## Aadhaar Number

Type: Text

Validation:
- Must be 12 Digits

Optional:
- Yes

---

# System Generated Fields

| Field | Description |
|---------|-------------|
| Member ID | Auto Generated |
| Family ID | From Parent Family Record |
| Registration Date | System Date |
| Status | Active |

---

# Validation Rules

## Mandatory Fields

- First Name
- Last Name
- Relationship to Head
- Gender
- Date of Birth

---

## Business Rules

### Relationship Validation

- Only one "Self" record allowed per family.
- Only one "Spouse" record allowed for family head.
- Parents and children can have multiple records.

---

### Age Validation

- Date of Birth cannot be future date.
- Age automatically calculated from Date of Birth.

---

# Navigation Logic

## Save Draft

Action:
- Save Current Member Details
- Stay on Same Page

---

## Save Member

Actions:
1. Validate Member Information
2. Generate Member ID
3. Save Member Record
4. Associate Member with Family ID
5. Save Uploaded Photo

After Save:

Message:
- Family Member Added Successfully

Option:
- Add Another Member

---

## Complete Registration

Action:
- Finalize Family Registration
- Navigate to Family Dashboard

---

# Buttons

## Bottom Navigation

Add Member Button:
- Save Current Member
- Clear Form
- Ready for Next Member Entry

Save Draft Button:
- Always Visible

Complete Registration Button:
- Visible After At Least One Member Is Added

Cancel Button:
- Return to Family Dashboard

---

# Success Message

Title:
Member Registration Successful

Message:
Family Member has been added successfully.

Generated Member ID:
{MemberId}

Options:
- Add Another Member
- Complete Registration

---

# UI Layout Recommendation

## Desktop

- Two Column Layout

Left Section:
- Personal Information

Right Section:
- Contact & Other Information

---

## Mobile

- Single Column Layout

---

## Theme

- Community Portal
- Orange + Maroon Accent
- Responsive Design

---

# Integration with Family Registration

Input Parameters:

```json
{
  "familyId": "AUTO_GENERATED",
  "familyHeadName": "HEAD_OF_FAMILY_NAME",
  "gotra": "SELECTED_GOTRA",
  "moolGaon": "NATIVE_PLACE"
}