import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcryptjs';
import { User } from '../entities/user.entity';
import { Song } from '../entities/song.entity';
import { Artist } from '../entities/artist.entity';
import { Album } from '../entities/album.entity';
import { PlayEvent } from '../entities/play-event.entity';

type UserRole = 'user' | 'admin';

@Injectable()
export class StatsService {
  constructor(
    @InjectRepository(User) private userRepo: Repository<User>,
    @InjectRepository(Song) private songRepo: Repository<Song>,
    @InjectRepository(Artist) private artistRepo: Repository<Artist>,
    @InjectRepository(Album) private albumRepo: Repository<Album>,
    @InjectRepository(PlayEvent) private eventRepo: Repository<PlayEvent>,
  ) {}

  async dashboard() {
    const [users, artists, albums, songs, publishedSongs] = await Promise.all([
      this.userRepo.count(),
      this.artistRepo.count(),
      this.albumRepo.count(),
      this.songRepo.count(),
      this.songRepo.count({ where: { isPublished: true } }),
    ]);

    const totals = await this.songRepo.createQueryBuilder('s')
      .select('COALESCE(SUM(s.playCount), 0)', 'plays')
      .addSelect('COALESCE(SUM(s.downloadCount), 0)', 'downloads')
      .getRawOne();

    const plays24h = await this.eventRepo
      .createQueryBuilder('e')
      .select('COUNT(*)', 'c')
      .where('e.at > :since', { since: new Date(Date.now() - 24 * 3600 * 1000) })
      .getRawOne();

    const totalPlays = Number(totals?.plays || 0);
    const totalDownloads = Number(totals?.downloads || 0);

    const topSongs = await this.songRepo.find({
      relations: { artist: true },
      order: { playCount: 'DESC' },
      take: 10,
    });
    const topArtists = await this.artistRepo.find({
      order: { playCount: 'DESC' },
      take: 10,
    });

    const daily: Array<{ date: string; plays: number }> = [];
    for (let i = 13; i >= 0; i--) {
      const d = new Date();
      d.setHours(0, 0, 0, 0);
      d.setDate(d.getDate() - i);
      const next = new Date(d);
      next.setDate(next.getDate() + 1);
      const c = await this.eventRepo.createQueryBuilder('e')
        .select('COUNT(*)', 'c')
        .where('e.at >= :from AND e.at < :to', { from: d, to: next })
        .getRawOne();
      daily.push({ date: d.toISOString().slice(0, 10), plays: Number(c?.c || 0) });
    }

    const activeUsers = await this.eventRepo.createQueryBuilder('e')
      .select('COUNT(DISTINCT e.userEmail)', 'c')
      .where('e.userEmail IS NOT NULL')
      .getRawOne();

    return {
      counts: { users, artists, albums, songs, publishedSongs },
      totals: {
        plays: totalPlays,
        downloads: totalDownloads,
        plays24h: Number(plays24h?.c || 0),
        activeUsers: Number(activeUsers?.c || 0),
      },
      daily,
      topSongs: topSongs.map(s => ({
        id: s.id, title: s.title, artistName: s.artist?.name,
        playCount: s.playCount, downloadCount: s.downloadCount,
      })),
      topArtists: topArtists.map(a => ({
        id: a.id, name: a.name, imageUrl: a.imageUrl, playCount: a.playCount,
      })),
    };
  }

  async history(userEmail?: string, limit = 50) {
    const qb = this.eventRepo.createQueryBuilder('e')
      .leftJoinAndSelect('e.song', 's')
      .leftJoinAndSelect('s.artist', 'a')
      .orderBy('e.at', 'DESC')
      .take(Math.min(limit, 200));
    if (userEmail) qb.where('e.userEmail = :userEmail', { userEmail });
    const events = await qb.getMany();
    return {
      items: events.map(e => ({
        id: e.id,
        songId: e.songId,
        title: e.song?.title,
        artistName: e.song?.artist?.name,
        artistId: e.song?.artist?.id,
        artistImageUrl: e.song?.artist?.imageUrl,
        imageUrl: e.song?.imageUrl,
        at: e.at,
        durationSec: e.durationSec,
      })).filter(x => x.songId),
      total: events.length,
    };
  }

  async listUsers() {
    const users = await this.userRepo.find({ order: { createdAt: 'DESC' } });
    return {
      items: users.map(u => ({
        id: u.id, email: u.email, name: u.name, role: u.role,
        isActive: u.isActive, language: u.language, createdAt: u.createdAt,
      })),
      total: users.length,
    };
  }

  async createUser(dto: { email: string; name: string; password: string; role?: UserRole }) {
    const exists = await this.userRepo.findOne({ where: { email: dto.email } });
    if (exists) throw new Error('Email already exists');
    const user = this.userRepo.create({
      email: dto.email,
      name: dto.name,
      passwordHash: await bcrypt.hash(dto.password, 10),
      role: dto.role === 'admin' ? 'admin' : 'user',
    });
    await this.userRepo.save(user);
    return { id: user.id, email: user.email, name: user.name, role: user.role };
  }

  async updateUser(id: string, dto: any) {
    const user = await this.userRepo.findOne({ where: { id } });
    if (!user) throw new NotFoundException('User not found');
    if (dto.name !== undefined) user.name = dto.name;
    if (dto.role !== undefined) user.role = dto.role === 'admin' ? 'admin' : 'user';
    if (dto.isActive !== undefined) user.isActive = dto.isActive;
    if (dto.password) user.passwordHash = await bcrypt.hash(dto.password, 10);
    await this.userRepo.save(user);
    return { id: user.id, email: user.email, name: user.name, role: user.role, isActive: user.isActive };
  }

  async deleteUser(id: string) {
    const user = await this.userRepo.findOne({ where: { id } });
    if (!user) throw new NotFoundException('User not found');
    await this.userRepo.remove(user);
    return { success: true };
  }
}