import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Song } from '../entities/song.entity';
import { Album } from '../entities/album.entity';
import { Artist } from '../entities/artist.entity';
import { PlayEvent } from '../entities/play-event.entity';
import { SongsController } from './songs.controller';
import { SongsService } from './songs.service';
import { MediaModule } from '../media/media.module';

@Module({
  imports: [TypeOrmModule.forFeature([Song, Album, Artist, PlayEvent]), MediaModule],
  controllers: [SongsController],
  providers: [SongsService],
  exports: [SongsService],
})
export class SongsModule {}