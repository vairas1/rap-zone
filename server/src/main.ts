import { NestFactory } from '@nestjs/core';
import { ValidationPipe, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import helmet from 'helmet';
import { AppModule } from './app.module';
import { bootstrapDatabase } from './database';
import { MediaUrlInterceptor } from './common/media-url.interceptor';

async function bootstrap() {
  // Start database (embedded postgres or external) before anything else.
  const db = await bootstrapDatabase();
  const logger = new Logger('Bootstrap');

  // Give embedded postgres a moment to fully accept connections
  if (process.env.USE_EMBEDDED_DB === 'true') {
    await new Promise(r => setTimeout(r, 2000));
  }

  const app = await NestFactory.create(AppModule, { logger: ['error', 'warn', 'log'] });

  app.use(helmet({
    crossOriginResourcePolicy: { policy: 'cross-origin' },
    contentSecurityPolicy: false,
  }));

  const corsOrigins = (process.env.CORS_ORIGINS || 'http://localhost:8080')
    .split(',').map(s => s.trim()).filter(Boolean);
  app.enableCors({ origin: corsOrigins, credentials: true });

  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));

  // Rewrite relative media paths to absolute URLs.
  app.useGlobalInterceptors(new MediaUrlInterceptor(app.get(ConfigService)));

  const port = parseInt(process.env.PORT || '4000', 10);
  await app.listen(port, '0.0.0.0');
  logger.log(`Rap Zone API listening on http://0.0.0.0:${port}`);
  if (db.cleanup) {
    // cleanup on exit
    for (const sig of ['SIGINT', 'SIGTERM']) {
      process.on(sig, () => {
        app.close().finally(() => db.cleanup().finally(() => process.exit(0)));
      });
    }
  }
}

bootstrap().catch((e) => {
  console.error('FATAL bootstrap error:', e);
  process.exit(1);
});
