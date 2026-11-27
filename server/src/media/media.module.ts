import { Module } from '@nestjs/common';
import { StorageService } from './storage.service';
import { MediaService } from './media.service';

@Module({
  providers: [StorageService, MediaService],
  exports: [StorageService, MediaService],
})
export class MediaModule {}