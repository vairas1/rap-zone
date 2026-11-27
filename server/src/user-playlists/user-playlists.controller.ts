import {
  Controller, Get, Post, Patch, Delete, Param, Body, UseGuards, Request,
} from '@nestjs/common';
import { UserPlaylistsService } from './user-playlists.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@Controller('me/playlists')
@UseGuards(JwtAuthGuard)
export class UserPlaylistsController {
  constructor(private playlists: UserPlaylistsService) {}

  @Get()
  list(@Request() req: any) {
    return this.playlists.list(req.user.email);
  }

  @Post()
  create(@Request() req: any, @Body() dto: any) {
    return this.playlists.create(req.user.email, dto);
  }

  @Patch(':id')
  update(@Request() req: any, @Param('id') id: string, @Body() dto: any) {
    return this.playlists.update(id, req.user.email, dto);
  }

  @Delete(':id')
  remove(@Request() req: any, @Param('id') id: string) {
    return this.playlists.remove(id, req.user.email);
  }

  @Post(':id/songs')
  addSong(@Request() req: any, @Param('id') id: string, @Body() body: { songId: string }) {
    return this.playlists.addSong(id, req.user.email, body.songId);
  }

  @Delete(':id/songs/:songId')
  removeSong(@Request() req: any, @Param('id') id: string, @Param('songId') songId: string) {
    return this.playlists.removeSong(id, req.user.email, songId);
  }
}