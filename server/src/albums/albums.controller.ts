import {
  Controller, Get, Post, Patch, Delete, Param, Body, Query,
  UseGuards, ParseUUIDPipe,
} from '@nestjs/common';
import { AlbumsService } from './albums.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { RolesGuard, Roles } from '../common/roles.guard';

@Controller('albums')
export class AlbumsController {
  constructor(private albums: AlbumsService) {}

  @Get()
  findAll(
    @Query('artistId') artistId: string,
    @Query('q') q: string,
    @Query('limit') limit: string,
    @Query('offset') offset: string,
  ) {
    return this.albums.findAll({
      artistId, q, limit: limit ? parseInt(limit, 10) : undefined,
      offset: offset ? parseInt(offset, 10) : undefined,
    });
  }

  @Get(':id')
  findOne(@Param('id', ParseUUIDPipe) id: string) {
    return this.albums.findOne(id);
  }

  @Post()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  create(@Body() dto: any) {
    return this.albums.create(dto);
  }

  @Patch(':id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  update(@Param('id', ParseUUIDPipe) id: string, @Body() dto: any) {
    return this.albums.update(id, dto);
  }

  @Delete(':id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  remove(@Param('id', ParseUUIDPipe) id: string) {
    return this.albums.remove(id);
  }
}