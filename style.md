# Design Guidelines: hotel-management System

This document serves as the source of truth for the visual identity and structural patterns of the hotel-management platform. AI assistants should strictly adhere to these guidelines to ensure brand consistency across all new screens and edits.

## 1. Brand Identity & Core Assets
- **Product Name**: hotel-management
- **Logo**: Use the high-fidelity brand mark {{DATA:IMAGE:IMAGE_17}}.
- **Core Aesthetic**: Premium, enterprise-level hospitality. Minimalist, professional, and trustworthy.

## 2. Design System (hotel-management) - Visual Language

### Color Palette
- **Primary Color**: `#0f4c81` (Deep Navy / Azure) - used for primary buttons, active states, and brand elements.
- **Secondary Color**: `#cbdbf5` / `#eff4ff` (Light Azure tints) - used for containers, backgrounds, and accents.
- **Surface Colors**: 
  - `Surface`: #f8f9ff (Main background for light mode).
  - `Surface-Container`: #eff4ff (Used for secondary sections or grouping elements).
  - `Surface-Bright`: #ffffff (Used for primary cards and input fields).
- **Status Colors (Functional)**:
  - *Success/Available*: Emerald Green (#10b981 background tint).
  - *Warning/Booked*: Amber/Gold (#f59e0b background tint).
  - *Error/Maintenance*: Crimson/Red (#ef4444 background tint).

### Typography
- **Primary Typeface**: **Hanken Grotesk** (Sans-serif).
  - **Headings**: Use Bold/Semi-bold weights with tight tracking (-0.02em). High contrast against surfaces.
  - **Body Copy**: Regular weight, minimum 16px for readability. Maintain generous line height (1.6).
  - **Labels**: Medium weight, strictly using the hotel-management token scale.

### Shape & Form
- **Corner Radius**: `ROUND_EIGHT` (8px) for all primary components (cards, buttons, input fields, badges).
- **Elevation**: Minimal. Use subtle shadows (`shadow-sm`) or 1px borders (`outline-variant`) to define hierarchy rather than heavy dropshadows.

## 3. Global Layout Patterns

### Header (Navigation)
- **Style**: Glassmorphism effect (`bg-white/70` with `backdrop-blur-xl`).
- **Logo Placement**: Logo {{DATA:IMAGE:IMAGE_17}} on the far left, followed by the text "hotel-management" in primary navy.
- **Navigation Links**: Centered or right-aligned. Active states must feature a 2px bottom border in `#0f4c81`.

### Footer (Corporate)
- **Style**: Solid deep navy background (`#002b49` or primary navy).
- **Structure**: 4-column grid for Desktop.
  - Column 1: Brand logo (white version) and mission statement.
  - Column 2: "Company" links.
  - Column 3: "Connect" links.
  - Column 4: "Newsletter" signup with an input field and arrow button.
- **Typography**: High-contrast white or light azure text on dark backgrounds.

## 4. Component Patterns

### Data Tables (Admin & Owner Portals)
- **Layout**: Full-width, high-density rows.
- **Interactions**: Row hover states (`bg-surface-container-low`), clear action buttons (view, edit, delete icons).

### Forms & Inputs
- **Style**: 8px radius, subtle 1px border. Floating or top-aligned labels.
- **Buttons**:
  - *Primary*: Solid `#0f4c81`, white text, 8px radius.
  - *Secondary*: Outlined or light surface tint.

## 5. UI Principles
- **Continuity**: Maintain the premium enterprise feel. Workspace areas should always be full-width (container-max).
- **Precision**: Replicate the exact header/footer structure from {{DATA:SCREEN:SCREEN_6}} (Landing Page) and {{DATA:SCREEN:SCREEN_71}} (Room Management).
- **Whitespace**: Use generous internal padding to maintain a luxury, breathable aesthetic.

---
*Authorized for use in all subsequent generation and edit tasks for the Can Tho Hotel project.*