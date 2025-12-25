import { NativeModules, Platform } from 'react-native';
import type { WidgetConfig, PermissionStatus } from './types';

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

    await NativeFloatingAppWidget.init(config);
    this.currentConfig = config;
    this.initialized = true;
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
