import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ServeStaticModule } from '@nestjs/serve-static';
import { ThrottlerModule } from '@nestjs/throttler';
import { join } from 'path';
import { ConfigModule as AppConfigModule } from './config';
import { AuthModule } from './auth/auth.module';
import { CoreAuthModule } from './auth/core-auth.module';
import { HomeModule } from './home/home.module';
import { SeedModule } from './seed/seed.module';
import { ArtistsModule } from './artists/artists.module';
import { AlbumsModule } from './albums/albums.module';
import { SongsModule } from './songs/songs.module';
import { PlaylistsModule } from './playlists/playlists.module';
import { UserPlaylistsModule } from './user-playlists/user-playlists.module';
import { FavoritesModule } from './favorites/favorites.module';
import { StatsModule } from './stats/stats.module';
import { UploadModule } from './upload/upload.module';
import { MediaModule } from './media/media.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
      load: [AppConfigModule.load],
    }),
    // TypeORM: connect to database (PostgreSQL for production, SQLite for dev fallback)
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      useFactory: (cfg: ConfigService) => {
        const useEmbedded = cfg.get('USE_EMBEDDED_DB') === 'true';
        
        if (useEmbedded) {
          return {
            type: 'postgres',
            host: cfg.get('DB_HOST') || 'localhost',
            port: parseInt(cfg.get('DB_PORT') || '5432', 10),
            username: cfg.get('DB_USER') || 'postgres',
            password: cfg.get('DB_PASSWORD') || 'postgres',
            database: cfg.get('DB_NAME') || 'rapzone',
            autoLoadEntities: true,
            synchronize: true,
            logging: cfg.get('NODE_ENV') !== 'production',
            extra: { statement_timeout: 10000 },
            retryAttempts: 10,
            retryDelay: 3000,
          };
        }
        
        // SQLite for development (no external DB needed).
        // DB_FILE lets a host (e.g. Render) point it at a persistent disk.
        const sqlitePath = process.env.DB_FILE || (process.cwd() + '/dev.db');
        console.log('[db] Using SQLite database at', sqlitePath);
        return {
          type: 'sqlite',
          database: sqlitePath,
          autoLoadEntities: true,
          synchronize: true,
          logging: cfg.get('NODE_ENV') !== 'production',
        };
      },
      inject: [ConfigService],
    }),
    // Serve static media files (uploads) at /media URL path.
    // MEDIA_ROOT lets a host (e.g. Render) point it at a persistent disk.
    ServeStaticModule.forRootAsync({
      imports: [ConfigModule],
      useFactory: (cfg: ConfigService) => [
        {
          rootPath: process.env.MEDIA_ROOT || join(process.cwd(), 'media'),
          serveRoot: '/media',
          serveStaticOptions: {
            fallthrough: false,
            immutable: true,
            maxAge: '1y',
          },
        },
      ],
      inject: [ConfigService],
    }),
    // Rate limiting.
    ThrottlerModule.forRoot([{
      ttl: 60000,
      limit: 300,
    }]),
    // Feature modules.
    CoreAuthModule,
    AuthModule,
    HomeModule,
    ArtistsModule,
    AlbumsModule,
    SongsModule,
    PlaylistsModule,
    UserPlaylistsModule,
    FavoritesModule,
    StatsModule,
    UploadModule,
    MediaModule,
    SeedModule,
  ],
})
export class AppModule {}