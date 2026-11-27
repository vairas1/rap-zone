import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, Index, ManyToOne, JoinColumn } from 'typeorm';
import { Song } from './song.entity';

/**
 * Listening events, used for:
 *  - per-user listening history
 *  - play statistics (artists/songs)
 */
@Entity('play_events')
export class PlayEvent {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Index()
  @Column({ length: 100, nullable: true })
  userEmail: string;

  @Index()
  @Column()
  songId: string;

  @Column({ default: 0 })
  artistId: string;

  /** 0 = guest / unknown */
  @Column({ default: 0 })
  durationSec: number;

  @CreateDateColumn()
  at: Date;

  @ManyToOne(() => Song, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'songId' })
  song: Song;
}