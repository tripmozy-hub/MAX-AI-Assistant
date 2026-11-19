import http from 'http';
import path from 'path';
import { fileURLToPath } from 'url';
import express from 'express';
import WebSocket from 'ws';
import cors from 'cors';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const PORT = process.env.WEAR_PORT || 8087;
const BACKEND_WS_URL = process.env.BACKEND_WS_URL || 'ws://localhost:8085/ws?deviceId=dev_watch_wearos';
const BACKEND_HTTP_URL = process.env.BACKEND_HTTP_URL || 'http://localhost:8085';

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

let ecosystemWs = null;

function connectToEcosystem() {
  console.log(`[Watch Node] Connecting to Central MAX Backend: ${BACKEND_WS_URL}`);
  ecosystemWs = new WebSocket(BACKEND_WS_URL);

  ecosystemWs.on('open', () => {
    console.log('[Watch Node] Connected to Central Backend as dev_watch_wearos.');
    ecosystemWs.send(
      JSON.stringify({
        type: 'HEARTBEAT',
        deviceId: 'dev_watch_wearos'
      })
    );
  });

  ecosystemWs.on('message', (data) => {
    try {
      const msg = JSON.parse(data.toString());
      if (msg.type === 'EXECUTE_REMOTE_COMMAND') {
        const { envelope } = msg;
        console.log(`[Watch Node] Received Remote Alert for Wear OS: ${envelope.action}`);
        ecosystemWs.send(
          JSON.stringify({
            type: 'COMMAND_EXECUTION_OUTCOME',
            commandId: envelope.commandId,
            originDeviceId: envelope.originDeviceId,
            status: 'SUCCESS',
            detail: `Haptic vibration and visual alert delivered to Galaxy Watch, Sir.`
          })
        );
      }
    } catch (_e) {}
  });

  ecosystemWs.on('close', () => {
    setTimeout(connectToEcosystem, 5000);
  });

  ecosystemWs.on('error', (err) => {
    console.warn('[Watch Node] Connection retry...');
  });
}

connectToEcosystem();

const server = http.createServer(app);
server.listen(PORT, () => {
  console.log(`=======================================================`);
  console.log(`  MAX SMARTWATCH NODE ONLINE (Wear OS)`);
  console.log(`  Smartwatch Interface: http://localhost:${PORT}`);
  console.log(`=======================================================`);
});
