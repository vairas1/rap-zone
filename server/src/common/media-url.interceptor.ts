import { CallHandler, ExecutionContext, Injectable, NestInterceptor } from '@nestjs/common';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ConfigService } from '@nestjs/config';

/**
 * Rewrites relative media paths ("/media/...") into absolute URLs so the
 * Android app and web panel can load them directly.
 *
 * Base URL resolution order:
 * 1. PUBLIC_BASE_URL env (explicit, e.g. for splitting API and media hosts)
 * 2. The Host header of the incoming request (works on Render/LAN with zero config)
 */
@Injectable()
export class MediaUrlInterceptor implements NestInterceptor {
  private base: string;

  constructor(cfg: ConfigService) {
    this.base = (cfg.get('PUBLIC_BASE_URL') || '').replace(/\/+$/, '');
  }

  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    return next.handle().pipe(
      map((data) => {
        if (this.base) return this.walk(data, this.base);
        // Fallback: use the host that asked us, with https for public hosts
        const req: any = context.switchToHttp().getRequest();
        const host: string = req?.get ? req.get('host') : req?.headers?.host || '';
        if (!host) return data;
        const base = host.startsWith('localhost') || host.startsWith('127.') || host.startsWith('192.168.') || host.startsWith('10.')
          ? `http://${host}`
          : `https://${host}`;
        return this.walk(data, base);
      })
    );
  }

  private walk(value: any, base: string): any {
    if (typeof value === 'string') {
      if (base && value.startsWith('/media/')) return `${base}${value}`;
      return value;
    }
    if (Array.isArray(value)) return value.map(v => this.walk(v, base));
    if (value && typeof value === 'object') {
      // Do not touch Date / Buffer / class instances with toJSON
      if (value instanceof Date || Buffer.isBuffer(value)) return value;
      const out: any = {};
      for (const key of Object.keys(value)) out[key] = this.walk(value[key], base);
      return out;
    }
    return value;
  }
}
