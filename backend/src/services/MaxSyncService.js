import { deviceRegistry } from './MaxDeviceRegistry.js';
import { conversationService } from './MaxConversationService.js';
import { memoryService } from './MaxMemoryService.js';
import { taskService } from './MaxTaskService.js';
import { remoteCommandService } from './MaxRemoteCommandService.js';

export class MaxSyncService {
  constructor() {
    this.clients = new Map(); // deviceId -> WebSocket instance
  }

  registerConnection(deviceId, ws) {
    this.clients.set(deviceId, ws);
    deviceRegistry.updateHeartbeat(deviceId, 'online');
    console.log(`[MaxSyncService] Device connected: ${deviceId}`);

    // Send initial ecosystem snapshot
    this.sendToDevice(deviceId, {
      type: 'INITIAL_SNAPSHOT',
      devices: deviceRegistry.getAllDevices(),
      conversations: conversationService.getRecentHistory(30),
      memories: memoryService.getAllMemories(),
      tasks: taskService.getAllTasks()
    });

    this.broadcastDeviceStatus();
  }

  removeConnection(deviceId) {
    this.clients.delete(deviceId);
    deviceRegistry.updateHeartbeat(deviceId, 'offline');
    console.log(`[MaxSyncService] Device disconnected: ${deviceId}`);
    this.broadcastDeviceStatus();
  }

  sendToDevice(deviceId, payload) {
    const ws = this.clients.get(deviceId);
    if (ws && ws.readyState === 1 /* OPEN */) {
      ws.send(JSON.stringify(payload));
      return true;
    }
    return false;
  }

  broadcast(payload, excludeDeviceId = null) {
    const raw = JSON.stringify(payload);
    for (const [devId, ws] of this.clients.entries()) {
      if (devId !== excludeDeviceId && ws.readyState === 1) {
        ws.send(raw);
      }
    }
  }

  broadcastDeviceStatus() {
    this.broadcast({
      type: 'DEVICE_LIST_UPDATED',
      devices: deviceRegistry.getAllDevices()
    });
  }

  handleIncomingMessage(sourceDeviceId, data) {
    try {
      const msg = typeof data === 'string' ? JSON.parse(data) : data;
      const { type } = msg;

      switch (type) {
        case 'HEARTBEAT':
          deviceRegistry.updateHeartbeat(sourceDeviceId, 'online');
          break;

        case 'CHAT_MESSAGE': {
          const added = conversationService.addMessage({
            senderDevice: sourceDeviceId,
            role: msg.role || 'USER',
            content: msg.content,
            language: msg.language
          });
          this.broadcast({ type: 'NEW_CHAT_MESSAGE', message: added });
          break;
        }

        case 'DISPATCH_REMOTE_COMMAND': {
          const { targetDeviceId, action, params, requiresConfirmation } = msg;
          remoteCommandService.dispatchRemoteCommand({
            originDeviceId: sourceDeviceId,
            targetDeviceId,
            action,
            params,
            requiresConfirmation
          }).then((res) => {
            if (res.success && res.envelope) {
              const delivered = this.sendToDevice(targetDeviceId, {
                type: 'EXECUTE_REMOTE_COMMAND',
                envelope: res.envelope
              });
              if (!delivered) {
                remoteCommandService.completeRemoteCommand(res.commandId, {
                  status: 'FAILED',
                  detail: `Target device '${targetDeviceId}' is currently unreachable via WebSocket.`
                });
              }
            }
            this.sendToDevice(sourceDeviceId, {
              type: 'REMOTE_COMMAND_DISPATCH_RESULT',
              result: res
            });
          });
          break;
        }

        case 'COMMAND_EXECUTION_OUTCOME': {
          const { commandId, status, detail, originDeviceId, data } = msg;
          const completed = remoteCommandService.completeRemoteCommand(commandId, {
            status,
            detail,
            originDeviceId,
            targetDeviceId: sourceDeviceId,
            data
          });

          // Inform origin device of outcome
          if (originDeviceId) {
            this.sendToDevice(originDeviceId, {
              type: 'REMOTE_COMMAND_COMPLETED',
              completed
            });
          }
          this.broadcast({ type: 'AUDIT_LOG_UPDATED', logs: remoteCommandService.getAuditLogs(20) });
          break;
        }

        case 'CREATE_TASK': {
          const task = taskService.createTask(msg);
          this.broadcast({ type: 'TASK_CREATED', task });
          break;
        }

        case 'TOGGLE_TASK': {
          const toggled = taskService.toggleTaskCompletion(msg.taskId);
          this.broadcast({ type: 'TASK_UPDATED', task: toggled });
          break;
        }

        case 'SAVE_MEMORY': {
          const mem = memoryService.saveMemory(msg.key, msg.content, sourceDeviceId);
          this.broadcast({ type: 'MEMORY_UPDATED', memory: mem });
          break;
        }

        default:
          console.log(`[MaxSyncService] Unhandled message type '${type}' from ${sourceDeviceId}`);
      }
    } catch (e) {
      console.error('[MaxSyncService] Error handling message:', e.message);
    }
  }
}

export const syncService = new MaxSyncService();
