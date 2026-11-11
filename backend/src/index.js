import { b64u, randId, hashPassword, safeEq, jwtSign, jwtVerify, aesEncrypt, aesDecrypt, hmac, deriveVideoKey, signLicense } from './crypto.js';

const now = () => Math.floor(Date.now() / 1000);
const json = (data, status = 200, headers = {}) => new Response(JSON.stringify(data), { status, headers: { 'content-type': 'application/json', ...headers } });
const err = (code, status = 400) => json({ error: code }, status);
const body = async (req) => { try { return await req.json(); } catch { return {}; } };
const ip = (req) => req.headers.get('CF-Connecting-IP') || '';

async function rateLimit(env, key, max = 5, windowSec = 900) {
  const r = await env.DB.prepare('SELECT count, reset_at FROM login_attempts WHERE key=?').bind(key).first();
  if (r && r.reset_at > now() && r.count >= max) return false;
  if (!r || r.reset_at <= now()) await env.DB.prepare('INSERT OR REPLACE INTO login_attempts VALUES (?,1,?)').bind(key, now() + windowSec).run();
  else await env.DB.prepare('UPDATE login_attempts SET count=count+1 WHERE key=?').bind(key).run();
  return true;
}
const clearAttempts = (env, key) => env.DB.prepare('DELETE FROM login_attempts WHERE key=?').bind(key).run();

// از لینک آپارات (aparat.com/v/XXXX یا embed) یا خود هش، هش را برمی‌گرداند
function aparatHash(v) {
  v = String(v || '').trim(); if (!v) return '';
  const m = v.match(/aparat\.com\/(?:v|video\/video\/embed\/videohash)\/([A-Za-z0-9]+)/);
  if (m) return m[1];
  return /^[A-Za-z0-9]+$/.test(v) ? v : '';
}

// ---------- کاربر ----------
async function authUser(req, env) {
  const t = (req.headers.get('Authorization') || '').replace('Bearer ', '');
  const c = t && await jwtVerify(t, env.JWT_SECRET);
  if (!c || c.role !== 'user') return { error: err('UNAUTHORIZED', 401) };
  const u = await env.DB.prepare('SELECT * FROM users WHERE id=?').bind(c.uid).first();
  if (!u || !u.is_active) return { error: err('BANNED', 403) };
  if (u.expires_at < now()) return { error: err('EXPIRED', 403) };
  const d = await env.DB.prepare('SELECT device_id FROM devices WHERE user_id=?').bind(u.id).first();
  if (!d || d.device_id !== c.did) return { error: err('DEVICE_MISMATCH', 403) };
  await env.DB.prepare('UPDATE devices SET last_seen=? WHERE user_id=?').bind(now(), u.id).run();
  return { user: u, deviceId: c.did };
}

async function makeSession(env, u, deviceId) {
  const token = await jwtSign({ uid: u.id, did: deviceId, role: 'user' }, env.JWT_SECRET, 30 * 86400);
  const exp = Math.min(u.expires_at, now() + u.offline_days * 86400);
  const license = await signLicense(env.LICENSE_PRIVATE_KEY, { uid: u.id, did: deviceId, iat: now(), exp, sub_exp: u.expires_at });
  return { token, license, server_time: now() };
}

async function login(req, env) {
  const b = await body(req);
  if (!b.phone || !b.password || !b.device_id) return err('BAD_REQUEST');
  const rk = 'u:' + b.phone + ':' + ip(req);
  if (!(await rateLimit(env, rk))) return err('TOO_MANY_ATTEMPTS', 429);
  const u = await env.DB.prepare('SELECT * FROM users WHERE phone=?').bind(b.phone).first();
  if (!u || !safeEq(await hashPassword(b.password, u.salt), u.password_hash)) return err('INVALID_CREDENTIALS', 401);
  if (!u.is_active) return err('BANNED', 403);
  if (u.expires_at < now()) return err('EXPIRED', 403);
  const d = await env.DB.prepare('SELECT device_id FROM devices WHERE user_id=?').bind(u.id).first();
  if (d && d.device_id !== b.device_id) return err('DEVICE_MISMATCH', 403);
  const session = await makeSession(env, u, b.device_id); // اول جلسه ساخته شود؛ اگر خطا داد دستگاه ثبت نشود
  if (!d) await env.DB.prepare('INSERT INTO devices VALUES (?,?,?,?,?)').bind(u.id, b.device_id, b.model || '', now(), now()).run();
  await clearAttempts(env, rk);
  return json(session);
}

