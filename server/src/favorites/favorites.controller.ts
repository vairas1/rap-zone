import {
  Controller, Get, Post, Delete, Param, Body, UseGuards, Request,
} from '@nestjs/common';
import { FavoritesService } from './favorites.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@Controller('favorites')
@UseGuards(JwtAuthGuard)
export class FavoritesController {
  constructor(private favorites: FavoritesService) {}

  @Get()
  list(@Request() req: any) {
    return this.favorites.list(req.user.email);
  }

  @Post(':songId')
  add(@Request() req: any, @Param('songId') songId: string) {
    return this.favorites.add(req.user.email, songId);
  }

  @Delete(':songId')
  remove(@Request() req: any, @Param('songId') songId: string) {
    return this.favorites.remove(req.user.email, songId);
  }
}