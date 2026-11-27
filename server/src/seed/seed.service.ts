import { Injectable, Logger, OnApplicationBootstrap } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ConfigService } from '@nestjs/config';
import { User } from '../entities/user.entity';
import { Artist } from '../entities/artist.entity';
import { Album } from '../entities/album.entity';
import { Song } from '../entities/song.entity';
import { Playlist } from '../entities/playlist.entity';
import { AuthService } from '../auth/auth.service';
import * as fs from 'fs';
import * as path from 'path';

@Injectable()
export class SeedService implements OnApplicationBootstrap {
  private logger = new Logger('Seed');

  constructor(
    @InjectRepository(User) private userRepo: Repository<User>,
    @InjectRepository(Artist) private artistRepo: Repository<Artist>,
    @InjectRepository(Album) private albumRepo: Repository<Album>,
    @InjectRepository(Song) private songRepo: Repository<Song>,
    @InjectRepository(Playlist) private playlistRepo: Repository<Playlist>,
    private auth: AuthService,
    private cfg: ConfigService,
  ) {}

  async onApplicationBootstrap() {
    await this.runSeed();
  }

  async runSeed() {
    const adminEmail = this.cfg.get('ADMIN_EMAIL') || 'admin@rapzone.app';
    const adminPass = this.cfg.get('ADMIN_PASSWORD') || 'Admin@123';
    await this.auth.createAdmin(adminEmail, adminPass, 'Rap Zone Master Admin');
    this.logger.log(`Admin account ensured: ${adminEmail}`);

    const artistCount = await this.artistRepo.count();
    if (artistCount > 0) {
      this.logger.log('Database already seeded.');
      return;
    }

    this.logger.log('Seeding initial rap artists, albums and songs...');

    // Ensure media directory exists (MEDIA_ROOT lets a host use a persistent disk)
    const mediaRoot = process.env.MEDIA_ROOT || path.resolve(process.cwd(), 'media');
    fs.mkdirSync(path.join(mediaRoot, 'audio'), { recursive: true });
    fs.mkdirSync(path.join(mediaRoot, 'image'), { recursive: true });

    // We copy pre-generated sample files from scripts/output when available;
    // otherwise audio falls back to public demo MP3 URLs (works on hosts where
    // local files cannot persist, e.g. Render free tier).
    const sampleDir = path.resolve(process.cwd(), '..', 'scripts', 'output');
    const hasLocalSamples = fs.existsSync(sampleDir);

    const sampleArtists = [
      { name: 'مروان موسى', nameEn: 'Marwan Moussa', bio: 'أحد أبرز نجوم الراب في العالم العربي، اشتهر بأسلوبه الفريد وإنتاجه الموسيقي المبتكر.' },
      { name: 'ويجز', nameEn: 'Wegz', bio: 'صوت جيل كامل وأيقونة الراب المصري والعربي، صاحب أشهر الأغاني التي حققت مليارات المشاهدات.' },
      { name: 'أفروتو', nameEn: 'Afroto', bio: 'مغني راب وكاتب أغاني مصري، يتميز بالطاقة العالية والأداء السريع والمميز.' },
      { name: 'البعلي', nameEn: 'El Baily', bio: 'نجم صاعد في سماء الراب العربي، بقوافي قوية وإيقاعات عصرية.' },
    ];

    const artistEntities: Artist[] = [];
    for (let i = 0; i < sampleArtists.length; i++) {
      const src = sampleArtists[i];
      const imgName = `artist_${i + 1}.jpg`;
      const imgRel = `image/${imgName}`;
      const imgDest = path.join(mediaRoot, imgRel);

      if (fs.existsSync(path.join(sampleDir, imgName))) {
        fs.copyFileSync(path.join(sampleDir, imgName), imgDest);
      } else {
        // write tiny 1x1 jpeg fallback
        fs.writeFileSync(imgDest, Buffer.from('/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=', 'base64'));
      }

      const artist = this.artistRepo.create({
        name: src.name,
        nameEn: src.nameEn,
        bio: src.bio,
        imageUrl: `/media/${imgRel}`,
        image: imgRel,
        isPublished: true,
        playCount: Math.floor(Math.random() * 500000) + 100000,
      });
      await this.artistRepo.save(artist);
      artistEntities.push(artist);
    }

    // Albums & Songs
    const sampleAlbums = [
      { title: 'شباكك', artistIdx: 0, year: 2024, cover: 'album_1.jpg' },
      { title: 'عزة نفس', artistIdx: 1, year: 2023, cover: 'album_2.jpg' },
      { title: 'سندباد', artistIdx: 2, year: 2024, cover: 'album_3.jpg' },
      { title: 'طريق النجاة', artistIdx: 3, year: 2025, cover: 'album_4.jpg' },
    ];

    for (let i = 0; i < sampleAlbums.length; i++) {
      const sa = sampleAlbums[i];
      const artist = artistEntities[sa.artistIdx];
      const coverName = sa.cover;
      const coverRel = `image/${coverName}`;
      const coverDest = path.join(mediaRoot, coverRel);

      if (fs.existsSync(path.join(sampleDir, coverName))) {
        fs.copyFileSync(path.join(sampleDir, coverName), coverDest);
      } else {
        fs.writeFileSync(coverDest, Buffer.from('/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=', 'base64'));
      }

      const album = this.albumRepo.create({
        title: sa.title,
        description: `الألبوم الرسمي الجديد للفنان ${artist.name}`,
        releaseYear: sa.year,
        coverUrl: `/media/${coverRel}`,
        coverPath: coverRel,
        artistId: artist.id,
        isPublished: true,
      });
      await this.albumRepo.save(album);

      // Add 3 songs per album
      for (let s = 1; s <= 3; s++) {
        const audioFileName = `song_${i + 1}_${s}.mp3`;
        const audioRel = `audio/${audioFileName}`;
        const audioDest = path.join(mediaRoot, audioRel);

        let songAudioUrl = `/media/${audioRel}`;
        let songAudioPath: string | null = audioRel;
        if (hasLocalSamples) {
          if (fs.existsSync(path.join(sampleDir, audioFileName))) {
            fs.copyFileSync(path.join(sampleDir, audioFileName), audioDest);
          } else {
            // local dev without samples: small dummy mp3
            fs.writeFileSync(audioDest, Buffer.from('ID3v2...dummy mp3 audio content...'));
          }
        } else {
          // Host without local samples (e.g. Render): point at public demo MP3s
          songAudioUrl = `https://www.soundhelix.com/examples/mp3/SoundHelix-Song-${((i * 3 + s) % 16) + 1}.mp3`;
          songAudioPath = null;
        }

        const song = this.songRepo.create({
          title: `${sa.title} - تراك ${s}`,
          lyrics: `[كلمات تراك ${s} - ${sa.title}]\nكل يوم حكاية جديدة في سماء الراب\nنكتب التاريخ بالكلمات والخطاب\nراب زون التطبيق الأول والأفضل\nلا نحتار ولا نتقبل إلا الأفضل`,
          durationSec: 185 + s * 12,
          audioUrl: songAudioUrl,
          audioPath: songAudioPath,
          imageUrl: `/media/${coverRel}`,
          artistId: artist.id,
          albumId: album.id,
          position: s,
          playCount: Math.floor(Math.random() * 200000) + 5000,
          downloadCount: Math.floor(Math.random() * 50000) + 1000,
          isPublished: true,
        });
        await this.songRepo.save(song);
      }
    }

    // Playlist
    const playlist = this.playlistRepo.create({
      name: 'أفضل أغاني الراب العربي 2026',
      nameEn: 'Top Arab Rap 2026',
      description: 'قائمة تشغيل مميزة تضم أقوى وأشهر تريكات الراب العربي.',
      imageUrl: artistEntities[0].imageUrl,
      isPublished: true,
      position: 1,
    });
    await this.playlistRepo.save(playlist);

    this.logger.log('Seed completed successfully!');
  }
}
