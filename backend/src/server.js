import express from 'express';
import http from 'http';
import { WebSocketServer } from 'ws';
import cors from 'cors';

import { authService } from './services/MaxAuthService.js';
import { deviceRegistry } from './services/MaxDeviceRegistry.js';
import { conversationService } from './services/MaxConversationService.js';
import { memoryService } from './services/MaxMemoryService.js';
import { taskService } from './services/MaxTaskService.js';
import { remoteCommandService } from './services/MaxRemoteCommandService.js';
import { notificationService } from './services/MaxNotificationService.js';
import { syncService } from './services/MaxSyncService.js';
import { updateService } from './services/MaxUpdateService.js';
import { aiOrchestrator } from './services/MaxAIOrchestrator.js';

const app = express();
const PORT = process.env.PORT || 8085;

app.use(cors());
app.use(express.json());

// 1. Health & Ecosystem Telemetry
app.get('/health', (req, res) => {
  const devices = deviceRegistry.getAllDevices();
  const onlineCount = devices.filter(d => d.status === 'online').length;
  res.json({
    status: 'ONLINE',
    ecosystem: 'MAX Universal Personal AI Assistant',
    version: '2.3.0',
    totalDevices: devices.length,
    onlineDevices: onlineCount,
    timestamp: Date.now()
  });
});

// 2. Authentication & Pairing APIs
app.post('/api/auth/pin', (req, res) => {
  const { pin } = req.body;
  const valid = authService.verifyUserPin(pin);
  res.json({ valid, message: valid ? 'Identity verified, Sir.' : 'Invalid PIN, Sir.' });
});

app.post('/api/auth/pair/start', (req, res) => {
  const { deviceName, platform } = req.body;
  const pairingData = authService.generatePairingToken(deviceName || 'New Device', platform || 'generic');
  res.json({ success: true, pairingData });
});

app.post('/api/auth/pair/confirm', (req, res) => {
  const { codeOrToken } = req.body;
  const result = authService.completePairing(codeOrToken);
  res.json(result);
});

// 3. Device Registry APIs
app.get('/api/devices', (req, res) => {
  res.json(deviceRegistry.getAllDevices());
});

app.post('/api/devices/register', (req, res) => {
  const dev = deviceRegistry.registerOrUpdateDevice(req.body);
  syncService.broadcastDeviceStatus();
  res.json({ success: true, device: dev });
});

app.post('/api/devices/:id/revoke', (req, res) => {
  const success = authService.revokeDevice(req.params.id);
  syncService.broadcastDeviceStatus();
  res.json({ success, message: success ? 'Device access revoked, Sir.' : 'Device not found.' });
});

// 4. Conversation Synchronization APIs
app.get('/api/conversations', (req, res) => {
  const limit = parseInt(req.query.limit || '50', 10);
  res.json(conversationService.getRecentHistory(limit));
});

app.post('/api/conversations', async (req, res) => {
  const { senderDevice, role, content, language, triggerAi } = req.body;
  const userMsg = conversationService.addMessage({ senderDevice, role, content, language });
  syncService.broadcast({ type: 'NEW_CHAT_MESSAGE', message: userMsg });

  if (triggerAi) {
    const history = conversationService.getRecentHistory(10);
    const aiText = await aiOrchestrator.processQuery({
      prompt: content,
      history,
      language: language || 'ENGLISH'
    });
    const aiMsg = conversationService.addMessage({
      senderDevice: 'cloud_orchestrator',
      role: 'MAX',
      content: aiText,
      language: language || 'ENGLISH'
    });
    syncService.broadcast({ type: 'NEW_CHAT_MESSAGE', message: aiMsg });
    return res.json({ userMessage: userMsg, aiMessage: aiMsg });
  }

  res.json({ userMessage: userMsg });
});

// 5. Shared Memory APIs
app.get('/api/memories', (req, res) => {
  res.json(memoryService.getAllMemories());
});

app.post('/api/memories', (req, res) => {
  const { key, content, sourceDevice } = req.body;
  const saved = memoryService.saveMemory(key, content, sourceDevice);
  syncService.broadcast({ type: 'MEMORY_UPDATED', memory: saved });
  res.json({ success: true, memory: saved });
});

// 6. Universal Task & Reminders APIs
app.get('/api/tasks', (req, res) => {
  res.json(taskService.getAllTasks());
});

