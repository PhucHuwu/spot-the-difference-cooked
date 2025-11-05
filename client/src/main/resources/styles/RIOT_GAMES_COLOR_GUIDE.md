# 🎨 Riot Games - Color Palette Guide

## Lobby Tables Styling Documentation

### Color Philosophy
The styling follows Riot Games' design language, particularly inspired by League of Legends client interface:
- **Premium feel** with gold accents
- **Dark, sleek backgrounds** for focus
- **High contrast** for readability
- **Glowing effects** for interactive elements

---

## 📊 Color Palette

### Primary Colors (Gold Theme)
| Color | Hex Code | Usage |
|-------|----------|-------|
| **Primary Gold** | `#C8AA6E` | Headers, borders, highlights |
| **Light Gold** | `#F0E6D2` | Primary text, light accents |
| **Dark Gold** | `#785A28` | Shadows, dark gradients |

### Background Colors
| Color | Hex Code | Usage |
|-------|----------|-------|
| **Deep Dark** | `#010A13` | Main backgrounds |
| **Medium Dark** | `#1E2328` | Secondary backgrounds |
| **Subtle Gray** | `#5B5A56` | Placeholder text |

### Accent Colors
| Color | Hex Code | Usage |
|-------|----------|-------|
| **Cyan Blue** | `#0AC8B9` | Online status, interactive hover |
| **Teal** | `#0397AB` | Secondary blue accent |
| **Red** | `#D13639` | In-game status, alerts |

### Rank Colors
| Rank | Color | Hex Code | Effect |
|------|-------|----------|--------|
| **1st Place** | Gold | `#FFD700` | Bright gold glow |
| **2nd Place** | Silver | `#C0C0C0` | Silver shine |
| **3rd Place** | Bronze | `#CD7F32` | Bronze metallic |
| **Top 10** | Premium Gold | `#C8AA6E` | Riot gold |

---

## 🎯 Application Areas

### 1. Table Headers
```css
Background: linear-gradient(rgba(1, 10, 19, 0.95), rgba(30, 35, 40, 0.95))
Border: rgba(200, 170, 110, 0.3) - bottom 2px
Text: #C8AA6E (bold, 11px, letter-spacing: 0.5px)
```

### 2. Table Cells
```css
Background: Transparent
Text: #F0E6D2 (13px)
Border: rgba(200, 170, 110, 0.08) - bottom 1px
```

### 3. Row Alternating
```css
Odd rows: rgba(200, 170, 110, 0.03)
Even rows: rgba(1, 10, 19, 0.3)
```

### 4. Hover State
```css
Background: rgba(10, 200, 185, 0.12) - Cyan glow
Text: #0AC8B9 - All text turns cyan
```

### 5. Selected Row
```css
Background: rgba(200, 170, 110, 0.25) - Gold highlight
Text: #F0E6D2 (bold)
```

### 6. Status Indicators

#### Online Status
```css
Color: #0AC8B9 (Cyan)
Effect: dropshadow with rgba(10, 200, 185, 0.4)
```

#### In-Game Status
```css
Color: #D13639 (Red)
Effect: dropshadow with rgba(209, 54, 57, 0.4)
```

### 7. Scrollbar
```css
Background: rgba(1, 10, 19, 0.5)
Thumb: linear-gradient(#C8AA6E, #785A28)
Hover: linear-gradient(#F0E6D2, #C8AA6E)
```

### 8. Action Buttons
```css
Background: rgba(200, 170, 110, 0.15)
Border: rgba(200, 170, 110, 0.4)
Text: #F0E6D2

Hover:
  Background: rgba(200, 170, 110, 0.3)
  Border: #C8AA6E
  Text: #C8AA6E
  Glow: rgba(200, 170, 110, 0.6)
```

---

## 🌟 Special Effects

### Gold Glow (Rank 1-3)
```css
-fx-effect: dropshadow(gaussian, rgba(255, 215, 0, 0.6), 6, 0.7, 0, 0);
```

### Cyan Glow (Online, Hover)
```css
-fx-effect: dropshadow(gaussian, rgba(10, 200, 185, 0.4), 4, 0.6, 0, 0);
```

### Red Glow (In-Game)
```css
-fx-effect: dropshadow(gaussian, rgba(209, 54, 57, 0.4), 4, 0.6, 0, 0);
```

---

## 💡 Design Principles

1. **Contrast**: Always maintain high contrast between text and background
2. **Hierarchy**: Use gold for important elements, cyan for interactive
3. **Consistency**: Apply same colors for same meanings across the UI
4. **Feedback**: Hover states use cyan glow, active states use gold
5. **Accessibility**: All text meets WCAG AA standards for readability

---

## 📝 CSS Class Reference

| Class Name | Purpose | Key Colors |
|------------|---------|------------|
| `.status-online` | Online status indicator | `#0AC8B9` |
| `.status-ingame` | In-game status indicator | `#D13639` |
| `.rank-1` | First place rank | `#FFD700` |
| `.rank-2` | Second place rank | `#C0C0C0` |
| `.rank-3` | Third place rank | `#CD7F32` |
| `.rank-top10` | Top 10 rank | `#C8AA6E` |
| `.player-name` | Standard player name | `#F0E6D2` |
| `.player-name-self` | Current player highlight | `#C8AA6E` |
| `.points-cell` | Points/score display | `#0AC8B9` |

---

## 🔧 Customization Tips

### To make elements more subtle:
- Reduce alpha channel in rgba() values
- Use darker gold shades (`#785A28` instead of `#C8AA6E`)

### To increase prominence:
- Increase glow radius and spread
- Use lighter gold (`#F0E6D2` instead of `#C8AA6E`)
- Add multiple layered dropshadows

### To add new status types:
1. Choose a color from the accent palette
2. Create a new CSS class with matching glow effect
3. Apply in the controller's cell factory

---

## 📚 References

- [League of Legends Client Design](https://www.leagueoflegends.com)
- [Riot Games Branding](https://www.riotgames.com/en/branding)
- Color values derived from official LoL client (2023-2025)

---

**Last Updated**: November 5, 2025
**Styled By**: AI Assistant
**Design System**: Riot Games / League of Legends Inspired

