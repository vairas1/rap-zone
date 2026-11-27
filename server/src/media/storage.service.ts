import { Injectable, BadRequestException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createHash, randomUUID } from 'crypto';
import { createReadStream, existsSync, mkdirSync, statSync, unlinkSync, writeFileSync } from 'fs';
import { join, extname, resolve, sep } from 'path';
import {
  S3Client, PutObjectCommand, DeleteObjectCommand, GetObjectCommand,
} from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';

export interface StoredFile {
  /** relative path inside the media root (local) or object key (s3) */
  path: string;
  /** public url to be stored in the database */
  publicUrl: string;
  sizeBytes: number;
  mime: string;
}

@Injectable()
export class StorageService {
  private driver: string;
  private mediaRoot = process.env.MEDIA_ROOT || resolve(process.cwd(), 'media');
  private s3?: S3Client;
  private bucket?: string;
  private publicBase?: string;

  constructor(private cfg: ConfigService) {
    this.driver = cfg.get('STORAGE_DRIVER') || 'local';
    if (this.driver === 's3') {
      this.bucket = cfg.get('S3_BUCKET');
      const endpoint = cfg.get('S3_ENDPOINT') || undefined;
      this.s3 = new S3Client({
        region: cfg.get('S3_REGION') || 'us-east-1',
        endpoint,
        forcePathStyle: !!endpoint,
        credentials: {
          accessKeyId: cfg.get('S3_ACCESS_KEY'),
          secretAccessKey: cfg.get('S3_SECRET_KEY'),
        },
      });
      this.publicBase = cfg.get('S3_PUBLIC_BASE') || '';
    }
    mkdirSync(this.mediaRoot, { recursive: true });
  }

  get publicBaseUrl(): string {
    const base = (this.cfg.get('PUBLIC_BASE_URL') || '').replace(/\/+$/, '');
    return base;
  }

  /**
   * Converts a stored relative path (/media/...) into an absolute URL
   * so that the Android app and the web admin panel can load it directly.
   */
  toUrl(rel: string | null | undefined): string | null {
    if (!rel) return null;
    if (/^https?:\/\//i.test(rel)) return rel;
    const base = this.publicBaseUrl;
    if (!base) return rel;
    return `${base}${rel.startsWith('/') ? '' : '/'}${rel}`;
  }

  /**
   * Saves an uploaded temporary file into permanent storage.
   * @param tmpPath absolute path of the multer temp file
   */
  async saveFile(tmpPath: string, kind: 'audio' | 'image', originalName: string): Promise<StoredFile> {
    const mime = kind === 'audio' ? 'audio/mpeg' : 'image/jpeg';
    const ext = kind === 'audio' ? '.mp3' : extname(originalName || '.jpg').toLowerCase() || '.jpg';
    const key = `${kind}/${randomUUID()}${ext}`;

    const sizeBytes = statSync(tmpPath).size;

    if (this.driver === 's3') {
      await this.s3.send(new PutObjectCommand({
        Bucket: this.bucket,
        Key: key,
        Body: createReadStream(tmpPath),
        ContentType: mime,
      }));
      const publicUrl = this.publicBase
        ? `${this.publicBase.replace(/\/+$/, '')}/${key}`
        : await getSignedUrl(this.s3, new GetObjectCommand({ Bucket: this.bucket, Key: key }), { expiresIn: 3600 });
      return { path: key, publicUrl, sizeBytes, mime };
    }

    const dest = join(this.mediaRoot, key);
    mkdirSync(join(dest, '..'), { recursive: true });
    writeFileSync(dest, require('fs').readFileSync(tmpPath));
    try { unlinkSync(tmpPath); } catch { /* ignore */ }

    return { path: key, publicUrl: `/media/${key}`, sizeBytes, mime };
  }

  /** Deletes a stored file by its relative path. */
  async deleteFile(relPath: string): Promise<void> {
    if (!relPath) return;
    if (this.driver === 's3') {
      await this.s3.send(new DeleteObjectCommand({ Bucket: this.bucket, Key: relPath }));
      return;
    }
    const target = resolve(this.mediaRoot, relPath);
    // Prevent path traversal
    if (!target.startsWith(this.mediaRoot + sep)) {
      throw new BadRequestException('Invalid media path');
    }
    if (existsSync(target)) unlinkSync(target);
  }

  /** Absolute local path of a stored file (local driver only). */
  absolutePath(relPath: string): string | null {
    if (this.driver !== 'local') return null;
    const target = resolve(this.mediaRoot, relPath);
    if (!target.startsWith(this.mediaRoot + sep)) return null;
    return target;
  }

  checksum(relPath: string): string | null {
    const abs = this.absolutePath(relPath);
    if (!abs || !existsSync(abs)) return null;
    return createHash('sha256').update(require('fs').readFileSync(abs)).digest('hex');
  }
}
