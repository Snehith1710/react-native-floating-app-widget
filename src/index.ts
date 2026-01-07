import { NativeModules, Platform, NativeEventEmitter } from 'react-native';
import type {
  WidgetConfig,
  PermissionStatus,
  WidgetClickEvent,
  WidgetLongPressEvent,
  WidgetDragEvent,
  WidgetPositionEvent,
} from './types';

// Try to get the TurboModule first, fall back to legacy NativeModules
let NativeFloatingAppWidget: any;

try {
  NativeFloatingAppWidget = require('./NativeFloatingAppWidget').default;
} catch {
  // Fallback for old architecture
  NativeFloatingAppWidget = NativeModules.FloatingAppWidget;
}

/**
 * FloatingAppWidget class
 * Provides a high-level API for managing floating widgets on Android
 */
class FloatingAppWidget {
  private initialized = false;
  private currentConfig: WidgetConfig | null = null;
  private eventEmitter: NativeEventEmitter | null = null;
  private clickListener: any = null;
  private longPressListener: any = null;
  private dragListener: any = null;
  private showListener: any = null;
  private hideListener: any = null;
  private dismissListener: any = null;
  private positionChangeListener: any = null;
  private appForegroundListener: any = null;
  private appBackgroundListener: any = null;

  /**
   * Initialize the floating widget with configuration
   * This must be called before start()
   *
   * @param config - Widget configuration
   * @throws Error if platform is not Android
   * @throws Error if native module is not available
   */
  async init(config: WidgetConfig): Promise<void> {
    this.checkPlatform();
    this.checkNativeModule();

    // Setup event emitter
    if (!this.eventEmitter) {
      this.eventEmitter = new NativeEventEmitter(NativeFloatingAppWidget);
    }

    // Remove existing listeners
    this.removeEventListeners();

    // Setup click listener if callback provided
    if (config.onWidgetClick) {
      this.clickListener = this.eventEmitter.addListener(
        'onWidgetClick',
        (event: WidgetClickEvent) => {
          config.onWidgetClick?.(event);
        }
      );
    }

    // Setup long press listener if callback provided
    if (config.onWidgetLongPress) {
      this.longPressListener = this.eventEmitter.addListener(
        'onWidgetLongPress',
        (event: WidgetLongPressEvent) => {
          config.onWidgetLongPress?.(event);
        }
      );
    }

    // Setup drag listener if callback provided
    if (config.onWidgetDrag) {
      this.dragListener = this.eventEmitter.addListener(
        'onWidgetDrag',
        (event: WidgetDragEvent) => {
          config.onWidgetDrag?.(event);
        }
      );
    }

    // Setup show listener if callback provided
    if (config.onWidgetShow) {
      this.showListener = this.eventEmitter.addListener(
        'onWidgetShow',
        () => {
          config.onWidgetShow?.();
        }
      );
    }

    // Setup hide listener if callback provided
    if (config.onWidgetHide) {
      this.hideListener = this.eventEmitter.addListener(
        'onWidgetHide',
        () => {
          config.onWidgetHide?.();
        }
      );
    }

    // Setup dismiss listener if callback provided
    if (config.onWidgetDismiss) {
      this.dismissListener = this.eventEmitter.addListener(
        'onWidgetDismiss',
        () => {
          config.onWidgetDismiss?.();
        }
      );
    }

    // Setup position change listener if callback provided
    if (config.onWidgetPositionChange) {
      this.positionChangeListener = this.eventEmitter.addListener(
        'onWidgetPositionChange',
        (event: WidgetPositionEvent) => {
          config.onWidgetPositionChange?.(event);
        }
      );
    }

    // Setup app state listeners if provided
    if (config.appStateMonitoring?.onAppForeground) {
      this.appForegroundListener = this.eventEmitter.addListener(
        'onAppForeground',
        () => {
          config.appStateMonitoring?.onAppForeground?.();
        }
      );
    }

    if (config.appStateMonitoring?.onAppBackground) {
      this.appBackgroundListener = this.eventEmitter.addListener(
        'onAppBackground',
        () => {
          config.appStateMonitoring?.onAppBackground?.();
        }
      );
    }

    // Prepare config for native side, adding callback flags
    const nativeConfig = {
      ...config,
      hasClickCallback: !!config.onWidgetClick,
      hasLongPressCallback: !!config.onWidgetLongPress,
      hasDragCallback: !!config.onWidgetDrag,
      hasShowCallback: !!config.onWidgetShow,
      hasHideCallback: !!config.onWidgetHide,
      hasDismissCallback: !!config.onWidgetDismiss,
      hasPositionChangeCallback: !!config.onWidgetPositionChange,
      hasAppStateCallbacks: !!(config.appStateMonitoring?.onAppForeground || config.appStateMonitoring?.onAppBackground),
    };

    await NativeFloatingAppWidget.init(nativeConfig);
    this.currentConfig = config;
    this.initialized = true;
  }

