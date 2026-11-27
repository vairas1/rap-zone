import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { MediaAsset } from '../entities/media-asset.entity';
import { StorageService } from '../media/storage.service';
import { MediaService } from '../media/media.service';

@Injectable()
export class UploadService {
  constructor(
    @InjectRepository(MediaAsset) private assetRepo: Repository<MediaAsset>,
    private storage: StorageService,
    private media: MediaService,
  ) {}

  async uploadAudio(tmpPath: string, originalName: string, uploadedBy: string) {
    const info = await this.media.inspectAudio(tmpPath);
    const stored = await this.storage.saveFile(tmpPath, 'audio', originalName);
    const asset = this.assetRepo.create({
      checksum: info.sha256,
      sizeBytes: stored.sizeBytes,
      mime: stored.mime,
      kind: 'audio',
      path: stored.path,
      publicUrl: stored.publicUrl,
      uploadedByEmail: uploadedBy,
    });
    await this.assetRepo.save(asset);
    return { ...asset, durationSec: info.durationSec };
  }

  async uploadImage(tmpPath: string, originalName: string, uploadedBy: string) {
    const info = this.media.inspectImage(tmpPath);
    const stored = await this.storage.saveFile(tmpPath, 'image', originalName);
    const asset = this.assetRepo.create({
      checksum: info.sha256,
      sizeBytes: stored.sizeBytes,
      mime: info.mime,
      kind: 'image',
      path: stored.path,
      publicUrl: stored.publicUrl,
      uploadedByEmail: uploadedBy,
    });
    await this.assetRepo.save(asset);
    return asset;
  }

  async findByChecksum(kind: string, checksum: string) {
    return this.assetRepo.findOne({ where: { kind, checksum } });
  }

  async remove(id: string) {
    const asset = await this.assetRepo.findOne({ where: { id } });
    if (!asset) return null;
    await this.storage.deleteFile(asset.path);
    await this.assetRepo.remove(asset);
    return { success: true };
  }
}