import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { UserPlaylist } from '../entities/user-playlist.entity';
import { UserPlaylistSong } from '../entities/user-playlist-song.entity';
import { Song } from '../entities/song.entity';
import { User } from '../entities/user.entity';

@Injectable()
export class UserPlaylistsService {
  constructor(
    @InjectRepository(UserPlaylist) private plRepo: Repository<UserPlaylist>,
    @InjectRepository(UserPlaylistSong) private psRepo: Repository<UserPlaylistSong>,
    @InjectRepository(Song) private songRepo: Repository<Song>,
    @InjectRepository(User) private userRepo: Repository<User>,
  ) {}

  async list(ownerEmail: string) {
    const playlists = await this.plRepo.find({
      where: { ownerEmail },
      order: { createdAt: 'DESC' },
    });
    const items = await Promise.all(playlists.map(p => this.withSongs(p)));
    return { items, total: items.length };
  }

  async create(ownerEmail: string, dto: any) {
    const owner = await this.userRepo.findOne({ where: { email: ownerEmail } });
    if (!owner) throw new NotFoundException('User not found');
    const playlist = this.plRepo.create({
      name: dto.name,
      description: dto.description,
      isPublic: !!dto.isPublic,
      ownerEmail,
      ownerId: owner.id,
    });
    await this.plRepo.save(playlist);
    if (Array.isArray(dto.songIds)) await this.setSongs(playlist.id, dto.songIds);
    return this.withSongs(playlist);
  }

  async update(id: string, ownerEmail: string, dto: any) {
    const playlist = await this.plRepo.findOne({ where: { id, ownerEmail } });
    if (!playlist) throw new NotFoundException('Playlist not found');
    if (dto.name !== undefined) playlist.name = dto.name;
    if (dto.description !== undefined) playlist.description = dto.description;
    if (dto.isPublic !== undefined) playlist.isPublic = dto.isPublic;
    await this.plRepo.save(playlist);
    if (Array.isArray(dto.songIds)) await this.setSongs(id, dto.songIds);
    return this.withSongs(playlist);
  }

  async remove(id: string, ownerEmail: string) {
    const playlist = await this.plRepo.findOne({ where: { id, ownerEmail } });
    if (!playlist) throw new NotFoundException('Playlist not found');
    await this.plRepo.remove(playlist);
    return { success: true };
  }

  async addSong(id: string, ownerEmail: string, songId: string) {
    const playlist = await this.plRepo.findOne({ where: { id, ownerEmail } });
    if (!playlist) throw new NotFoundException('Playlist not found');
    const song = await this.songRepo.findOne({ where: { id: songId } });
    if (!song) throw new NotFoundException('Song not found');
    const existing = await this.psRepo.findOne({ where: { playlistId: id, songId } });
    if (existing) return { success: true, alreadyExists: true };
    const max = await this.psRepo.createQueryBuilder('ps')
      .select('MAX(ps.position)', 'max').where('ps.playlistId = :id', { id }).getRawOne();
    await this.psRepo.save(this.psRepo.create({ playlistId: id, songId, position: (max?.max || 0) + 1 }));
    return { success: true };
  }

  async removeSong(id: string, ownerEmail: string, songId: string) {
    const playlist = await this.plRepo.findOne({ where: { id, ownerEmail } });
    if (!playlist) throw new NotFoundException('Playlist not found');
    await this.psRepo.delete({ playlistId: id, songId });
    return { success: true };
  }

  private async setSongs(playlistId: string, songIds: string[]) {
    await this.psRepo.delete({ playlistId });
    let pos = 1;
    for (const songId of songIds) {
      const song = await this.songRepo.findOne({ where: { id: songId } });
      if (!song) continue;
      await this.psRepo.save(this.psRepo.create({ playlistId, songId, position: pos++ }));
    }
  }

  private async withSongs(p: UserPlaylist) {
    const links = await this.psRepo.find({
      where: { playlistId: p.id },
      relations: { song: { artist: true, album: true } },
      order: { position: 'ASC' },
    });
    return {
      id: p.id,
      name: p.name,
      description: p.description,
      isPublic: p.isPublic,
      createdAt: p.createdAt,
      songs: links.map(l => ({
        id: l.song?.id,
        title: l.song?.title,
        durationSec: l.song?.durationSec,
        audioUrl: l.song?.audioUrl,
        imageUrl: l.song?.imageUrl,
        artistId: l.song?.artist?.id,
        artistName: l.song?.artist?.name,
        albumId: l.song?.albumId,
        albumTitle: l.song?.album?.title,
        artistImageUrl: l.song?.artist?.imageUrl,
      })).filter(s => s.id),
    };
  }
}