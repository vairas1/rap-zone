import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Song } from '../entities/song.entity';
import { Album } from '../entities/album.entity';
import { Artist } from '../entities/artist.entity';
import { PlayEvent } from '../entities/play-event.entity';
import { StorageService } from '../media/storage.service';

@Injectable()
export class SongsService {
  constructor(
    @InjectRepository(Song) private songRepo: Repository<Song>,
    @InjectRepository(Album) private albumRepo: Repository<Album>,
    @InjectRepository(Artist) private artistRepo: Repository<Artist>,
    @InjectRepository(PlayEvent) private playEventRepo: Repository<PlayEvent>,
    private storage: StorageService,
  ) {}

  async findAll(opts: {
    artistId?: string;
    albumId?: string;
    q?: string;
    limit?: number;
    offset?: number;
    includeUnpublished?: boolean;
    sortBy?: 'latest' | 'popular';
  }) {
    const qb = this.songRepo.createQueryBuilder('s')
      .leftJoinAndSelect('s.artist', 'a')
      .leftJoinAndSelect('s.album', 'al')
      .orderBy(opts.sortBy === 'popular' ? 's.playCount' : 's.releasedAt', 'DESC')
      .addOrderBy('s.position', 'ASC');

    if (opts.artistId) qb.andWhere('s.artistId = :artistId', { artistId: opts.artistId });
    if (opts.albumId) qb.andWhere('s.albumId = :albumId', { albumId: opts.albumId });
    if (opts.q && opts.q.trim()) {
      const q = `%${opts.q.trim()}%`;
      qb.andWhere('(s.title ILIKE :q OR a.name ILIKE :q)', { q });
    }
    if (!opts.includeUnpublished) {
      qb.andWhere('s.isPublished = true');
    }
    qb.take(Math.min(opts.limit || 50, 200)).skip(opts.offset || 0);

    const [items, total] = await qb.getManyAndCount();
    return {
      items: items.map(s => this.toPublic(s)),
      total,
    };
  }

  async findOne(id: string, includeUnpublished = false) {
    const song = await this.songRepo.findOne({
      where: { id },
      relations: { artist: true, album: true },
    });
    if (!song) throw new NotFoundException('Song not found');
    if (!includeUnpublished && !song.isPublished) throw new NotFoundException('Song not found');
    return this.toPublic(song);
  }

  async create(dto: any) {
    const album = await this.albumRepo.findOne({ where: { id: dto.albumId } });
    if (!album) throw new NotFoundException('Album not found');
    const artist = await this.artistRepo.findOne({ where: { id: album.artistId } });
    if (!artist) throw new NotFoundException('Artist not found');

    const maxPos = await this.songRepo
      .createQueryBuilder('s')
      .select('MAX(s.position)', 'max')
      .where('s.albumId = :albumId', { albumId: dto.albumId })
      .getRawOne();

    const song = this.songRepo.create({
      title: dto.title,
      lyrics: dto.lyrics,
      durationSec: dto.durationSec,
      audioUrl: dto.audioUrl,
      audioPath: dto.audioPath,
      imageUrl: dto.imageUrl,
      imagePath: dto.imagePath,
      artistId: artist.id,
      albumId: dto.albumId,
      assetId: dto.assetId,
      position: dto.position || ((maxPos?.max || 0) + 1),
      isPublished: dto.isPublished !== false,
      licenseStatus: dto.licenseStatus || 'owned',
      licenseNote: dto.licenseNote,
      releasedAt: new Date(),
    });
    await this.songRepo.save(song);
    return this.toPublic(song);
  }

  async update(id: string, dto: any) {
    const song = await this.songRepo.findOne({ where: { id }, relations: { artist: true, album: true } });
    if (!song) throw new NotFoundException('Song not found');

    if (dto.title !== undefined) song.title = dto.title;
    if (dto.lyrics !== undefined) song.lyrics = dto.lyrics;
    if (dto.durationSec !== undefined) song.durationSec = dto.durationSec;
    if (dto.audioUrl !== undefined) song.audioUrl = dto.audioUrl;
    if (dto.audioPath !== undefined) song.audioPath = dto.audioPath;
    if (dto.imageUrl !== undefined) song.imageUrl = dto.imageUrl;
    if (dto.imagePath !== undefined) song.imagePath = dto.imagePath;
    if (dto.position !== undefined) song.position = dto.position;
    if (dto.isPublished !== undefined) song.isPublished = dto.isPublished;
    if (dto.licenseStatus !== undefined) song.licenseStatus = dto.licenseStatus;
    if (dto.licenseNote !== undefined) song.licenseNote = dto.licenseNote;

    await this.songRepo.save(song);
    return this.toPublic(song);
  }

  async remove(id: string) {
    const song = await this.songRepo.findOne({ where: { id } });
    if (!song) throw new NotFoundException('Song not found');
    if (song.audioPath) await this.storage.deleteFile(song.audioPath);
    if (song.imagePath) await this.storage.deleteFile(song.imagePath);
    await this.songRepo.remove(song);
    return { success: true };
  }

  async recordPlay(songId: string, userEmail: string | null, durationSec: number) {
    const song = await this.songRepo.findOne({ where: { id: songId } });
    if (!song) return;
    song.playCount = (song.playCount || 0) + 1;
    await this.songRepo.save(song);
    // Also update artist play count
    const artist = await this.artistRepo.findOne({ where: { id: song.artistId } });
    if (artist) {
      artist.playCount = (artist.playCount || 0) + 1;
      await this.artistRepo.save(artist);
    }
    // Record detailed event
    const event = this.playEventRepo.create({
      userEmail: userEmail || null,
      songId,
      artistId: song.artistId,
      durationSec,
    });
    await this.playEventRepo.save(event);
  }

  async recordDownload(songId: string) {
    const song = await this.songRepo.findOne({ where: { id: songId } });
    if (!song) return;
    song.downloadCount = (song.downloadCount || 0) + 1;
    await this.songRepo.save(song);
  }

  toPublic(s: Song) {
    return {
      id: s.id,
      title: s.title,
      lyrics: s.lyrics,
      durationSec: s.durationSec,
      audioUrl: s.audioUrl,
      imageUrl: s.imageUrl,
      playCount: s.playCount,
      downloadCount: s.downloadCount,
      isPublished: s.isPublished,
      position: s.position,
      releasedAt: s.releasedAt,
      artistId: (s as any).artist?.id,
      artistName: (s as any).artist?.name,
      albumId: s.albumId,
      albumTitle: (s as any).album?.title,
      artistImageUrl: (s as any).artist?.imageUrl,
    };
  }
}