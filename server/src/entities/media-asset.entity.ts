import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn } from 'typeorm';

/**
 * Tracks an uploaded media file (mp3 / image) for verification & content policy:
 * checksum, declared size, mime type and the admin who uploaded it.
 */
@Entity('media_assets')
export class MediaAsset {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  /** sha256 of the file content */
  @Column({ length: 64 })
  checksum: string;

  @Column()
  sizeBytes: number;

  @Column({ length: 100 })
  mime: string;

  @Column()
  kind: string; // 'audio' | 'image'

  /** storage path relative to media root */
  @Column()
  path: string;

  /** /media/... */
  @Column()
  publicUrl: string;

  @Column({ length: 100, nullable: true })
  uploadedByEmail: string;

  @CreateDateColumn()
  uploadedAt: Date;
}
