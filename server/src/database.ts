import 'reflect-metadata';
import * as path from 'path';
import * as fs from 'fs';
import * as crypto from 'crypto';

let embeddedCleanup: (() => Promise<void>) | null = null;

export async function bootstrapDatabase(): Promise<{ cleanup?: () => Promise<void> }> {
  // If USE_EMBEDDED_DB is explicitly true, use embedded postgres
  if (process.env.USE_EMBEDDED_DB === 'true') {
    const EmbeddedPostgres = (await import('embedded-postgres')).default;
    const dataDir = process.env.EMBEDDED_PG_DATA || path.join(process.cwd(), '.pgdata');
    const password = process.env.DB_PASSWORD || 'postgres';

    fs.mkdirSync(dataDir, { recursive: true });

    const pg = new EmbeddedPostgres({
      user: 'postgres',
      password,
      port: 5432,
      persistent: true,
      databaseDir: dataDir,
    });

    const isInitialized = fs.existsSync(path.join(dataDir, 'PG_VERSION'));
    if (!isInitialized) {
      console.log('[db] First run: initializing embedded postgres cluster in', dataDir);
      await pg.initialise();
    } else {
      console.log('[db] Starting existing embedded postgres cluster from', dataDir);
    }

    await pg.start();

    const dbName = process.env.DB_NAME || 'rapzone';
    try {
      await pg.createDatabase(dbName);
      console.log('[db] Created database', dbName);
    } catch (e: any) {
      if (String(e.message || e).includes('already exists')) {
        console.log('[db] Database', dbName, 'already exists');
      } else {
        throw e;
      }
    }

    embeddedCleanup = async () => {
      try { await pg.stop(); } catch { /* ignore */ }
    };

    return { cleanup: embeddedCleanup };
  }

  // Default: no embedded database, assume external PostgreSQL
  console.log('[db] Using external PostgreSQL at', process.env.DB_HOST || 'localhost');
  return {};
}

export function sha256File(file: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const h = crypto.createHash('sha256');
    const s = fs.createReadStream(file);
    s.on('data', (d) => h.update(d));
    s.on('end', () => resolve(h.digest('hex')));
    s.on('error', reject);
  });
}