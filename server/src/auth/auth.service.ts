import { Injectable, UnauthorizedException, ConflictException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcryptjs';
import { User } from '../entities/user.entity';

@Injectable()
export class AuthService {
  constructor(
    @InjectRepository(User) private userRepo: Repository<User>,
    private jwt: JwtService,
  ) {}

  async register(dto: { email: string; password: string; name: string }) {
    const exists = await this.userRepo.findOne({ where: { email: dto.email } });
    if (exists) throw new ConflictException('Email already registered');
    const passwordHash = await bcrypt.hash(dto.password, 10);
    const user = this.userRepo.create({ email: dto.email, name: dto.name, passwordHash });
    await this.userRepo.save(user);
    return this.makeToken(user);
  }

  async login(email: string, password: string) {
    const user = await this.userRepo.findOne({ where: { email }, select: ['id', 'email', 'passwordHash', 'role', 'name'] });
    if (!user || !(await bcrypt.compare(password, user.passwordHash))) {
      throw new UnauthorizedException('Invalid credentials');
    }
    return this.makeToken(user);
  }

  private makeToken(user: User) {
    const payload = { sub: user.id, email: user.email, role: user.role };
    return { accessToken: this.jwt.sign(payload), user: { id: user.id, email: user.email, role: user.role } };
  }

  async validateUser(email: string): Promise<User | null> {
    return this.userRepo.findOne({ where: { email, isActive: true } });
  }

  async getProfile(email: string) {
    const user = await this.userRepo.findOne({ where: { email } });
    if (!user) return null;
    const { passwordHash, ...rest } = user;
    return rest;
  }

  async changePassword(email: string, oldPassword: string, newPassword: string) {
    const user = await this.userRepo.findOne({ where: { email }, select: ['id', 'passwordHash'] });
    if (!user || !(await bcrypt.compare(oldPassword, user.passwordHash))) {
      throw new UnauthorizedException('Invalid old password');
    }
    user.passwordHash = await bcrypt.hash(newPassword, 10);
    await this.userRepo.save(user);
    return { success: true };
  }

  async createAdmin(email: string, password: string, name: string) {
    const existing = await this.userRepo.findOne({ where: { email } });
    if (existing) return existing;
    const passwordHash = await bcrypt.hash(password, 10);
    const admin = this.userRepo.create({ email, name, passwordHash, role: 'admin' });
    return this.userRepo.save(admin);
  }
}