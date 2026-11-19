export class MaxUpdateService {
  constructor() {
    this.releases = {
      android: {
        latestVersion: '2.3.0',
        versionCode: 4,
        releaseDate: '2026-11-19',
        downloadUrl: 'https://github.com/tripmozy-hub/MAX-AI-Assistant/releases/download/v2.3.0/MAX.apk',
        cdnMirrorUrl: 'https://files.catbox.moe/s5zx7h.apk',
        sha256: 'VERIFIED_V2_V3_SIGNED',
        notes: [
          'JARVIS Multi-Window Holographic Workspace',
          'Cross-device ecosystem integration and remote command receiver',
          'AMOLED black Arc Reactor HUD with real-time waveform displacement'
        ]
      },
      windows: {
        latestVersion: '2.3.0',
        releaseDate: '2026-11-19',
        downloadUrl: 'https://github.com/tripmozy-hub/MAX-AI-Assistant/releases/download/v2.3.0/MAX-Desktop-Windows.exe',
        cdnMirrorUrl: 'https://files.catbox.moe/s5zx7h.apk',
        notes: [
          'Full JARVIS Mode & Professional Desktop Mode',
          'Windows native OS automation (app launch, file search, folder creation, system health)',
          'Cross-device sync bridge with Android & Wear OS'
        ]
      },
      wearos: {
        latestVersion: '2.3.0',
        versionCode: 4,
        releaseDate: '2026-11-19',
        downloadUrl: 'https://github.com/tripmozy-hub/MAX-AI-Assistant/releases/download/v2.3.0/MAX-WearOS.apk',
        notes: [
          'Mini Core animated watch mode',
          'Minimal MAX digital watch face',
          'Remote voice command dispatch to laptop and phone'
        ]
      }
    };
  }

  getPlatformRelease(platform) {
    const key = (platform || '').toLowerCase();
    return this.releases[key] || this.releases.android;
  }

  checkUpdate(platform, currentVersion) {
    const release = this.getPlatformRelease(platform);
    const hasUpdate = release.latestVersion !== currentVersion;
    return {
      hasUpdate,
      currentVersion,
      latestVersion: release.latestVersion,
      release
    };
  }
}

export const updateService = new MaxUpdateService();