async function licenseRefresh(req, env) {
  const a = await authUser(req, env); if (a.error) return a.error;
  return json(await makeSession(env, a.user, a.deviceId));
}

async function listPlaylists(req, env) {
  const a = await authUser(req, env); if (a.error) return a.error;
  const { results } = await env.DB.prepare('SELECT p.*, (SELECT COUNT(*) FROM videos v WHERE v.playlist_id=p.id) AS video_count FROM playlists p ORDER BY sort_order').all();
  return json(results);
}
async function getPlaylist(req, env, id) {
  const a = await authUser(req, env); if (a.error) return a.error;
  const { results } = await env.DB.prepare('SELECT id,title,position,duration_sec,size_bytes,version,source_type FROM videos WHERE playlist_id=? ORDER BY position').bind(id).all();
  return json({ id, videos: results });
}
async function videoSource(req, env, id) {
  const a = await authUser(req, env); if (a.error) return a.error;
  const v = await env.DB.prepare('SELECT source_url, source_type, aparat_hash, version FROM videos WHERE id=?').bind(id).first();
  if (!v || !v.source_url) return err('NOT_FOUND', 404);
  return json({ url: v.source_url, type: v.source_type, aparat_hash: v.aparat_hash, version: v.version });
}
async function videoKey(req, env, id) {
  const a = await authUser(req, env); if (a.error) return a.error;
  return json({ key: await deriveVideoKey(env.MASTER_KEY, a.user.id, a.deviceId, id) });
}

// ---------- ادمین ----------
async function authAdmin(req, env) {
  const m = (req.headers.get('Cookie') || '').match(/adm=([^;]+)/);
  const c = m && await jwtVerify(m[1], env.JWT_SECRET);
  if (!c || c.role !== 'admin') return { error: err('UNAUTHORIZED', 401) };
  if (req.method !== 'GET' && req.headers.get('X-Requested-With') !== 'vidapp') return { error: err('CSRF', 403) };
  return { admin: c };
}
const audit = (env, req, admin, action, target, detail = '') =>
  env.DB.prepare('INSERT INTO audit_log (admin_id,action,target_id,detail,ip,created_at) VALUES (?,?,?,?,?,?)').bind(admin, action, target, detail, ip(req), now()).run();

async function adminSetup(req, env) { // یک بار برای ساخت اولین ادمین
  const b = await body(req);
  if (!env.SETUP_TOKEN || !safeEq(b.setup_token || '', env.SETUP_TOKEN)) return err('FORBIDDEN', 403);
  const c = await env.DB.prepare('SELECT COUNT(*) n FROM admins').first();
  if (c.n > 0) return err('ALREADY_SETUP', 403);
  const salt = randId();
  await env.DB.prepare('INSERT INTO admins (id,username,password_hash,salt) VALUES (?,?,?,?)').bind(randId(), b.username, await hashPassword(b.password, salt), salt).run();
  return json({ ok: true });
}
async function adminLogin(req, env) {
  const b = await body(req);
  const rk = 'a:' + ip(req);
  if (!(await rateLimit(env, rk, 5, 900))) return err('TOO_MANY_ATTEMPTS', 429);
  const a = await env.DB.prepare('SELECT * FROM admins WHERE username=?').bind(b.username || '').first();
  if (!a || !safeEq(await hashPassword(b.password || '', a.salt), a.password_hash)) return err('INVALID_CREDENTIALS', 401);
  await clearAttempts(env, rk);
  await audit(env, req, a.id, 'admin_login', a.id);
  const t = await jwtSign({ aid: a.id, role: 'admin' }, env.JWT_SECRET, 8 * 3600);
  return json({ ok: true }, 200, { 'Set-Cookie': `adm=${t}; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=28800` });
}

