# Dismiss Zone Styling Guide

The dismiss zone now supports advanced styling with gradients and curved corners.

## Basic Configuration

```typescript
import FloatingWidget from 'react-native-floating-app-widget';

await FloatingWidget.init({
  notification: {
    title: 'Widget Active',
    text: 'Tap to open app',
  },
  dismissZone: {
    enabled: true,
    showOn: 'longPress', // Only show after long press
    height: 120,
    text: '🗑️ Release to remove',
    textColor: '#FFFFFF',
    textSize: 18,
  },
});
```

## Solid Color Background

Use `backgroundColor` and `activeBackgroundColor` for solid colors:

```typescript
dismissZone: {
  enabled: true,
  backgroundColor: '#96FF0000', // Semi-transparent red
  activeBackgroundColor: '#C8FF0000', // More opaque when active
  cornerRadius: 20, // Curved corners
}
```

## Gradient Background

Use `gradientColors` for gradient backgrounds (2-5 colors):

### Vertical Gradient (Default)

```typescript
dismissZone: {
  enabled: true,
  gradientColors: ['#FF0000', '#FF6600', '#FFAA00'], // Red to orange to yellow
  activeGradientColors: ['#CC0000', '#CC4400', '#CC8800'], // Darker when active
  gradientOrientation: 'vertical', // Top to bottom
  cornerRadius: 25,
}
```

### Horizontal Gradient

```typescript
dismissZone: {
  enabled: true,
  gradientColors: ['#8B00FF', '#FF1493'], // Purple to pink
  gradientOrientation: 'horizontal', // Left to right
  cornerRadius: 30,
}
```

### Diagonal Gradients

```typescript
// Top-left to bottom-right
dismissZone: {
  enabled: true,
  gradientColors: ['#00C6FF', '#0072FF'],
  gradientOrientation: 'diagonal-tl-br',
  cornerRadius: 20,
}

// Bottom-left to top-right
dismissZone: {
  enabled: true,
  gradientColors: ['#F857A6', '#FF5858'],
  gradientOrientation: 'diagonal-bl-tr',
  cornerRadius: 20,
}
```

## Advanced Examples

### Modern Gradient with Active State

```typescript
dismissZone: {
  enabled: true,
  showOn: 'longPress',
  height: 100,

  // Normal state - subtle gradient
  gradientColors: ['#667eea', '#764ba2', '#f093fb'],

  // Active state - vibrant gradient
  activeGradientColors: ['#4568dc', '#b06ab3', '#dd5e89'],

  gradientOrientation: 'vertical',
  cornerRadius: 30,

  text: '🗑️ Drop here to remove',
  textColor: '#FFFFFF',
  textSize: 16,
}
```

### Glass Morphism Effect

```typescript
dismissZone: {
  enabled: true,
  gradientColors: ['#FFFFFF33', '#FFFFFF1A'], // Semi-transparent white
  activeGradientColors: ['#FF000055', '#FF000033'], // Semi-transparent red when active
  cornerRadius: 25,
  text: 'Release to dismiss',
  textColor: '#FFFFFF',
  textSize: 14,
}
```

### Neon Glow Effect

```typescript
dismissZone: {
  enabled: true,
  gradientColors: ['#00F5FF', '#00D9FF', '#00BDFF'],
  activeGradientColors: ['#FF0080', '#FF0040', '#FF0000'],
  gradientOrientation: 'horizontal',
  cornerRadius: 40,
  text: '⚡ Release to remove',
  textColor: '#FFFFFF',
  textSize: 18,
}
```

## Complete Example

```typescript
import React, { useEffect } from 'react';
import FloatingWidget from 'react-native-floating-app-widget';

export default function App() {
  useEffect(() => {
    setupWidget();
  }, []);

  const setupWidget = async () => {
    try {
      // Initialize widget with gradient dismiss zone
      await FloatingWidget.init({
        size: 60,
        shape: 'circle',

        notification: {
          title: 'Widget Running',
          text: 'Long press to dismiss',
        },

        dismissZone: {
          enabled: true,
          showOn: 'longPress', // Only show on long press
          height: 120,

          // Beautiful gradient
          gradientColors: ['#667eea', '#764ba2', '#f093fb'],
          activeGradientColors: ['#4568dc', '#b06ab3', '#dd5e89'],
          gradientOrientation: 'vertical',

          // Curved edges
          cornerRadius: 30,

          text: '🗑️ Drop to remove',
          textColor: '#FFFFFF',
          textSize: 16,
          position: 'bottom',
        },

        appearance: {
          backgroundColor: '#007AFF',
          borderColor: '#0056b3',
          borderWidth: 2,
          cornerRadius: 30,
        },
      });

      // Check permission
      const hasPermission = await FloatingWidget.hasPermission();
      if (!hasPermission) {
        await FloatingWidget.requestPermission();
      }

      // Start the widget
      await FloatingWidget.start();
    } catch (error) {
      console.error('Widget setup failed:', error);
    }
  };

  return <YourApp />;
}
```

## Configuration Options

### DismissZoneConfig

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | boolean | `false` | Enable dismiss zone |
| `showOn` | `'always'` \| `'longPress'` | `'always'` | When to show dismiss zone |
| `height` | number | `100` | Height in dp |
| `backgroundColor` | string | `'#96FF0000'` | Solid background color (hex) |
| `activeBackgroundColor` | string | `'#C8FF0000'` | Active state color (hex) |
| `gradientColors` | string[] | `undefined` | Array of 2-5 gradient colors |
| `activeGradientColors` | string[] | `undefined` | Active state gradient colors |
| `gradientOrientation` | `'vertical'` \| `'horizontal'` \| `'diagonal-tl-br'` \| `'diagonal-bl-tr'` | `'vertical'` | Gradient direction |
| `cornerRadius` | number | `0` | Corner radius in dp (0 for rectangle) |
| `text` | string | `'⊗ Release to remove'` | Display text |
| `textColor` | string | `'#FFFFFF'` | Text color (hex) |
| `textSize` | number | `16` | Text size in sp |
| `position` | `'top'` \| `'bottom'` | `'bottom'` | Zone position |

## Notes

- **Gradient Priority**: If `gradientColors` is provided, it takes priority over `backgroundColor`
- **Memory Efficient**: Gradients use native `GradientDrawable` - no bitmap allocation
- **Color Format**: Use hex strings with optional alpha: `'#RRGGBBAA'` or `'#RRGGBB'`
- **Corner Radius**: Applied only when > 0, works with both solid and gradient backgrounds
- **Active State**: Automatically switches between normal and active colors when widget enters dismiss zone
