import { Injectable, BadRequestException } from '@nestjs/common';
import { existsSync, readFileSync } from 'fs';
import { StorageService } from './storage.service';

const MP3_MAGIC = Buffer.from([0x49, 0x44, 0x33]); // "ID3"

@Injectable()
export class MediaService {
  constructor(private storage: StorageService) {}

  async inspectAudio(tmpPath: string): Promise<{ durationSec: number; mime: string; sha256: string }> {
    if (!existsSync(tmpPath)) throw new BadRequestException('Uploaded file missing');
    const stat = require('fs').statSync(tmpPath);
    if (stat.size < 1024) throw new BadRequestException('Audio file is too small to be valid');

    const head = readFileSync(tmpPath).slice(0, 16);
    const hasId3 = head.slice(0, 3).equals(MP3_MAGIC);
    const hasFrameSync = head[0] === 0xff && (head[1] & 0xe0) === 0xe0;
    if (!hasId3 && !hasFrameSync) {
      throw new BadRequestException('Invalid MP3 file: missing audio frame header');
    }

    const durationSec = this.parseMp3Duration(tmpPath, stat.size);
    if (!durationSec || durationSec <= 0) {
      throw new BadRequestException('Could not determine audio duration - file may be corrupted');
    }
    const sha256 = this.storage.checksum(tmpPath) || '';
    return { durationSec: Math.round(durationSec), mime: 'audio/mpeg', sha256 };
  }

  private parseMp3Duration(filePath: string, size: number): number {
    const buf = readFileSync(filePath);

    let offset = 0;
    if (buf.slice(0, 3).equals(MP3_MAGIC)) {
      const sizeBytes = ((buf[6] & 0x7f) << 21) | ((buf[7] & 0x7f) << 14) |
        ((buf[8] & 0x7f) << 7) | (buf[9] & 0x7f);
      offset = 10 + sizeBytes;
    }

    const BITRATES_V1_L3 = [0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0];
    const BITRATES_V2_L3 = [0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0];
    const RATES_V1 = [44100, 48000, 32000, 0];
    const RATES_V2 = [22050, 24000, 16000, 0];
    const RATES_V25 = [11025, 12000, 8000, 0];

    let frameCount = 0;
    let totalBitrate = 0;
    let samplesPerFrame = 1152;
    let sampleRate = 44100;
    let foundFrame = false;

    let i = offset;
    while (i < buf.length - 4) {
      if (buf[i] === 0xff && (buf[i + 1] & 0xe0) === 0xe0) {
        const versionBits = (buf[i + 1] >> 3) & 0x03;
        const layerBits = (buf[i + 1] >> 1) & 0x03;
        const bitrateIndex = (buf[i + 2] >> 4) & 0x0f;
        const rateIndex = (buf[i + 2] >> 2) & 0x03;
        const padding = (buf[i + 2] >> 1) & 0x01;

        if (versionBits === 1 || layerBits !== 1) { i++; continue; }
        if (bitrateIndex === 0 || bitrateIndex === 15 || rateIndex === 3) { i++; continue; }

        let bitrate: number;
        let rate: number;
        if (versionBits === 3) {
          bitrate = BITRATES_V1_L3[bitrateIndex] * 1000;
          rate = RATES_V1[rateIndex];
          samplesPerFrame = 1152;
        } else if (versionBits === 2) {
          bitrate = BITRATES_V2_L3[bitrateIndex] * 1000;
          rate = RATES_V2[rateIndex];
          samplesPerFrame = 576;
        } else {
          bitrate = BITRATES_V2_L3[bitrateIndex] * 1000;
          rate = RATES_V25[rateIndex];
          samplesPerFrame = 576;
        }
        if (!bitrate || !rate) { i++; continue; }

        const xingOffset = i + 4 + (versionBits === 3 ? 32 : 17);
        const xingTag = buf.slice(i + 4, i + 8).toString('latin1');
        if (xingTag === 'Xing' || xingTag === 'Info') {
          const flags = buf.readUInt32BE(i + 8);
          let frames = 0;
          if (flags & 0x0001) frames = buf.readUInt32BE(i + 12);
          if (flags & 0x0002) frames = buf.readUInt32BE(i + 16);
          if (frames > 0) {
            return (frames * samplesPerFrame) / rate;
          }
        }

        foundFrame = true;
        sampleRate = rate;
        totalBitrate += bitrate;
        frameCount++;

        const frameLength = Math.floor((samplesPerFrame / 8) * (bitrate / rate)) + padding;
        i += frameLength > 0 ? frameLength : 1;
        if (frameCount > 400000) break;
      } else {
        i++;
      }
    }

    if (!foundFrame || frameCount === 0 || totalBitrate === 0) return 0;
    const avgBitrate = totalBitrate / frameCount;
    return (size * 8) / avgBitrate;
  }

  inspectImage(tmpPath: string): { mime: string; sha256: string } {
    if (!existsSync(tmpPath)) throw new BadRequestException('Uploaded file missing');
    const buf = readFileSync(tmpPath).slice(0, 32);
    let mime = '';
    if (buf[0] === 0xff && buf[1] === 0xd8 && buf[2] === 0xff) mime = 'image/jpeg';
    else if (buf.slice(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))) mime = 'image/png';
    else if (buf.slice(0, 4).toString('ascii') === 'RIFF' && buf.slice(8, 12).toString('ascii') === 'WEBP') mime = 'image/webp';

    if (!mime) throw new BadRequestException('Invalid image file (only JPEG, PNG and WEBP are allowed)');
    return { mime, sha256: this.storage.checksum(tmpPath) || '' };
  }
}