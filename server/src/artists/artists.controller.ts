import {
  Controller, Get, Post, Patch, Delete, Param, Body, Query,
  UseGuards, ParseUUIDPipe,
} from '@nestjs/common';
import { ArtistsService } from './artists.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { RolesGuard, Roles } from '../common/roles.guard';

@Controller('artists')
export class ArtistsController {
  constructor(private artists: ArtistsService) {}

  // Public endpoints
  @Get()
  findAll(@Query('q') q: string, @Query('limit') limit: string, @Query('offset') offset: string) {
    return this.artists.findAll({
      q, limit: limit ? parseInt(limit, 10) : undefined, offset: offset ? parseInt(offset, 10) : undefined,
    });
  }

  @Get(':id')
  findOne(@Param('id', ParseUUIDPipe) id: string) {
    return this.artists.findOne(id);
  }

  // Admin endpoints
  @Post()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  create(@Body() dto: any) {
    return this.artists.create(dto);
  }

  @Patch(':id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  update(@Param('id', ParseUUIDPipe) id: string, @Body() dto: any) {
    return this.artists.update(id, dto);
  }

  @Delete(':id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  remove(@Param('id', ParseUUIDPipe) id: string) {
    return this.artists.remove(id);
  }
}