app.post('/api/tasks', (req, res) => {
  const task = taskService.createTask(req.body);
  syncService.broadcast({ type: 'TASK_CREATED', task });
  res.json({ success: true, task });
});

app.post('/api/tasks/:id/toggle', (req, res) => {
  const toggled = taskService.toggleTaskCompletion(req.params.id);
  syncService.broadcast({ type: 'TASK_UPDATED', task: toggled });
  res.json({ success: true, task: toggled });
});

// 7. Remote Command Routing APIs
app.post('/api/commands/dispatch', async (req, res) => {
  const result = await remoteCommandService.dispatchRemoteCommand(req.body);
  if (result.success && result.envelope) {
    syncService.sendToDevice(req.body.targetDeviceId, {
      type: 'EXECUTE_REMOTE_COMMAND',
      envelope: result.envelope
    });
  }
  res.json(result);
});

app.post('/api/commands/complete', (req, res) => {
  const { commandId, ...resultData } = req.body;
  const completed = remoteCommandService.completeRemoteCommand(commandId, resultData);
  syncService.broadcast({ type: 'AUDIT_LOG_UPDATED', logs: remoteCommandService.getAuditLogs(20) });
  res.json({ success: true, completed });
});

app.get('/api/commands/audit', (req, res) => {
  const limit = parseInt(req.query.limit || '50', 10);
  res.json(remoteCommandService.getAuditLogs(limit));
});

// 8. Notifications APIs
app.get('/api/notifications', (req, res) => {
  res.json(notificationService.getAllNotifications());
});

app.post('/api/notifications', (req, res) => {
  const notif = notificationService.sendNotification(req.body);
  syncService.broadcast({ type: 'NEW_NOTIFICATION', notification: notif });
  res.json({ success: true, notification: notif });
});

// 9. Update Management APIs
app.get('/api/updates/:platform', (req, res) => {
  const { platform } = req.params;
  const currentVersion = req.query.version || '2.0.0';
  res.json(updateService.checkUpdate(platform, currentVersion));
});

// 10. Fish Audio Neural JARVIS TTS Endpoint
const FISH_AUDIO_API_KEY = process.env.FISH_AUDIO_API_KEY || 'sk-fish-b06X3QPdnaWD4AV3MO9Klj0DtvYuiWnkoil-Cdv6PEM';
const DEFAULT_JARVIS_VOICE = '14129c3e320149449d6bada6862f7338';

app.post('/api/voice/fish-tts', async (req, res) => {
  const { text, voiceId } = req.body;
  if (!text) return res.status(400).json({ error: 'Text is required' });

  try {
    const fetchRes = await fetch('https://api.fish.audio/v1/tts', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${FISH_AUDIO_API_KEY}`,
        'Content-Type': 'application/json',
        'model': 's2.1-pro'
      },
      body: JSON.stringify({
        text,
        reference_id: voiceId || DEFAULT_JARVIS_VOICE,
        format: 'mp3',
        latency: 'low'
      })
    });

    if (!fetchRes.ok) {
      const err = await fetchRes.text();
      return res.status(fetchRes.status).json({ error: err });
    }

    const buffer = await fetchRes.arrayBuffer();
    res.setHeader('Content-Type', 'audio/mpeg');
    res.send(Buffer.from(buffer));
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Create HTTP server
const server = http.createServer(app);

// WebSocket Real-Time Synchronization Engine
const wss = new WebSocketServer({ server, path: '/ws' });

wss.on('connection', (ws, req) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  const deviceId = url.searchParams.get('deviceId') || `client_${Date.now().toString(36)}`;

  syncService.registerConnection(deviceId, ws);

  ws.on('message', (message) => {
    syncService.handleIncomingMessage(deviceId, message.toString());
  });

  ws.on('close', () => {
    syncService.removeConnection(deviceId);
  });

  ws.on('error', (err) => {
    console.error(`[WebSocket] Error for device ${deviceId}:`, err.message);
  });
});

server.listen(PORT, () => {
  console.log(`=======================================================`);
  console.log(`  MAX UNIVERSAL AI ECOSYSTEM SERVER ONLINE`);
  console.log(`  Port: ${PORT}`);
  console.log(`  REST API: http://localhost:${PORT}`);
  console.log(`  WebSocket: ws://localhost:${PORT}/ws?deviceId=...`);
  console.log(`=======================================================`);
});
