import crypto from 'crypto';
import { store } from '../db/store.js';

export class MaxAuthService {
  constructor() {
    this.pairingTokens = new Map(); // token -> { deviceName, platform, expiresAt }
  }

  verifyUserPin(pin) {
    const user = store.get('users')[0];
    return user && user.pin === pin;
  }

  generatePairingToken(deviceName, platform) {
    const token = crypto.randomBytes(4).toString('hex').toUpperCase(); // e.g. "A8F4D2B1"
    const sixDigitCode = Math.floor(100000 + Math.random() * 900000).toString();
    const pairingData = {
      token,
      code: sixDigitCode,
      deviceName,
      platform,
      expiresAt: Date.now() + 10 * 60 * 1000 // 10 minutes
    };
    this.pairingTokens.set(token, pairingData);
    this.pairingTokens.set(sixDigitCode, pairingData);
    return pairingData;
  }

  completePairing(codeOrToken) {
    const pairing = this.pairingTokens.get(codeOrToken);
    if (!pairing || pairing.expiresAt < Date.now()) {
      return { success: false, message: 'Invalid or expired pairing code, Sir.' };
    }

    const deviceId = `dev_${pairing.platform}_${crypto.randomBytes(3).toString('hex')}`;
    const newDevice = {
      id: deviceId,
      name: pairing.deviceName,
      platform: pairing.platform,
      status: 'online',
      lastSeen: Date.now(),
      isAuthorized: true,
      capabilities: this.getDefaultCapabilities(pairing.platform)
    };

    store.update('devices', (devices) => [...devices.filter(d => d.id !== deviceId), newDevice]);
    this.pairingTokens.delete(pairing.token);
    this.pairingTokens.delete(pairing.code);

    const authToken = crypto.randomBytes(24).toString('hex');
    return {
      success: true,
      deviceId,
      device: newDevice,
      authToken,
      message: `Device '${pairing.deviceName}' paired and authorized successfully, Sir.`
    };
  }

  getDefaultCapabilities(platform) {
    switch (platform) {
      case 'android':
        return ['screen_vision', 'voice_engine', 'app_control', 'phone_call', 'sms', 'whatsapp', 'flashlight', 'notifications'];
      case 'windows':
      case 'macos':
      case 'linux':
        return ['launch_app', 'file_search', 'create_folder', 'read_document', 'system_health', 'browser_control', 'holographic_workspace'];
      case 'wearos':
      case 'watchos':
        return ['quick_voice', 'mini_core', 'haptic_alert', 'reminder_notify'];
      default:
        return ['chat_sync'];
    }
  }

  revokeDevice(deviceId) {
    let found = false;
    store.update('devices', (devices) =>
      devices.map((d) => {
        if (d.id === deviceId) {
          found = true;
          return { ...d, isAuthorized: false, status: 'revoked' };
        }
        return d;
      })
    );
    return found;
  }
}

export const authService = new MaxAuthService();
