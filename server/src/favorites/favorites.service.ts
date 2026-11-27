import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Favorite } from '../entities/favorite.entity';
import { Song } from '../entities/song.entity';

@Injectable()
export class FavoritesService {
  constructor(
    @InjectRepository(Favorite) private favRepo: Repository<Favorite>,
    @InjectRepository(Song) private songRepo: Repository<Song>,
  ) {}

  async list(userEmail: string) {
    const favs = await this.favRepo.find({
      where: { userEmail },
      relations: { song: { artist: true, album: true } },
      order: { addedAt: 'DESC' },
    });
    return {
      items: favs.map(f => ({
        id: f.song?.id,
        title: f.song?.title,
        durationSec: f.song?.durationSec,
        audioUrl: f.song?.audioUrl,
        imageUrl: f.song?.imageUrl,
        playCount: f.song?.playCount,
        artistId: f.song?.artist?.id,
        artistName: f.song?.artist?.name,
        albumId: f.song?.albumId,
        albumTitle: f.song?.album?.title,
        artistImageUrl: f.song?.artist?.imageUrl,
        addedAt: f.addedAt,
      })).filter(x => x.id),
      total: favs.length,
    };
  }

  async add(userEmail: string, songId: string) {
    const song = await this.songRepo.findOne({ where: { id: songId } });
    if (!song) throw new Error('Song not found');
    const existing = await this.favRepo.findOne({ where: { userEmail, songId } });
    if (existing) return { success: true, alreadyExists: true };
    await this.favRepo.save(this.favRepo.create({ userEmail, songId }));
    return { success: true };
  }

  async remove(userEmail: string, songId: string) {
    await this.favRepo.delete({ userEmail, songId });
    return { success: true };
  }

  async songIds(userEmail: string): Promise<string[]> {
    const favs = await this.favRepo.find({ where: { userEmail } });
    return favs.map(f => f.songId);
  }
}