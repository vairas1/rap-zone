import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn,
  OneToMany, Index,
} from 'typeorm';
import { Album } from './album.entity';

@Entity('artists')
export class Artist {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Index()
  @Column({ length: 120 })
  name: string;

  @Column({ length: 120, nullable: true })
  nameEn: string;

  @Column({ type: 'text', nullable: true })
  bio: string;

  /** Local storage path or remote URL of the profile image */
  @Column({ type: 'text', nullable: true })
  image: string;

  /** /media/... or https://... */
  @Column({ type: 'text', nullable: true })
  imageUrl: string;

  @Column({ default: true })
  isPublished: boolean;

  /** Rights/licensing declaration (content policy) */
  @Column({ default: 'owned' })
  licenseStatus: string;

  @Column({ type: 'text', nullable: true })
  licenseNote: string;

  @Column({ default: 0 })
  playCount: number;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;

  @OneToMany(() => Album, (a) => a.artist)
  albums: Album[];
}
