import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn,
  OneToMany, Index,
} from 'typeorm';
import { Favorite } from './favorite.entity';
import { UserPlaylist } from './user-playlist.entity';

@Entity('users')
export class User {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Index({ unique: true })
  @Column({ length: 100 })
  email: string;

  @Column({ length: 60 })
  name: string;

  /** bcrypt hash */
  @Column({ length: 120, select: false })
  passwordHash: string;

  @Column({ default: 'user' })
  role: string;

  @Column({ default: true })
  isActive: boolean;

  @Column({ default: 'ar' })
  language: string;

  @Column({ type: 'text', nullable: true })
  bio: string;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;

  @OneToMany(() => Favorite, (f) => f.user)
  favorites: Favorite[];

  @OneToMany(() => UserPlaylist, (p) => p.owner)
  playlists: UserPlaylist[];
}
