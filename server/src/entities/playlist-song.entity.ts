import { Entity, PrimaryGeneratedColumn, Column, ManyToOne, JoinColumn, PrimaryColumn } from 'typeorm';
import { Playlist } from './playlist.entity';
import { Song } from './song.entity';

/** Join table: official playlist <-> song (ordered) */
@Entity('playlist_songs')
export class PlaylistSong {
  @PrimaryColumn()
  playlistId: string;

  @PrimaryColumn()
  songId: string;

  @Column({ default: 1 })
  position: number;

  @ManyToOne(() => Playlist, (p) => p.songs, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'playlistId' })
  playlist: Playlist;

  @ManyToOne(() => Song, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'songId' })
  song: Song;
}
