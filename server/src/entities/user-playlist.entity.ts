import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn,
  ManyToOne, JoinColumn, Index, OneToMany,
} from 'typeorm';
import { User } from './user.entity';
import { UserPlaylistSong } from './user-playlist-song.entity';

/** Personal playlists created by users */
@Entity('user_playlists')
export class UserPlaylist {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ length: 160 })
  name: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ default: false })
  isPublic: boolean;

  @Index({ unique: true })
  @Column()
  ownerEmail: string;

  @ManyToOne(() => User, (u) => u.playlists, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'ownerId', referencedColumnName: 'id' })
  owner: User;

  @Column()
  ownerId: string;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;

  @OneToMany(() => UserPlaylistSong, (ps) => ps.playlist)
  songs: UserPlaylistSong[];
}
