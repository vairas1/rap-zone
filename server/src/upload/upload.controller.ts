import {
  Controller, Post, UploadedFile, UseInterceptors, UseGuards, Request,
  BadRequestException, MaxFileSizeValidator,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { diskStorage } from 'multer';
import { join } from 'path';
import { tmpdir } from 'os';
import { UploadService } from './upload.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { RolesGuard, Roles } from '../common/roles.guard';
import { ConfigService } from '@nestjs/config';

@Controller('upload')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles('admin')
export class UploadController {
  constructor(private upload: UploadService, private cfg: ConfigService) {}

  @Post('audio')
  @UseInterceptors(FileInterceptor('file', {
    storage: diskStorage({
      destination: (req, file, cb) => {
        const dir = join(tmpdir(), 'rapzone-uploads');
        require('fs').mkdirSync(dir, { recursive: true });
        cb(null, dir);
      },
      filename: (req, file, cb) => {
        const ext = (file.originalname.match(/\.[a-z0-9]+$/i) || ['.mp3'])[0].toLowerCase();
        cb(null, `${Date.now()}-${Math.random().toString(36).slice(2)}${ext}`);
      },
    }),
    limits: { fileSize: parseInt(process.env.MAX_UPLOAD_SIZE || '62914560', 10) },
    fileFilter: (req, file, cb) => {
      const ok = /^audio\/(mpeg|mp3|mpeg3)$/i.test(file.mimetype) || /\.mp3$/i.test(file.originalname);
      cb(null, ok);
    },
  }))
  async uploadAudio(@UploadedFile() file: any, @Request() req: any) {
    if (!file) throw new BadRequestException('No audio file provided (expected .mp3)');
    const result = await this.upload.uploadAudio(file.path, file.originalname, req.user.email);
    return {
      id: result.id,
      url: result.publicUrl,
      path: result.path,
      durationSec: result.durationSec,
      sizeBytes: result.sizeBytes,
      mime: result.mime,
      checksum: result.checksum,
    };
  }

  @Post('image')
  @UseInterceptors(FileInterceptor('file', {
    storage: diskStorage({
      destination: (req, file, cb) => {
        const dir = join(tmpdir(), 'rapzone-uploads');
        require('fs').mkdirSync(dir, { recursive: true });
        cb(null, dir);
      },
      filename: (req, file, cb) => {
        const ext = (file.originalname.match(/\.[a-z0-9]+$/i) || ['.jpg'])[0].toLowerCase();
        cb(null, `${Date.now()}-${Math.random().toString(36).slice(2)}${ext}`);
      },
    }),
    limits: { fileSize: 10 * 1024 * 1024 },
    fileFilter: (req, file, cb) => {
      const ok = /^image\/(jpeg|png|webp)$/i.test(file.mimetype) ||
        /\.(jpe?g|png|webp)$/i.test(file.originalname);
      cb(null, ok);
    },
  }))
  async uploadImage(@UploadedFile() file: any, @Request() req: any) {
    if (!file) throw new BadRequestException('No image file provided (JPEG/PNG/WEBP)');
    const result = await this.upload.uploadImage(file.path, file.originalname, req.user.email);
    return {
      id: result.id,
      url: result.publicUrl,
      path: result.path,
      sizeBytes: result.sizeBytes,
      mime: result.mime,
    };
  }
}
