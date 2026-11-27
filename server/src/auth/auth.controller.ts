import { Controller, Post, Get, Body, UseGuards, Request } from '@nestjs/common';
import { AuthService } from './auth.service';
import { JwtAuthGuard } from './jwt-auth.guard';

@Controller('auth')
export class AuthController {
  constructor(private auth: AuthService) {}

  @Post('register')
  register(@Body() dto: { email: string; password: string; name: string }) {
    return this.auth.register(dto);
  }

  @Post('login')
  login(@Body() dto: { email: string; password: string }) {
    return this.auth.login(dto.email, dto.password);
  }

  @UseGuards(JwtAuthGuard)
  @Get('profile')
  profile(@Request() req: any) {
    return this.auth.getProfile(req.user.email);
  }

  @UseGuards(JwtAuthGuard)
  @Post('change-password')
  changePassword(@Request() req: any, @Body() dto: { oldPassword: string; newPassword: string }) {
    return this.auth.changePassword(req.user.email, dto.oldPassword, dto.newPassword);
  }
}