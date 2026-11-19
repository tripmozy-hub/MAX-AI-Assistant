import { store } from '../db/store.js';

export class MaxDeviceRegistry {
  getAllDevices() {
    return store.get('devices');
  }

  getDeviceById(deviceId) {
    const devices = store.get('devices');
    return devices.find(d => d.id === deviceId);
  }

  updateHeartbeat(deviceId, currentStatus = 'online') {
    store.update('devices', (devices) =>
      devices.map((d) => {
        if (d.id === deviceId) {
          return { ...d, lastSeen: Date.now(), status: currentStatus };
        }
        return d;
      })
    );
  }

  registerOrUpdateDevice(deviceData) {
    const { id, name, platform, capabilities } = deviceData;
    store.update('devices', (devices) => {
      const existing = devices.find(d => d.id === id);
      if (existing) {
        return devices.map(d => (d.id === id ? { ...d, ...deviceData, lastSeen: Date.now(), status: 'online' } : d));
      } else {
        const newDev = {
          id: id || `dev_${platform}_${Date.now().toString(36)}`,
          name: name || `MAX Device (${platform})`,
          platform: platform || 'unknown',
          status: 'online',
          lastSeen: Date.now(),
          isAuthorized: true,
          capabilities: capabilities || []
        };
        return [...devices, newDev];
      }
    });
    return this.getDeviceById(id);
  }

  checkDeviceCapability(deviceId, requiredCapability) {
    const device = this.getDeviceById(deviceId);
    if (!device) return { allowed: false, reason: 'Target device not found in ecosystem directory, Sir.' };
    if (!device.isAuthorized) return { allowed: false, reason: 'Device authorization has been revoked, Sir.' };
    if (device.status !== 'online') return { allowed: false, reason: `Target device '${device.name}' is currently offline, Sir.` };
    if (!device.capabilities.includes(requiredCapability)) {
      return {
        allowed: false,
        reason: `Target device '${device.name}' does not support '${requiredCapability}' capability, Sir.`
      };
    }
    return { allowed: true, device };
  }
}

export const deviceRegistry = new MaxDeviceRegistry();
