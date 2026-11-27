import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn, OneToMany,
} from 'typeorm';
import { PlaylistSong } from './playlist-song.entity';

/** Official curated playlists created by admins */
@Entity('playlists')
export class Playlist {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ length: 160 })
  name: string;

  @Column({ length: 160, nullable: true })
  nameEn: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  /** /media/... or https://... */
  @Column({ type: 'text', nullable: true })
  imageUrl: string;

  @Column({ type: 'text', nullable: true })
  imagePath: string;

  @Column({ default: true })
  isPublished: boolean;

  @Column({ default: 0 })
  position: number;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;

  @OneToMany(() => PlaylistSong, (ps) => ps.playlist)
  songs: PlaylistSong[];
}
