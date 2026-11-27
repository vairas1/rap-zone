import { Entity, PrimaryColumn, PrimaryGeneratedColumn, CreateDateColumn, Column, ManyToOne, JoinColumn, Index } from 'typeorm';
import { User } from './user.entity';
import { Song } from './song.entity';

/** User favorites (synced across devices via server) */
@Entity('favorites')
export class Favorite {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Index()
  @Column({ length: 100 })
  userEmail: string;

  @Index()
  @Column()
  songId: string;

  @CreateDateColumn()
  addedAt: Date;

  @ManyToOne(() => User, (u) => u.favorites, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'userEmail', referencedColumnName: 'email' })
  user: User;

  @ManyToOne(() => Song, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'songId' })
  song: Song;
}