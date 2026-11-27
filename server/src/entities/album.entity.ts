import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn,
  OneToMany, ManyToOne, JoinColumn,
} from 'typeorm';
import { Artist } from './artist.entity';
import { Song } from './song.entity';

@Entity('albums')
export class Album {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ length: 160 })
  title: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ default: 0 })
  releaseYear: number;

  @Column({ type: 'text', nullable: true })
  coverUrl: string;

  @Column({ type: 'text', nullable: true })
  coverPath: string;

  @Column({ default: true })
  isPublished: boolean;

  @Column({ default: 'owned' })
  licenseStatus: string;

  @Column({ type: 'text', nullable: true })
  licenseNote: string;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;

  @ManyToOne(() => Artist, (a) => a.albums, { onDelete: 'CASCADE', nullable: false })
  @JoinColumn({ name: 'artistId' })
  artist: Artist;

  @Column()
  artistId: string;

  @OneToMany(() => Song, (s) => s.album)
  songs: Song[];
}
