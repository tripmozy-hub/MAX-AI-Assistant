import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const DATA_DIR = path.resolve(__dirname, '../../data');
const DATA_FILE = path.join(DATA_DIR, 'ecosystem-state.json');

// Ensure data directory exists
if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

class Store {
  constructor() {
    this.state = {
      users: [
        {
          id: 'user_master',
          username: 'Sir',
          pin: '1212',
          createdAt: Date.now()
        }
      ],
      devices: [
        {
          id: 'dev_phone_s25',
          name: 'Samsung Galaxy S25 Ultra',
          platform: 'android',
          status: 'online',
          lastSeen: Date.now(),
          isAuthorized: true,
          capabilities: [
            'screen_vision',
            'voice_engine',
            'app_control',
            'phone_call',
            'sms',
            'whatsapp',
            'flashlight',
            'notifications'
          ]
        },
        {
          id: 'dev_laptop_win',
          name: 'JARVIS Workstation Windows',
          platform: 'windows',
          status: 'online',
          lastSeen: Date.now(),
          isAuthorized: true,
          capabilities: [
            'launch_app',
            'file_search',
            'create_folder',
            'read_document',
            'system_health',
            'browser_control',
            'holographic_workspace'
          ]
        },
        {
          id: 'dev_watch_wearos',
          name: 'MAX Galaxy Watch',
          platform: 'wearos',
          status: 'online',
          lastSeen: Date.now(),
          isAuthorized: true,
          capabilities: [
            'quick_voice',
            'mini_core',
            'haptic_alert',
            'reminder_notify'
          ]
        }
      ],
      conversations: [
        {
          id: 'msg_welcome',
          senderDevice: 'system',
          role: 'MAX',
          content: 'MAX Universal AI Ecosystem initialized. All personal nodes connected and synchronized, Sir.',
          language: 'ENGLISH',
          timestamp: Date.now()
        }
      ],
      memories: [
        {
          id: 'mem_1',
          key: 'user_title',
          content: 'Always address the user with the highest respect as Sir.',
          sourceDevice: 'system',
          updatedAt: Date.now()
        },
        {
          id: 'mem_2',
          key: 'preferred_mode',
          content: 'High preference for cinematic JARVIS 3D HUD interface and AMOLED black aesthetics.',
          sourceDevice: 'dev_phone_s25',
          updatedAt: Date.now()
        }
      ],
      tasks: [
        {
          id: 'task_1',
          title: 'Finish website deployment tonight',
          completed: false,
          dueAt: Date.now() + 6 * 3600 * 1000,
          originDevice: 'dev_watch_wearos',
          assignedDevice: 'dev_laptop_win',
          createdAt: Date.now()
        }
      ],
      auditLogs: [],
      notifications: []
    };

    this.load();
  }

  load() {
    try {
      if (fs.existsSync(DATA_FILE)) {
        const raw = fs.readFileSync(DATA_FILE, 'utf-8');
        const parsed = JSON.parse(raw);
        this.state = { ...this.state, ...parsed };
      } else {
        this.save();
      }
    } catch (e) {
      console.warn('[Store] Could not load state, using defaults:', e.message);
    }
  }

  save() {
    try {
      const tempFile = `${DATA_FILE}.tmp`;
      fs.writeFileSync(tempFile, JSON.stringify(this.state, null, 2), 'utf-8');
      fs.renameSync(tempFile, DATA_FILE);
    } catch (e) {
      console.error('[Store] Save state error:', e.message);
    }
  }

  get(key) {
    return this.state[key];
  }

  set(key, value) {
    this.state[key] = value;
    this.save();
  }

  update(key, updater) {
    this.state[key] = updater(this.state[key]);
    this.save();
  }
}

export const store = new Store();
