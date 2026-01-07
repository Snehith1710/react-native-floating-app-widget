/**
 * Widget shape types
 */
export type WidgetShape = 'circle' | 'rounded';

/**
 * Widget position on screen
 */
export interface WidgetPosition {
  x: number;
  y: number;
}

/**
 * Notification configuration for the foreground service
 */
export interface NotificationConfig {
  /**
   * Notification title
   */
  title: string;

  /**
   * Notification body text
   */
  text: string;

  /**
   * Notification channel ID (Android 8.0+)
   */
  channelId?: string;

  /**
   * Notification channel name (Android 8.0+)
   */
  channelName?: string;

  /**
   * Small icon resource name (without extension, should exist in android/app/src/main/res/drawable)
   * Defaults to app icon
   */
  icon?: string;
}

/**
 * Widget click event
 */
export interface WidgetClickEvent {
  /**
   * Timestamp of the click event
   */
  timestamp: number;
}

/**
 * Widget long press event
 */
export interface WidgetLongPressEvent {
  /**
   * Timestamp of the long press event
   */
  timestamp: number;

  /**
   * Duration of the press in milliseconds
   */
  duration: number;
}

/**
 * Widget drag event
 */
export interface WidgetDragEvent {
  /**
   * Current X position
   */
  x: number;

  /**
   * Current Y position
   */
  y: number;

  /**
   * Indicates if widget is in the dismiss zone
   */
  inDismissZone: boolean;
}

/**
 * Widget position
 */
export interface WidgetPositionEvent {
  /**
   * X position
   */
  x: number;

  /**
   * Y position
   */
  y: number;
}

/**
 * Widget appearance configuration
 */
export interface WidgetAppearance {
  /**
   * Background color as hex string (e.g., '#FFFFFF' or '#CCFFFFFF' with alpha)
   * Default: '#CCFFFFFF' (semi-transparent white)
   */
  backgroundColor?: string;

  /**
   * Border color as hex string
   * Default: '#FFCCCCCC' (light gray)
   */
  borderColor?: string;

  /**
   * Border width in dp
   * Default: 2
   */
  borderWidth?: number;

  /**
   * Padding in dp
   * Default: 8
   */
  padding?: number;

  /**
   * Widget opacity (0.0 - 1.0)
   * Default: 1.0
   */
  opacity?: number;

  /**
   * Corner radius in dp (for rounded shape)
   * Default: 12
   */
  cornerRadius?: number;
}

/**
 * Dismiss zone configuration
 */
export interface DismissZoneConfig {
  /**
   * Enable drag-to-dismiss functionality
   * Default: false
   */
  enabled?: boolean;

  /**
   * When to show the dismiss zone
   * - 'always': Show during any drag (default for backward compatibility)
   * - 'longPress': Only show after long press
   * Default: 'always'
   */
  showOn?: 'always' | 'longPress';

  /**
   * Height of the dismiss zone from bottom of screen in dp
   * Default: 100
   */
  height?: number;

  /**
   * Background color as hex string (e.g., '#F44336CC')
   * For gradient, use gradientColors instead
   * Default: '#96FF0000' (semi-transparent red)
   */
  backgroundColor?: string;

  /**
   * Color when widget is in dismiss zone
   * For gradient, use activeGradientColors instead
   * Default: '#C8FF0000' (more opaque red)
   */
  activeBackgroundColor?: string;

  /**
   * Gradient colors for background (array of 2-5 colors)
   * Example: ['#FF0000', '#FF6600', '#FFAA00']
   * Overrides backgroundColor when provided
   */
  gradientColors?: string[];

  /**
   * Active gradient colors when widget is in dismiss zone
   * Overrides activeBackgroundColor when provided
   */
  activeGradientColors?: string[];

  /**
   * Gradient orientation
   * - 'horizontal': Left to right
   * - 'vertical': Top to bottom
   * - 'diagonal-tl-br': Top-left to bottom-right
   * - 'diagonal-bl-tr': Bottom-left to top-right
   * Default: 'vertical'
   */
  gradientOrientation?: 'horizontal' | 'vertical' | 'diagonal-tl-br' | 'diagonal-bl-tr';

  /**
   * Corner radius for curved edges in dp
   * Set to 0 for rectangle
   * Default: 0
   */
  cornerRadius?: number;

  /**
   * Text displayed in dismiss zone
   * Default: '⊗ Release to remove'
   */
  text?: string;

  /**
   * Text color as hex string
   * Default: '#FFFFFF' (white)
   */
  textColor?: string;

  /**
   * Text size in sp
   * Default: 16
   */
  textSize?: number;

  /**
   * Position of dismiss zone
   * Default: 'bottom'
   */
  position?: 'top' | 'bottom';
}

/**
 * Animation configuration
 */
export interface AnimationConfig {
  /**
   * Duration of snap-to-edge animation in milliseconds
   * Default: 300
   */
  snapDuration?: number;

  /**
   * Interpolator type for animations
   * Default: 'decelerate'
   */
  snapInterpolator?: 'decelerate' | 'accelerate' | 'linear' | 'bounce' | 'overshoot';

  /**
   * Enable scale down effect when pressed
   * Default: false
   */
  enableScaleOnPress?: boolean;

