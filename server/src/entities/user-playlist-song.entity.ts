import { Entity, PrimaryColumn, Column, ManyToOne, JoinColumn, PrimaryGeneratedColumn } from 'typeorm';
import { UserPlaylist } from './user-playlist.entity';
import { Song } from './song.entity';

/** Join table: user playlist <-> song (ordered) */
@Entity('user_playlist_songs')
export class UserPlaylistSong {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @PrimaryColumn()
  playlistId: string;

  @PrimaryColumn()
  songId: string;

  @Column({ default: 1 })
  position: number;

  @ManyToOne(() => UserPlaylist, (p) => p.songs, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'playlistId' })
  playlist: UserPlaylist;

  @ManyToOne(() => Song, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'songId' })
  song: Song;
}
