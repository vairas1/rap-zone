import * as fs from 'fs';
import * as path from 'path';
import { Mp3Encoder } from '@breezystack/lamejs';

const outDir = path.resolve(process.cwd(), 'output');
fs.mkdirSync(outDir, { recursive: true });

console.log('Generating sample MP3 audio and cover images in', outDir);

function generateMp3(filename, durationSec = 15, freq = 440) {
  const sampleRate = 44100;
  const channels = 1;
  const kbps = 128;
  const samplesPerFrame = 1152;
  const mp3encoder = new Mp3Encoder(channels, sampleRate, kbps);
  
  const totalSamples = sampleRate * durationSec;
  const mp3Data = [];

  const sampleChunk = new Int16Array(samplesPerFrame);
  for (let s = 0; s < totalSamples; s += samplesPerFrame) {
    for (let i = 0; i < samplesPerFrame; i++) {
      const t = (s + i) / sampleRate;
      const val = Math.sin(2 * Math.PI * freq * t) * 10000 +
                  Math.sin(2 * Math.PI * (freq / 2) * t) * 15000 * (Math.floor(t * 4) % 2);
      sampleChunk[i] = Math.max(-32768, Math.min(32767, val));
    }
    const mp3buf = mp3encoder.encodeBuffer(sampleChunk);
    if (mp3buf.length > 0) {
      mp3Data.push(new Uint8Array(mp3buf));
    }
  }
  const endBuf = mp3encoder.flush();
  if (endBuf.length > 0) {
    mp3Data.push(new Uint8Array(endBuf));
  }

  const totalLength = mp3Data.reduce((acc, b) => acc + b.length, 0);
  const result = new Uint8Array(totalLength);
  let offset = 0;
  for (const b of mp3Data) {
    result.set(b, offset);
    offset += b.length;
  }

  fs.writeFileSync(path.join(outDir, filename), result);
  console.log(`Generated MP3: ${filename} (${result.length} bytes)`);
}

for (let a = 1; a <= 4; a++) {
  for (let s = 1; s <= 3; s++) {
    generateMp3(`song_${a}_${s}.mp3`, 12, 220 + a * 30 + s * 15);
  }
}

function generateImage(filename, rBase, gBase, bBase) {
  const width = 400;
  const height = 400;
  const rowSize = Math.floor((3 * width + 3) / 4) * 4;
  const pixelDataSize = rowSize * height;
  const fileSize = 54 + pixelDataSize;

  const buf = Buffer.alloc(fileSize);
  buf.write('BM', 0);
  buf.writeUInt32LE(fileSize, 2);
  buf.writeUInt32LE(54, 10);

  buf.writeUInt32LE(40, 14);
  buf.writeInt32LE(width, 18);
  buf.writeInt32LE(height, 22);
  buf.writeUInt16LE(1, 26);
  buf.writeUInt16LE(24, 28);
  buf.writeUInt32LE(pixelDataSize, 34);

  for (let y = 0; y < height; y++) {
    const rowOffset = 54 + (height - 1 - y) * rowSize;
    for (let x = 0; x < width; x++) {
      const colOffset = rowOffset + x * 3;
      const dx = x - width / 2;
      const dy = y - height / 2;
      const dist = Math.sqrt(dx * dx + dy * dy);
      const factor = Math.max(0, 1 - dist / (width / 1.2));

      const b = Math.min(255, Math.floor(bBase * factor + (x / width) * 50));
      const g = Math.min(255, Math.floor(gBase * factor + (y / height) * 50));
      const r = Math.min(255, Math.floor(rBase * factor + 40));

      buf.writeUInt8(b, colOffset);
      buf.writeUInt8(g, colOffset + 1);
      buf.writeUInt8(r, colOffset + 2);
    }
  }

  fs.writeFileSync(path.join(outDir, filename), buf);
  console.log(`Generated Image: ${filename}`);
}

for (let i = 1; i <= 4; i++) {
  generateImage(`artist_${i}.jpg`, 180 + i * 15, 20, 30);
  generateImage(`album_${i}.jpg`, 20, 20 + i * 40, 200);
}

console.log('Media generation completed successfully.');
