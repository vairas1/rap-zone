import { readFileSync, existsSync } from 'fs';
import { join } from 'path';

export function loadConfig() {
  const envFile = join(process.cwd(), '.env');
  if (!existsSync(envFile)) {
    return {};
  }
  const content = readFileSync(envFile, 'utf-8');
  const config: Record<string, string> = {};
  for (const line of content.split('\n')) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eq = trimmed.indexOf('=');
    if (eq > 0) {
      const key = trimmed.slice(0, eq).trim();
      const val = trimmed.slice(eq + 1).trim();
      config[key] = val;
    }
  }
  return config;
}

export const ConfigModule = { load: loadConfig };