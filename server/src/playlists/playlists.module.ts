import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Playlist } from '../entities/playlist.entity';
import { PlaylistSong } from '../entities/playlist-song.entity';
import { Song } from '../entities/song.entity';
import { PlaylistsController } from './playlists.controller';
import { PlaylistsService } from './playlists.service';
import { MediaModule } from '../media/media.module';

@Module({
  imports: [TypeOrmModule.forFeature([Playlist, PlaylistSong, Song]), MediaModule],
  controllers: [PlaylistsController],
  providers: [PlaylistsService],
  exports: [PlaylistsService],
})
export class PlaylistsModule {}