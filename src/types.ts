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
}

/**
 * Permission status
 */
export interface PermissionStatus {
  granted: boolean;
}
