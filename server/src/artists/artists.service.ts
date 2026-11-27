import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Like, ILike, In } from 'typeorm';
import { Artist } from '../entities/artist.entity';
import { Album } from '../entities/album.entity';
import { Song } from '../entities/song.entity';
import { StorageService } from '../media/storage.service';

@Injectable()
export class ArtistsService {
  constructor(
    @InjectRepository(Artist) private artistRepo: Repository<Artist>,
    @InjectRepository(Album) private albumRepo: Repository<Album>,
    @InjectRepository(Song) private songRepo: Repository<Song>,
    private storage: StorageService,
  ) {}

  async findAll(opts: { q?: string; limit?: number; offset?: number; includeUnpublished?: boolean }) {
    const qb = this.artistRepo.createQueryBuilder('a')
      .leftJoinAndSelect('a.albums', 'al', 'al.isPublished = true')
      .orderBy('a.playCount', 'DESC')
      .addOrderBy('a.createdAt', 'DESC');

    if (opts.q && opts.q.trim()) {
      const q = `%${opts.q.trim()}%`;
      qb.andWhere('(a.name ILIKE :q OR a.nameEn ILIKE :q OR a.bio ILIKE :q)', { q });
    }
    if (!opts.includeUnpublished) {
      qb.andWhere('a.isPublished = true');
    }
    qb.take(Math.min(opts.limit || 50, 200)).skip(opts.offset || 0);

    const [items, total] = await qb.getManyAndCount();
    return { items: items.map(a => this.toPublic(a)), total };
  }

  async findOne(id: string, includeUnpublished = false) {
    const artist = await this.artistRepo.findOne({ where: { id } });
    if (!artist) throw new NotFoundException('Artist not found');
    if (!includeUnpublished && !artist.isPublished) throw new NotFoundException('Artist not found');

    const songs = await this.songRepo.find({
      where: { artistId: id, ...(includeUnpublished ? {} : { isPublished: true }) },
      relations: { album: true },
      order: { position: 'ASC', releasedAt: 'DESC' },
    });
    const albums = await this.albumRepo.find({
      where: { artistId: id, ...(includeUnpublished ? {} : { isPublished: true }) },
      order: { releaseYear: 'DESC' },
    });
    const playCount = await this.songRepo
      .createQueryBuilder('s')
      .select('COALESCE(SUM(s.playCount), 0)', 'total')
      .where('s.artistId = :id', { id })
      .getRawOne();

    return {
      ...this.toPublic(artist),
      songs: songs.map(s => ({
        id: s.id,
        title: s.title,
        durationSec: s.durationSec,
        audioUrl: s.audioUrl,
        imageUrl: s.imageUrl || this.artistImage(s),
        playCount: s.playCount,
        downloadCount: s.downloadCount,
        isPublished: s.isPublished,
        releasedAt: s.releasedAt,
        albumId: s.albumId,
        albumTitle: s.album?.title,
      })),
      albums: albums.map(a => ({
        id: a.id,
        title: a.title,
        coverUrl: a.coverUrl,
        releaseYear: a.releaseYear,
        description: a.description,
        isPublished: a.isPublished,
      })),
      totalPlays: Number(playCount?.total || 0),
    };
  }

  async create(dto: any) {
    const artist = this.artistRepo.create({
      name: dto.name,
      nameEn: dto.nameEn,
      bio: dto.bio,
      image: dto.imagePath ?? dto.image,
      imageUrl: dto.imageUrl,
      isPublished: dto.isPublished !== false,
      licenseStatus: dto.licenseStatus || 'owned',
      licenseNote: dto.licenseNote,
    });
    await this.artistRepo.save(artist);
    return this.toPublic(artist);
  }

  async update(id: string, dto: any) {
    const artist = await this.artistRepo.findOne({ where: { id } });
    if (!artist) throw new NotFoundException('Artist not found');

    if (dto.name !== undefined) artist.name = dto.name;
    if (dto.nameEn !== undefined) artist.nameEn = dto.nameEn;
    if (dto.bio !== undefined) artist.bio = dto.bio;
    if (dto.imageUrl !== undefined) artist.imageUrl = dto.imageUrl;
    if (dto.imagePath !== undefined) artist.image = dto.imagePath;
    if (dto.isPublished !== undefined) artist.isPublished = dto.isPublished;
    if (dto.licenseStatus !== undefined) artist.licenseStatus = dto.licenseStatus;
    if (dto.licenseNote !== undefined) artist.licenseNote = dto.licenseNote;

    await this.artistRepo.save(artist);
    return this.toPublic(artist);
  }

  async remove(id: string) {
    const artist = await this.artistRepo.findOne({ where: { id } });
    if (!artist) throw new NotFoundException('Artist not found');
    const songs = await this.songRepo.find({ where: { artistId: id } });
    for (const s of songs) {
      if (s.audioPath) await this.storage.deleteFile(s.audioPath);
      if (s.imagePath) await this.storage.deleteFile(s.imagePath);
    }
    if (artist.image) await this.storage.deleteFile(artist.image);
    await this.artistRepo.remove(artist);
    return { success: true };
  }

  artistImage(song: Song): string | null {
    return null; // replaced by relation loading in caller when needed
  }

  toPublic(a: Artist) {
    return {
      id: a.id,
      name: a.name,
      nameEn: a.nameEn,
      bio: a.bio,
      imageUrl: a.imageUrl,
      isPublished: a.isPublished,
      licenseStatus: a.licenseStatus,
      licenseNote: a.licenseNote,
      playCount: a.playCount,
      createdAt: a.createdAt,
      albums: a.albums?.map(al => ({
        id: al.id, title: al.title, coverUrl: al.coverUrl, releaseYear: al.releaseYear,
      })),
    };
  }
}