async function adminRoute(req, env, path) {
  const a = await authAdmin(req, env); if (a.error) return a.error;
  const aid = a.admin.aid, m = req.method;
  const url = new URL(req.url);

  if (path === '/admin/api/users' && m === 'GET') {
    const q = (url.searchParams.get('q') || '').trim(), st = url.searchParams.get('status') || '';
    const page = Math.max(0, +url.searchParams.get('page') || 0);
    let where = [], args = [];
    if (q) { where.push('(full_name LIKE ? OR phone LIKE ? OR national_id_hash=?)'); args.push(`%${q}%`, `%${q}%`, await hmac(env.NID_KEY, q)); }
    if (st === 'active') where.push(`is_active=1 AND expires_at>${now()}`);
    if (st === 'expired') where.push(`expires_at<=${now()}`);
    if (st === 'banned') where.push('is_active=0');
    const sql = `SELECT u.id,full_name,phone,national_id_mask,is_active,expires_at,offline_days,device_resets,note,created_at,d.model,d.last_seen
      FROM users u LEFT JOIN devices d ON d.user_id=u.id ${where.length ? 'WHERE ' + where.join(' AND ') : ''} ORDER BY created_at DESC LIMIT 50 OFFSET ${page * 50}`;
    return json((await env.DB.prepare(sql).bind(...args).all()).results);
  }
  if (path === '/admin/api/users' && m === 'POST') {
    const b = await body(req);
    if (!b.phone || !b.password || !b.days) return err('BAD_REQUEST');
    const id = randId(), salt = randId(), nid = (b.national_id || '').replace(/\D/g, '');
    try {
      await env.DB.prepare('INSERT INTO users (id,full_name,phone,password_hash,salt,national_id_enc,national_id_hash,national_id_mask,expires_at,offline_days,note,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)')
        .bind(id, b.full_name || '', b.phone, await hashPassword(b.password, salt), salt,
          nid ? await aesEncrypt(env.NID_KEY, nid) : null, nid ? await hmac(env.NID_KEY, nid) : null,
          nid ? nid.slice(0, 3) + '****' + nid.slice(-3) : null, now() + b.days * 86400, b.offline_days || 14, b.note || '', now()).run();
    } catch { return err('DUPLICATE_PHONE_OR_NID', 409); }
    await audit(env, req, aid, 'create_user', id);
    return json({ id });
  }
  let mm = path.match(/^\/admin\/api\/users\/([^/]+)\/(extend|ban|unban|reset-device|national-id|password)$/);
  if (mm) {
    const [, id, act] = mm;
    const u = await env.DB.prepare('SELECT * FROM users WHERE id=?').bind(id).first();
    if (!u) return err('NOT_FOUND', 404);
    if (act === 'national-id') {
      await audit(env, req, aid, 'view_national_id', id);
      return json({ national_id: u.national_id_enc ? await aesDecrypt(env.NID_KEY, u.national_id_enc) : null });
    }
    const b = await body(req);
    if (act === 'extend') { // days می‌تواند منفی باشد
      const base = Math.max(u.expires_at, now());
      await env.DB.prepare('UPDATE users SET expires_at=? WHERE id=?').bind(b.days < 0 ? u.expires_at + b.days * 86400 : base + b.days * 86400, id).run();
    }
    if (act === 'ban') await env.DB.prepare('UPDATE users SET is_active=0 WHERE id=?').bind(id).run();
    if (act === 'unban') await env.DB.prepare('UPDATE users SET is_active=1 WHERE id=?').bind(id).run();
    if (act === 'reset-device') {
      await env.DB.prepare('DELETE FROM devices WHERE user_id=?').bind(id).run();
      await env.DB.prepare('UPDATE users SET device_resets=device_resets+1 WHERE id=?').bind(id).run();
    }
    if (act === 'password') {
      const salt = randId();
      await env.DB.prepare('UPDATE users SET password_hash=?, salt=? WHERE id=?').bind(await hashPassword(b.password, salt), salt, id).run();
    }
    await audit(env, req, aid, act, id, JSON.stringify(b.password ? {} : b));
    return json({ ok: true });
  }
  if (path === '/admin/api/users/offline' && m === 'POST') {
    const b = await body(req);
    await env.DB.prepare('UPDATE users SET offline_days=? WHERE id=?').bind(b.offline_days, b.id).run();
    return json({ ok: true });
  }
  if (path === '/admin/api/playlists') {
    if (m === 'GET') return json((await env.DB.prepare('SELECT * FROM playlists ORDER BY sort_order').all()).results);
    const b = await body(req); const id = b.id || randId(8);
    await env.DB.prepare('INSERT OR REPLACE INTO playlists VALUES (?,?,?,?,?)').bind(id, b.title, b.cover_url || null, b.sort_order || 0, now()).run();
    await audit(env, req, aid, 'save_playlist', id); return json({ id });
  }
  if (path === '/admin/api/videos') {
    if (m === 'GET') return json((await env.DB.prepare('SELECT * FROM videos WHERE playlist_id=? ORDER BY position').bind(url.searchParams.get('playlist_id')).all()).results);
    const b = await body(req); const id = b.id || randId(8);
    const hash = aparatHash(b.aparat_url || b.aparat_hash);
    if (!b.playlist_id || !b.title || !hash) return err('BAD_REQUEST');
    await env.DB.prepare('INSERT OR REPLACE INTO videos (id,playlist_id,title,position,duration_sec,aparat_hash,source_url,source_type,version) VALUES (?,?,?,?,?,?,?,?,?)')
      .bind(id, b.playlist_id, b.title, b.position || 0, b.duration_sec || null, hash, b.source_url || null, b.source_type || 'mp4', b.version || 1).run();
    await audit(env, req, aid, 'save_video', id); return json({ id, aparat_hash: hash });
  }
  if ((mm = path.match(/^\/admin\/api\/(videos|playlists)\/([^/]+)\/delete$/)) && m === 'POST') {
    if (mm[1] === 'videos') await env.DB.prepare('DELETE FROM videos WHERE id=?').bind(mm[2]).run();
    else { await env.DB.prepare('DELETE FROM videos WHERE playlist_id=?').bind(mm[2]).run(); await env.DB.prepare('DELETE FROM playlists WHERE id=?').bind(mm[2]).run(); }
    await audit(env, req, aid, 'delete_' + mm[1], mm[2]); return json({ ok: true });
  }
  if (path === '/admin/api/audit') return json((await env.DB.prepare('SELECT * FROM audit_log ORDER BY id DESC LIMIT 200').all()).results);
  if (path === '/admin/api/stats') {
    const r = await env.DB.prepare(`SELECT COUNT(*) total, SUM(is_active=1 AND expires_at>?) active, SUM(expires_at<=?) expired, SUM(is_active=1 AND expires_at BETWEEN ? AND ?) soon FROM users`).bind(now(), now(), now(), now() + 7 * 86400).first();
    return json(r);
  }
  return err('NOT_FOUND', 404);
}

