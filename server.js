const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 8080;
const APK_PATH = path.join(__dirname, 'MAX.apk');
const PREVIEW_PATH = path.join(__dirname, 'preview.html');

const server = http.createServer((req, res) => {
    if (req.url === '/MAX.apk' || req.url === '/download') {
        if (!fs.existsSync(APK_PATH)) {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            return res.end('MAX.apk not found.');
        }
        const stat = fs.statSync(APK_PATH);
        res.writeHead(200, {
            'Content-Type': 'application/vnd.android.package-archive',
            'Content-Length': stat.size,
            'Content-Disposition': 'attachment; filename="MAX.apk"'
        });
        const readStream = fs.createReadStream(APK_PATH);
        readStream.pipe(res);
    } else if (req.url === '/preview' || req.url === '/preview.html' || req.url === '/') {
        if (fs.existsSync(PREVIEW_PATH)) {
            res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
            const stream = fs.createReadStream(PREVIEW_PATH);
            stream.pipe(res);
        } else {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            res.end('preview.html not found.');
        }
    } else {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(`
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MAX // Voice Assistant Installer</title>
    <style>
        body {
            background: #06090F;
            color: #00F0FF;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, monospace;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            min-height: 90vh;
            margin: 0;
            padding: 20px;
            text-align: center;
        }
        .card {
            background: #0A0F1D;
            border: 2px solid #00F0FF;
            box-shadow: 0 0 25px rgba(0, 240, 255, 0.35);
            border-radius: 16px;
            padding: 30px 24px;
            max-width: 360px;
            width: 100%;
        }
        h1 {
            font-size: 26px;
            margin-bottom: 6px;
            letter-spacing: 2px;
        }
        .subtitle {
            color: #0088CC;
            font-size: 12px;
            margin-bottom: 24px;
            letter-spacing: 1px;
        }
        .btn {
            display: block;
            background: linear-gradient(135deg, #0077FE, #00F0FF);
            color: #06090F;
            font-size: 18px;
            font-weight: bold;
            text-decoration: none;
            padding: 16px 20px;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0, 240, 255, 0.4);
            margin: 20px 0 10px 0;
            letter-spacing: 1px;
        }
        .info {
            color: #88AACC;
            font-size: 13px;
            line-height: 1.6;
            margin-top: 15px;
        }
    </style>
</head>
<body>
    <div class="card">
        <h1>MAX // CYBER-AI</h1>
        <div class="subtitle">SAMSUNG GALAXY S25 ULTRA</div>
        <p class="info">Ready to install native voice assistant on your device.</p>
        <a class="btn" href="/MAX.apk">DOWNLOAD MAX.APK</a>
        <div class="info">Tap to download & install directly.</div>
    </div>
</body>
</html>
        `);
    }
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`MAX installer server running on http://0.0.0.0:${PORT}`);
});
