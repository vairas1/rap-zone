import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Album } from '../entities/album.entity';
import { Artist } from '../entities/artist.entity';
import { Song } from '../entities/song.entity';
import { AlbumsController } from './albums.controller';
import { AlbumsService } from './albums.service';
import { MediaModule } from '../media/media.module';

@Module({
  imports: [TypeOrmModule.forFeature([Album, Artist, Song]), MediaModule],
  controllers: [AlbumsController],
  providers: [AlbumsService],
  exports: [AlbumsService],
})
export class AlbumsModule {}