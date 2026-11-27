import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { User } from '../entities/user.entity';
import { Artist } from '../entities/artist.entity';
import { Album } from '../entities/album.entity';
import { Song } from '../entities/song.entity';
import { Playlist } from '../entities/playlist.entity';
import { SeedService } from './seed.service';
import { AuthModule } from '../auth/auth.module';

@Module({
  imports: [AuthModule, TypeOrmModule.forFeature([User, Artist, Album, Song, Playlist])],
  providers: [SeedService],
  exports: [SeedService],
})
export class SeedModule {}