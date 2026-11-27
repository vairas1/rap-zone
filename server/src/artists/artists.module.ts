import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Artist } from '../entities/artist.entity';
import { Album } from '../entities/album.entity';
import { Song } from '../entities/song.entity';
import { ArtistsController } from './artists.controller';
import { ArtistsService } from './artists.service';
import { MediaModule } from '../media/media.module';

@Module({
  imports: [TypeOrmModule.forFeature([Artist, Album, Song]), MediaModule],
  controllers: [ArtistsController],
  providers: [ArtistsService],
  exports: [ArtistsService],
})
export class ArtistsModule {}