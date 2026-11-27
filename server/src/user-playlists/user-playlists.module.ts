import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { UserPlaylist } from '../entities/user-playlist.entity';
import { UserPlaylistSong } from '../entities/user-playlist-song.entity';
import { Song } from '../entities/song.entity';
import { User } from '../entities/user.entity';
import { UserPlaylistsController } from './user-playlists.controller';
import { UserPlaylistsService } from './user-playlists.service';

@Module({
  imports: [TypeOrmModule.forFeature([UserPlaylist, UserPlaylistSong, Song, User])],
  controllers: [UserPlaylistsController],
  providers: [UserPlaylistsService],
  exports: [UserPlaylistsService],
})
export class UserPlaylistsModule {}