import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Playlist } from '../entities/playlist.entity';
import { PlaylistSong } from '../entities/playlist-song.entity';
import { Song } from '../entities/song.entity';
import { StorageService } from '../media/storage.service';

@Injectable()
export class PlaylistsService {
  constructor(
    @InjectRepository(Playlist) private playlistRepo: Repository<Playlist>,
    @InjectRepository(PlaylistSong) private psRepo: Repository<PlaylistSong>,
    @InjectRepository(Song) private songRepo: Repository<Song>,
    private storage: StorageService,
  ) {}

  async findAll(includeUnpublished = false) {
    const qb = this.playlistRepo.createQueryBuilder('p')
      .orderBy('p.position', 'ASC').addOrderBy('p.createdAt', 'DESC');
    if (!includeUnpublished) qb.andWhere('p.isPublished = true');
    const items = await qb.getMany();
    return { items: items.map(p => this.toPublic(p)), total: items.length };
  }

  async findOne(id: string, includeUnpublished = false) {
    const playlist = await this.playlistRepo.findOne({ where: { id } });
    if (!playlist) throw new NotFoundException('Playlist not found');
    if (!includeUnpublished && !playlist.isPublished) throw new NotFoundException('Playlist not found');

    const links = await this.psRepo.find({
      where: { playlistId: id },
      relations: { song: { artist: true, album: true } },
      order: { position: 'ASC' },
    });
    return {
      ...this.toPublic(playlist),
      songs: links.map(l => this.songToPublic(l.song)),
    };
  }

  async create(dto: any) {
    const playlist = this.playlistRepo.create({
      name: dto.name,
      nameEn: dto.nameEn,
      description: dto.description,
      imageUrl: dto.imageUrl,
      imagePath: dto.imagePath,
      isPublished: dto.isPublished !== false,
      position: dto.position || 0,
    });
    await this.playlistRepo.save(playlist);
    if (Array.isArray(dto.songIds)) {
      await this.setSongs(playlist.id, dto.songIds);
    }
    return this.toPublic(playlist);
  }

  async update(id: string, dto: any) {
    const playlist = await this.playlistRepo.findOne({ where: { id } });
    if (!playlist) throw new NotFoundException('Playlist not found');
    if (dto.name !== undefined) playlist.name = dto.name;
    if (dto.nameEn !== undefined) playlist.nameEn = dto.nameEn;
    if (dto.description !== undefined) playlist.description = dto.description;
    if (dto.imageUrl !== undefined) playlist.imageUrl = dto.imageUrl;
    if (dto.imagePath !== undefined) playlist.imagePath = dto.imagePath;
    if (dto.isPublished !== undefined) playlist.isPublished = dto.isPublished;
    if (dto.position !== undefined) playlist.position = dto.position;
    await this.playlistRepo.save(playlist);
    if (Array.isArray(dto.songIds)) await this.setSongs(id, dto.songIds);
    return this.toPublic(playlist);
  }

  async remove(id: string) {
    const playlist = await this.playlistRepo.findOne({ where: { id } });
    if (!playlist) throw new NotFoundException('Playlist not found');
    if (playlist.imagePath) await this.storage.deleteFile(playlist.imagePath);
    await this.playlistRepo.remove(playlist);
    return { success: true };
  }

  async setSongs(playlistId: string, songIds: string[]) {
    await this.psRepo.delete({ playlistId });
    let pos = 1;
    for (const songId of songIds) {
      const song = await this.songRepo.findOne({ where: { id: songId } });
      if (!song) continue;
      await this.psRepo.save(this.psRepo.create({ playlistId, songId, position: pos++ }));
    }
    return { count: pos - 1 };
  }

  async addSong(playlistId: string, songId: string) {
    const playlist = await this.playlistRepo.findOne({ where: { id: playlistId } });
    if (!playlist) throw new NotFoundException('Playlist not found');
    const song = await this.songRepo.findOne({ where: { id: songId } });
    if (!song) throw new NotFoundException('Song not found');
    const existing = await this.psRepo.findOne({ where: { playlistId, songId } });
    if (existing) return { success: true, alreadyExists: true };
    const max = await this.psRepo
      .createQueryBuilder('ps')
      .select('MAX(ps.position)', 'max')
      .where('ps.playlistId = :playlistId', { playlistId })
      .getRawOne();
    await this.psRepo.save(this.psRepo.create({
      playlistId, songId, position: (max?.max || 0) + 1,
    }));
    return { success: true };
  }

  async removeSong(playlistId: string, songId: string) {
    await this.psRepo.delete({ playlistId, songId });
    return { success: true };
  }

  toPublic(p: Playlist) {
    return {
      id: p.id,
      name: p.name,
      nameEn: p.nameEn,
      description: p.description,
      imageUrl: p.imageUrl,
      isPublished: p.isPublished,
      position: p.position,
      createdAt: p.createdAt,
    };
  }

  songToPublic(s: any) {
    if (!s) return null;
    return {
      id: s.id,
      title: s.title,
      durationSec: s.durationSec,
      audioUrl: s.audioUrl,
      imageUrl: s.imageUrl,
      playCount: s.playCount,
      artistId: s.artist?.id,
      artistName: s.artist?.name,
      albumId: s.albumId,
      albumTitle: s.album?.title,
      artistImageUrl: s.artist?.imageUrl,
    };
  }
}