  /**
   * Scale factor when pressed (0.0 - 1.0)
   * Default: 0.9
   */
  pressScale?: number;

  /**
   * Enable haptic feedback on interactions
   * Default: false
   */
  enableHapticFeedback?: boolean;
}

/**
 * Position constraints configuration
 */
export interface PositionConstraints {
  /**
   * Minimum X position in pixels
   */
  minX?: number;

  /**
   * Maximum X position in pixels
   */
  maxX?: number;

  /**
   * Minimum Y position in pixels
   */
  minY?: number;

  /**
   * Maximum Y position in pixels
   */
  maxY?: number;

  /**
   * Prevent widget from being dragged off screen
   * Default: false
   */
  keepOnScreen?: boolean;

  /**
   * Snap widget to grid (in dp)
   * Set to 0 to disable
   * Default: 0
   */
  snapToGrid?: number;
}

/**
 * Badge configuration
 */
export interface BadgeConfig {
  /**
   * Badge text (e.g., '3', 'NEW')
   */
  text?: string;

  /**
   * Badge count (alternative to text)
   */
  count?: number;

  /**
   * Badge position
   * Default: 'top-right'
   */
  position?: 'top-right' | 'top-left' | 'bottom-right' | 'bottom-left';

  /**
   * Background color as hex string
   * Default: '#F44336' (red)
   */
  backgroundColor?: string;

  /**
   * Text color as hex string
   * Default: '#FFFFFF' (white)
   */
  textColor?: string;

  /**
   * Badge size in dp
   * Default: 20
   */
  size?: number;

  /**
   * Text size in sp
   * Default: 10
   */
  textSize?: number;
}

/**
 * App state monitoring configuration
 */
export interface AppStateMonitoring {
  /**
   * Enable app state monitoring
   * Default: true
   */
  enabled?: boolean;

  /**
   * Interval between app state checks in milliseconds
   * Default: 1000
   */
  checkInterval?: number;

  /**
   * Callback when app comes to foreground
   */
  onAppForeground?: () => void;

  /**
   * Callback when app goes to background
   */
  onAppBackground?: () => void;
}

/**
 * Widget configuration
 */
export interface WidgetConfig {
  /**
   * Widget icon as a base64 encoded image string or resource name
   * If not provided, uses the app icon
   */
  icon?: string;

  /**
   * Widget size in dp (density-independent pixels)
   * Default: 56
   */
  size?: number;

  /**
   * Widget shape
   * Default: 'circle'
   */
  shape?: WidgetShape;

  /**
   * Whether the widget can be dragged around the screen
   * Default: true
   */
  draggable?: boolean;

  /**
   * Initial position of the widget
   * If not provided, widget appears at a default position (top-right)
   */
  initialPosition?: WidgetPosition;

  /**
   * Foreground service notification configuration
   */
  notification: NotificationConfig;

  /**
   * Whether to automatically start the widget on device boot
   * Requires RECEIVE_BOOT_COMPLETED permission
   * Default: false
   */
  autoStartOnBoot?: boolean;

  /**
   * Whether to hide the widget when the app is opened
   * Default: true
   */
  hideOnAppOpen?: boolean;

  /**
   * Widget appearance customization
   */
  appearance?: WidgetAppearance;

  /**
   * Dismiss zone configuration
   * Alternative to enableDragToDismiss and dismissZoneHeight
   */
  dismissZone?: DismissZoneConfig;

  /**
   * Animation configuration
   */
  animations?: AnimationConfig;

  /**
   * Position constraints
   */
  constraints?: PositionConstraints;

  /**
   * Badge configuration
   */
  badge?: BadgeConfig;

  /**
   * App state monitoring configuration
   */
  appStateMonitoring?: AppStateMonitoring;

  /**
   * Callback when widget is clicked
   * If not provided, widget will open the app (default behavior)
   */
  onWidgetClick?: (event: WidgetClickEvent) => void;

  /**
   * Callback when widget is long pressed
   */
  onWidgetLongPress?: (event: WidgetLongPressEvent) => void;

  /**
   * Callback when widget is being dragged
   * Useful for tracking widget position or dismiss zone entry
   */
  onWidgetDrag?: (event: WidgetDragEvent) => void;

  /**
   * Callback when widget is shown
   */
  onWidgetShow?: () => void;

  /**
   * Callback when widget is hidden
   */
  onWidgetHide?: () => void;

  /**
   * Callback when widget is dismissed (via drag-to-dismiss)
   */
  onWidgetDismiss?: () => void;

  /**
   * Callback when widget position changes (after drag ends)
   */
  onWidgetPositionChange?: (event: WidgetPositionEvent) => void;

  // Backward compatibility - deprecated in favor of dismissZone
  /**
   * @deprecated Use dismissZone.enabled instead
   */
  enableDragToDismiss?: boolean;

  /**
   * @deprecated Use dismissZone.height instead
   */
  dismissZoneHeight?: number;

  /**
   * @deprecated Use animations.snapToEdge instead
   */
  snapToEdge?: boolean;
}

/**
 * Permission status
 */
export interface PermissionStatus {
  granted: boolean;
}
