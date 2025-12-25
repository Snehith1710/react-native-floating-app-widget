import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';
import type { WidgetConfig } from './types';

/**
 * Native module interface for FloatingAppWidget
 * This is compatible with the New Architecture (TurboModules)
 */
export interface Spec extends TurboModule {
  /**
   * Initialize the floating widget with configuration
   */
  init(config: WidgetConfig): Promise<void>;

  /**
   * Start the floating widget
   * The widget will appear when the app goes to background
   */
  start(): Promise<void>;

  /**
   * Stop the floating widget and remove it from screen
   */
  stop(): Promise<void>;

  /**
   * Update the widget configuration
   */
  update(config: WidgetConfig): Promise<void>;

  /**
   * Check if SYSTEM_ALERT_WINDOW permission is granted
   */
  hasPermission(): Promise<boolean>;

  /**
   * Request SYSTEM_ALERT_WINDOW permission
   * Opens the system settings screen where user can grant the permission
   */
  requestPermission(): Promise<void>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('FloatingAppWidget');
