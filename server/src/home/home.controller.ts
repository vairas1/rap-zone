import { Controller, Get, Query } from '@nestjs/common';
import { HomeService } from './home.service';

@Controller('home')
export class HomeController {
  constructor(private home: HomeService) {}

  @Get()
  feed() {
    return this.home.feed();
  }

  @Get('search')
  search(@Query('q') q: string, @Query('limit') limit: string) {
    return this.home.search(q, limit ? parseInt(limit, 10) : 20);
  }
}
