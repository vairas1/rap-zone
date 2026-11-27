import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Artist } from '../entities/artist.entity';
import { Album } from '../entities/album.entity';
import { Song } from '../entities/song.entity';
import { Playlist } from '../entities/playlist.entity';
import { StorageService } from '../media/storage.service';

@Injectable()
export class HomeService {
  constructor(
    @InjectRepository(Artist) private artistRepo: Repository<Artist>,
    @InjectRepository(Album) private albumRepo: Repository<Album>,
    @InjectRepository(Song) private songRepo: Repository<Song>,
    @InjectRepository(Playlist) private playlistRepo: Repository<Playlist>,
    private storage: StorageService,
  ) {}

  /** Aggregated home feed: everything the app home screen needs in one request. */
  async feed() {
    const [artists, albums, latestSongs, popularSongs, playlists] = await Promise.all([
      this.artistRepo.find({
        where: { isPublished: true },
        order: { playCount: 'DESC', createdAt: 'DESC' },
        take: 12,
      }),
      this.albumRepo.find({
        where: { isPublished: true },
        relations: { artist: true },
        order: { releaseYear: 'DESC', createdAt: 'DESC' },
        take: 12,
      }),
      this.songRepo.find({
        where: { isPublished: true },
        relations: { artist: true, album: true },
        order: { releasedAt: 'DESC' },
        take: 20,
      }),
      this.songRepo.find({
        where: { isPublished: true },
        relations: { artist: true, album: true },
        order: { playCount: 'DESC' },
        take: 20,
      }),
      this.playlistRepo.find({
        where: { isPublished: true },
        order: { position: 'ASC', createdAt: 'DESC' },
        take: 12,
      }),
    ]);

    return {
      featuredArtists: artists.map(a => ({
        id: a.id,
        name: a.name,
        nameEn: a.nameEn,
        bio: a.bio,
        imageUrl: this.storage.toUrl(a.imageUrl),
        playCount: a.playCount,
      })),
      latestAlbums: albums.map(a => ({
        id: a.id,
        title: a.title,
        coverUrl: this.storage.toUrl(a.coverUrl),
        releaseYear: a.releaseYear,
        artistId: a.artistId,
        artistName: a.artist?.name,
      })),
      latestSongs: latestSongs.map(s => this.song(s)),
      popularSongs: popularSongs.map(s => this.song(s)),
      featuredPlaylists: playlists.map(p => ({
        id: p.id,
        name: p.name,
        nameEn: p.nameEn,
        description: p.description,
        imageUrl: this.storage.toUrl(p.imageUrl),
      })),
    };
  }

  /** Quick search across artists, albums and songs. */
  async search(q: string, limit = 20) {
    const query = (q || '').trim();
    if (!query) return { query, artists: [], albums: [], songs: [] };
    const like = `%${query}%`;
    const take = Math.min(limit, 50);

    const [artists, albums, songs] = await Promise.all([
      this.artistRepo.createQueryBuilder('a')
        .where('a.isPublished = true AND (a.name ILIKE :q OR a.nameEn ILIKE :q)', { q: like })
        .take(take).getMany(),
      this.albumRepo.createQueryBuilder('al')
        .leftJoinAndSelect('al.artist', 'ar')
        .where('al.isPublished = true AND (al.title ILIKE :q OR ar.name ILIKE :q)', { q: like })
        .take(take).getMany(),
      this.songRepo.createQueryBuilder('s')
        .leftJoinAndSelect('s.artist', 'ar')
        .leftJoinAndSelect('s.album', 'alb')
        .where('s.isPublished = true AND (s.title ILIKE :q OR ar.name ILIKE :q)', { q: like })
        .take(take).getMany(),
    ]);

    return {
      query,
      artists: artists.map(a => ({
        id: a.id, name: a.name, imageUrl: this.storage.toUrl(a.imageUrl),
      })),
      albums: albums.map(a => ({
        id: a.id, title: a.title, coverUrl: this.storage.toUrl(a.coverUrl),
        artistName: a.artist?.name,
      })),
      songs: songs.map(s => this.song(s)),
    };
  }

  private song(s: any) {
    return {
      id: s.id,
      title: s.title,
      durationSec: s.durationSec,
      audioUrl: this.storage.toUrl(s.audioUrl),
      imageUrl: this.storage.toUrl(s.imageUrl || s.artist?.imageUrl),
      playCount: s.playCount,
      downloadCount: s.downloadCount,
      artistId: s.artist?.id,
      artistName: s.artist?.name,
      albumId: s.albumId,
      albumTitle: s.album?.title,
      artistImageUrl: this.storage.toUrl(s.artist?.imageUrl),
      releasedAt: s.releasedAt,
    };
  }
}