  /**
   * Remove all event listeners
   */
  private removeEventListeners(): void {
    if (this.clickListener) {
      this.clickListener.remove();
      this.clickListener = null;
    }
    if (this.longPressListener) {
      this.longPressListener.remove();
      this.longPressListener = null;
    }
    if (this.dragListener) {
      this.dragListener.remove();
      this.dragListener = null;
    }
    if (this.showListener) {
      this.showListener.remove();
      this.showListener = null;
    }
    if (this.hideListener) {
      this.hideListener.remove();
      this.hideListener = null;
    }
    if (this.dismissListener) {
      this.dismissListener.remove();
      this.dismissListener = null;
    }
    if (this.positionChangeListener) {
      this.positionChangeListener.remove();
      this.positionChangeListener = null;
    }
    if (this.appForegroundListener) {
      this.appForegroundListener.remove();
      this.appForegroundListener = null;
    }
    if (this.appBackgroundListener) {
      this.appBackgroundListener.remove();
      this.appBackgroundListener = null;
    }
  }

  /**
   * Start the floating widget
   * The widget will appear when the app goes to background
   *
   * @throws Error if not initialized
   * @throws Error if permission is not granted
   */
  async start(): Promise<void> {
    this.checkPlatform();
    this.checkNativeModule();

    if (!this.initialized) {
      throw new Error('FloatingAppWidget: Must call init() before start()');
    }

    const hasPermission = await this.hasPermission();
    if (!hasPermission) {
      throw new Error(
        'FloatingAppWidget: SYSTEM_ALERT_WINDOW permission not granted. Call requestPermission() first.'
      );
    }

    await NativeFloatingAppWidget.start();
  }

  /**
   * Stop the floating widget and remove it from screen
   */
  async stop(): Promise<void> {
    this.checkPlatform();
    this.checkNativeModule();

    // Remove event listeners
    this.removeEventListeners();

    await NativeFloatingAppWidget.stop();
  }

  /**
   * Update the widget configuration
   * Widget must be initialized first
   *
   * @param config - New widget configuration
   */
  async update(config: WidgetConfig): Promise<void> {
    this.checkPlatform();
    this.checkNativeModule();

    if (!this.initialized) {
      throw new Error('FloatingAppWidget: Must call init() before update()');
    }

    await NativeFloatingAppWidget.update(config);
    this.currentConfig = config;
  }

  /**
   * Check if SYSTEM_ALERT_WINDOW permission is granted
   *
   * @returns Promise resolving to true if permission is granted
   */
  async hasPermission(): Promise<boolean> {
    this.checkPlatform();
    this.checkNativeModule();

    return await NativeFloatingAppWidget.hasPermission();
  }

  /**
   * Request SYSTEM_ALERT_WINDOW permission
   * Opens the system settings screen where user can grant the permission
   *
   * Note: You should check permission status after user returns from settings
   * using hasPermission()
   */
  async requestPermission(): Promise<void> {
    this.checkPlatform();
    this.checkNativeModule();

    await NativeFloatingAppWidget.requestPermission();
  }

  /**
   * Get permission status with detailed information
   *
   * @returns Permission status object
   */
  async getPermissionStatus(): Promise<PermissionStatus> {
    const granted = await this.hasPermission();
    return { granted };
  }

  /**
   * Get current widget configuration
   *
   * @returns Current configuration or null if not initialized
   */
  getConfig(): WidgetConfig | null {
    return this.currentConfig;
  }

  /**
   * Check if widget is initialized
   */
  isInitialized(): boolean {
    return this.initialized;
  }

  private checkPlatform(): void {
    if (Platform.OS !== 'android') {
      throw new Error('FloatingAppWidget: Only Android platform is supported');
    }
  }

  private checkNativeModule(): void {
    if (!NativeFloatingAppWidget) {
      throw new Error(
        'FloatingAppWidget: Native module not found. Make sure the library is linked correctly.'
      );
    }
  }
}

// Export singleton instance
export default new FloatingAppWidget();

// Export types
export * from './types';
