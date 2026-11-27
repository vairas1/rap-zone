import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn,
  ManyToOne, JoinColumn, Index,
} from 'typeorm';
import { Album } from './album.entity';
import { Artist } from './artist.entity';
import { MediaAsset } from './media-asset.entity';

@Entity('songs')
export class Song {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ length: 160 })
  title: string;

  @Column({ type: 'text', nullable: true })
  lyrics: string;

  @Column()
  durationSec: number;

  @Column()
  audioUrl: string;

  @Column({ type: 'text', nullable: true })
  audioPath: string;

  @Column({ type: 'text', nullable: true })
  imageUrl: string;

  @Column({ type: 'text', nullable: true })
  imagePath: string;

  @Index()
  @Column({ default: 0 })
  playCount: number;

  @Column({ default: 0 })
  downloadCount: number;

  @Column({ default: 1 })
  position: number;

  @Index()
  @Column({ default: true })
  isPublished: boolean;

  @Column({ default: 'owned' })
  licenseStatus: string;

  @Column({ type: 'text', nullable: true })
  licenseNote: string;

  @CreateDateColumn()
  releasedAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;

  @ManyToOne(() => Artist, { onDelete: 'CASCADE', nullable: false })
  @JoinColumn({ name: 'artistId' })
  artist: Artist;

  @Column()
  artistId: string;

  @ManyToOne(() => Album, (a) => a.songs, { onDelete: 'CASCADE', nullable: false })
  @JoinColumn({ name: 'albumId' })
  album: Album;

  @Column()
  albumId: string;

  @ManyToOne(() => MediaAsset, { onDelete: 'SET NULL', nullable: true })
  @JoinColumn({ name: 'assetId' })
  asset: MediaAsset;

  @Column({ nullable: true })
  assetId: string;
}