export default {
  async fetch(req, env) {
    const path = new URL(req.url).pathname, m = req.method;
    try {
      if (path === '/auth/login' && m === 'POST') return login(req, env);
      if (path === '/license/refresh' && m === 'POST') return licenseRefresh(req, env);
      if (path === '/playlists' && m === 'GET') return listPlaylists(req, env);
      let x;
      if ((x = path.match(/^\/playlists\/([^/]+)$/))) return getPlaylist(req, env, x[1]);
      if ((x = path.match(/^\/videos\/([^/]+)\/source$/))) return videoSource(req, env, x[1]);
      if ((x = path.match(/^\/videos\/([^/]+)\/key$/))) return videoKey(req, env, x[1]);
      if (path === '/admin/api/setup' && m === 'POST') return adminSetup(req, env);
      if (path === '/admin/api/login' && m === 'POST') return adminLogin(req, env);
      if (path.startsWith('/admin/api/')) return adminRoute(req, env, path);
      return env.ASSETS.fetch(req);
    } catch (e) { console.error(e && e.stack || e); return json({ error: 'SERVER_ERROR', detail: String(e && e.message || e).slice(0, 200) }, 500); }
  },
  async scheduled(event, env) {
    // TODO: Sync لینک‌های آپارات (مرحله بعد)
  },
};
