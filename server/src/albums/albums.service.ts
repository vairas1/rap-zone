import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Album } from '../entities/album.entity';
import { Artist } from '../entities/artist.entity';
import { Song } from '../entities/song.entity';
import { StorageService } from '../media/storage.service';

@Injectable()
export class AlbumsService {
  constructor(
    @InjectRepository(Album) private albumRepo: Repository<Album>,
    @InjectRepository(Artist) private artistRepo: Repository<Artist>,
    @InjectRepository(Song) private songRepo: Repository<Song>,
    private storage: StorageService,
  ) {}

  async findAll(opts: { artistId?: string; q?: string; limit?: number; offset?: number; includeUnpublished?: boolean }) {
    const qb = this.albumRepo.createQueryBuilder('al')
      .leftJoinAndSelect('al.artist', 'a')
      .orderBy('al.releaseYear', 'DESC')
      .addOrderBy('al.createdAt', 'DESC');

    if (opts.artistId) qb.andWhere('al.artistId = :artistId', { artistId: opts.artistId });
    if (opts.q && opts.q.trim()) {
      const q = `%${opts.q.trim()}%`;
      qb.andWhere('(al.title ILIKE :q OR a.name ILIKE :q)', { q });
    }
    if (!opts.includeUnpublished) {
      qb.andWhere('al.isPublished = true');
    }
    qb.take(Math.min(opts.limit || 50, 200)).skip(opts.offset || 0);

    const [items, total] = await qb.getManyAndCount();
    return {
      items: items.map(a => ({
        id: a.id,
        title: a.title,
        description: a.description,
        coverUrl: a.coverUrl,
        releaseYear: a.releaseYear,
        isPublished: a.isPublished,
        artistId: a.artistId,
        artistName: a.artist?.name,
      })),
      total,
    };
  }

  async findOne(id: string, includeUnpublished = false) {
    const album = await this.albumRepo.findOne({
      where: { id },
      relations: { artist: true },
    });
    if (!album) throw new NotFoundException('Album not found');
    if (!includeUnpublished && !album.isPublished) throw new NotFoundException('Album not found');

    const songs = await this.songRepo.find({
      where: { albumId: id, ...(includeUnpublished ? {} : { isPublished: true }) },
      order: { position: 'ASC', releasedAt: 'DESC' },
    });

    return {
      id: album.id,
      title: album.title,
      description: album.description,
      coverUrl: album.coverUrl,
      releaseYear: album.releaseYear,
      isPublished: album.isPublished,
      licenseStatus: album.licenseStatus,
      licenseNote: album.licenseNote,
      artistId: album.artistId,
      artistName: album.artist?.name,
      songs: songs.map(s => ({
        id: s.id,
        title: s.title,
        durationSec: s.durationSec,
        audioUrl: s.audioUrl,
        imageUrl: s.imageUrl,
        position: s.position,
        playCount: s.playCount,
        isPublished: s.isPublished,
      })),
    };
  }

  async create(dto: any) {
    const artist = await this.artistRepo.findOne({ where: { id: dto.artistId } });
    if (!artist) throw new NotFoundException('Artist not found');

    const album = this.albumRepo.create({
      title: dto.title,
      description: dto.description,
      coverUrl: dto.coverUrl,
      coverPath: dto.coverPath,
      releaseYear: dto.releaseYear || new Date().getFullYear(),
      artistId: dto.artistId,
      isPublished: dto.isPublished !== false,
      licenseStatus: dto.licenseStatus || 'owned',
      licenseNote: dto.licenseNote,
    });
    await this.albumRepo.save(album);
    return this.toPublic(album, artist);
  }

  async update(id: string, dto: any) {
    const album = await this.albumRepo.findOne({ where: { id }, relations: { artist: true } });
    if (!album) throw new NotFoundException('Album not found');

    if (dto.title !== undefined) album.title = dto.title;
    if (dto.description !== undefined) album.description = dto.description;
    if (dto.coverUrl !== undefined) album.coverUrl = dto.coverUrl;
    if (dto.coverPath !== undefined) album.coverPath = dto.coverPath;
    if (dto.releaseYear !== undefined) album.releaseYear = dto.releaseYear;
    if (dto.isPublished !== undefined) album.isPublished = dto.isPublished;
    if (dto.licenseStatus !== undefined) album.licenseStatus = dto.licenseStatus;
    if (dto.licenseNote !== undefined) album.licenseNote = dto.licenseNote;

    await this.albumRepo.save(album);
    return this.toPublic(album, album.artist);
  }

  async remove(id: string) {
    const album = await this.albumRepo.findOne({ where: { id } });
    if (!album) throw new NotFoundException('Album not found');
    const songs = await this.songRepo.find({ where: { albumId: id } });
    for (const s of songs) {
      if (s.audioPath) await this.storage.deleteFile(s.audioPath);
    }
    if (album.coverPath) await this.storage.deleteFile(album.coverPath);
    await this.albumRepo.remove(album);
    return { success: true };
  }

  private toPublic(a: Album, artist: Artist) {
    return {
      id: a.id,
      title: a.title,
      description: a.description,
      coverUrl: a.coverUrl,
      releaseYear: a.releaseYear,
      isPublished: a.isPublished,
      artistId: a.artistId,
      artistName: artist?.name,
    };
  }
}