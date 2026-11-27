import {
  Controller, Get, Post, Patch, Delete, Param, Body, Query, UseGuards, Request,
} from '@nestjs/common';
import { StatsService } from './stats.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { RolesGuard, Roles } from '../common/roles.guard';

@Controller('stats')
export class StatsController {
  constructor(private stats: StatsService) {}

  // Admin dashboard
  @Get('dashboard')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  dashboard() {
    return this.stats.dashboard();
  }

  // Listening history: user's own history when authenticated, global feed otherwise
  @Get('history')
  history(@Request() req: any, @Query('limit') limit: string) {
    const userEmail = req.user?.email;
    return this.stats.history(userEmail, limit ? parseInt(limit, 10) : 50);
  }

  // ===== Admin user management =====
  @Get('users')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  listUsers() {
    return this.stats.listUsers();
  }

  @Post('users')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  createUser(@Body() dto: any) {
    return this.stats.createUser(dto);
  }

  @Patch('users/:id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  updateUser(@Param('id') id: string, @Body() dto: any) {
    return this.stats.updateUser(id, dto);
  }

  @Delete('users/:id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('admin')
  deleteUser(@Param('id') id: string) {
    return this.stats.deleteUser(id);
  }
}
