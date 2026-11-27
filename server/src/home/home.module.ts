import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Artist } from '../entities/artist.entity';
import { Album } from '../entities/album.entity';
import { Song } from '../entities/song.entity';
import { Playlist } from '../entities/playlist.entity';
import { HomeController } from './home.controller';
import { HomeService } from './home.service';
import { MediaModule } from '../media/media.module';

@Module({
  imports: [TypeOrmModule.forFeature([Artist, Album, Song, Playlist]), MediaModule],
  controllers: [HomeController],
  providers: [HomeService],
})
export class HomeModule {}