# FocusNest Design System Specification

## 1. Color Palette Tokens

### 1.1 Semantic Colors (Dark Theme — Default)
- **Primary / Brand**: `#5BB381` (Bright Sage)
- **On Primary**: `#0D2818` (Deep Forest Ink)
- **Primary Container**: `#1E3F2B` (Subdued Leaf)
- **Background / Canvas**: `#0D1117` (Obsidian Dark)
- **Surface**: `#161B22` (Card Slate)
- **Surface Elevated**: `#21262D` (Elevated Tile)
- **Surface Border**: `rgba(255, 255, 255, 0.08)`
- **Text Primary**: `#F0F6FC` (Crisp Off-White)
- **Text Secondary / Muted**: `#8B949E` (Neutral Gray)
- **Success**: `#3FB950` (Vibrant Emerald)
- **Warning**: `#D29922` (Warm Amber)
- **Error**: `#F85149` (Soft Crimson)
- **Accent Focus**: `#38BDF8` (Sky Cyan)

### 1.2 Semantic Colors (Light Theme)
- **Primary / Brand**: `#2E7D32` (Forest Green)
- **Background / Canvas**: `#F8F9FA` (Clean Paper)
- **Surface**: `#FFFFFF` (Pure White)
- **Surface Elevated**: `#F1F3F5` (Soft Gray)
- **Surface Border**: `rgba(0, 0, 0, 0.08)`
- **Text Primary**: `#1F2937` (Dark Charcoal)
- **Text Secondary**: `#6B7280` (Muted Slate)

---

## 2. Typography Hierarchy (Material 3 Scale)

| Token | Size | Line Height | Weight | Usage |
| :--- | :--- | :--- | :--- | :--- |
| **Display Large** | 36sp | 44sp | Bold (700) | Metric Hero Gauges |
| **Headline Large** | 28sp | 34sp | Semi-Bold (600) | Top Screen Titles |
| **Headline Medium**| 22sp | 28sp | Semi-Bold (600) | Section Headers |
| **Title Large** | 18sp | 24sp | Medium (500) | Card Headlines |
| **Title Medium** | 16sp | 22sp | Medium (500) | Tile Titles |
| **Body Large** | 15sp | 22sp | Regular (400) | Primary Explanations |
| **Body Medium** | 13sp | 18sp | Regular (400) | Sub-captions & Descriptions |
| **Label Large** | 13sp | 16sp | Semi-Bold (600) | Buttons & Tabs |
| **Label Medium** | 11sp | 14sp | Medium (500) | Badges & Chips |

---

## 3. Spacing Grid (4dp Increments)
- **Micro (2dp - 4dp)**: Spacing between badge icons and text, inner chip padding.
- **Compact (8dp)**: Spacing between grid items, list item vertical gaps.
- **Default (16dp)**: Card inner padding, screen horizontal edge padding.
- **Section (24dp)**: Margin between major card sections.
- **Hero (32dp - 48dp)**: Top padding for large gauges and modal dialogues.

---

## 4. Reusable Compose Component Catalog
- `FocusNestButton`: Primary, secondary, and outlined buttons with minimum 48dp touch targets and ripple animations.
- `SanctuaryCard`: Elevated surface container with subtle border highlights and elevation tiers.
- `CircularProgressGauge`: SVG/Canvas arc renderer with smooth animated stroke-dashoffset transitions.
- `StreakStrip`: 7-day horizontal calendar tracker with checked and pending day markers.
- `AudioWaveformVisualizer`: 12-bar bouncing equalizer visualizer for procedural ambient focus sounds.
- `AppRowItem`: Standardized installed application row with app icon, usage limit indicators, and Material 3 toggle switch.
