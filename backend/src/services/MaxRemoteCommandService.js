import { store } from '../db/store.js';
import { deviceRegistry } from './MaxDeviceRegistry.js';

export class MaxRemoteCommandService {
  constructor() {
    this.pendingCommands = new Map(); // commandId -> { resolve, reject, timer }
  }

  async dispatchRemoteCommand({ originDeviceId, targetDeviceId, action, params = {}, requiresConfirmation = false }) {
    const timestamp = Date.now();
    const commandId = `cmd_${timestamp}_${Math.random().toString(36).substr(2, 4)}`;

    // 1. Check capability & status of target device
    const capCheck = deviceRegistry.checkDeviceCapability(targetDeviceId, this.mapActionToCapability(action));
    if (!capCheck.allowed) {
      this.logAudit({
        commandId,
        originDeviceId,
        targetDeviceId,
        action,
        status: 'DENIED',
        detail: capCheck.reason
      });
      return {
        success: false,
        commandId,
        message: capCheck.reason
      };
    }

    // 2. Build command envelope
    const envelope = {
      commandId,
      originDeviceId,
      targetDeviceId,
      action,
      params,
      requiresConfirmation,
      createdAt: timestamp,
      status: 'PENDING'
    };

    // 3. Log PENDING audit
    this.logAudit({
      commandId,
      originDeviceId,
      targetDeviceId,
      action,
      status: 'DISPATCHED',
      detail: `Dispatched '${action}' to target device '${capCheck.device.name}'`
    });

    return {
      success: true,
      commandId,
      envelope,
      targetDevice: capCheck.device,
      message: `Command dispatched to ${capCheck.device.name}, Sir.`
    };
  }

  completeRemoteCommand(commandId, result) {
    const { status, detail, data } = result;
    this.logAudit({
      commandId,
      originDeviceId: result.originDeviceId || 'unknown',
      targetDeviceId: result.targetDeviceId || 'unknown',
      action: result.action || 'execute',
      status: status || 'SUCCESS',
      detail: detail || 'Execution completed'
    });
    return { commandId, status, detail, data };
  }

  logAudit({ commandId, originDeviceId, targetDeviceId, action, status, detail }) {
    const entry = {
      id: `audit_${Date.now()}_${Math.random().toString(36).substr(2, 4)}`,
      commandId,
      originDeviceId,
      targetDeviceId,
      action,
      status,
      detail,
      timestamp: Date.now()
    };

    store.update('auditLogs', (logs) => {
      const updated = [entry, ...(logs || [])];
      return updated.slice(0, 150);
    });
  }

  getAuditLogs(limit = 50) {
    return (store.get('auditLogs') || []).slice(0, limit);
  }

  mapActionToCapability(action) {
    switch (action) {
      case 'open_app':
      case 'launch_app':
        return 'launch_app';
      case 'file_search':
        return 'file_search';
      case 'system_health':
        return 'system_health';
      case 'create_folder':
        return 'create_folder';
      case 'flashlight':
      case 'toggle_flashlight':
        return 'flashlight';
      case 'screen_vision':
        return 'screen_vision';
      case 'phone_call':
        return 'phone_call';
      case 'sms':
        return 'sms';
      case 'haptic_alert':
        return 'haptic_alert';
      default:
        return 'app_control';
    }
  }
}

export const remoteCommandService = new MaxRemoteCommandService();
