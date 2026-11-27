import {
  Controller, Get, Post, Patch, Delete, Param, Body, Query,
  UseGuards, ParseUUIDPipe, Request,
} from '@nestjs/common';
import { SongsService } from './songs.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { RolesGuard, Roles } from '../common/roles.guard';

@Controller('songs')
export class SongsController {
  constructor(private songs: SongsService) {}

  @Get()
  findAll(
    @Query('artistId') artistId: string,
    @Query('albumId') albumId: string,
    @Query('q') q: string,
    @Query('limit') limit: string,
    @Query('offset') offset: string,
    @Query('sortBy') sortBy: string,
  ) {
    return this.songs.findAll({
      artistId, albumId, q,
      limit: limit ? parseInt(limit, 10) : undefined,
      offset: offset ? parseInt(offset, 10) : undefined,
      sortBy: sortBy as any,
    });
  }

  @Get('popular')
  findPopular(@Query('limit') limit: string) {
    return this.songs.findAll({ limit: limit ? parseInt(limit, 10) : 20, sortBy: 'popular' });
  }

  @Get('latest')
  findLatest(@Query('limit') limit: string) {
    return this.songs.findAll({ limit: limit ? parseInt(limit, 10) : 20, sortBy: 'latest' });
  }

  @Get(':id')
  findOne(@Param('id', ParseUUIDPipe) id: string) {
    return this.songs.findOne(id);
  }

  // Play tracking (may be called by unauthenticated users as well)
  @Post(':id/play')
  recordPlay(@Param('id', ParseUUIDPipe) id: string, @Body() body: { userEmail?: string; durationSec?: number }) {
    return this.songs.recordPlay(id, body.userEmail, body.durationSec || 0);
  }

  @Post(':id/download')
  recordDownload(@Param('id', ParseUUIDPipe) id: string) {
    return this.songs.recordDownload(id);
  }

  // Admin only
  @Post()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  create(@Body() dto: any) {
    return this.songs.create(dto);
  }

  @Patch(':id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  update(@Param('id', ParseUUIDPipe) id: string, @Body() dto: any) {
    return this.songs.update(id, dto);
  }

  @Delete(':id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  remove(@Param('id', ParseUUIDPipe) id: string) {
    return this.songs.remove(id);
  }
}