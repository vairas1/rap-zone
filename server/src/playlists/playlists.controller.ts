import {
  Controller, Get, Post, Patch, Delete, Param, Body, UseGuards, ParseUUIDPipe,
} from '@nestjs/common';
import { PlaylistsService } from './playlists.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { RolesGuard, Roles } from '../common/roles.guard';

@Controller('playlists')
export class PlaylistsController {
  constructor(private playlists: PlaylistsService) {}

  @Get()
  findAll() {
    return this.playlists.findAll();
  }

  @Get(':id')
  findOne(@Param('id', ParseUUIDPipe) id: string) {
    return this.playlists.findOne(id);
  }

  @Post()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  create(@Body() dto: any) {
    return this.playlists.create(dto);
  }

  @Patch(':id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  update(@Param('id', ParseUUIDPipe) id: string, @Body() dto: any) {
    return this.playlists.update(id, dto);
  }

  @Delete(':id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  remove(@Param('id', ParseUUIDPipe) id: string) {
    return this.playlists.remove(id);
  }

  @Post(':id/songs')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  addSong(@Param('id', ParseUUIDPipe) id: string, @Body() body: { songId: string }) {
    return this.playlists.addSong(id, body.songId);
  }

  @Delete(':id/songs/:songId')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  removeSong(@Param('id', ParseUUIDPipe) id: string, @Param('songId') songId: string) {
    return this.playlists.removeSong(id, songId);
  }
}