function digest_(value) {
  return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, value, Utilities.Charset.UTF_8)
    .map(b => ('0' + ((b + 256) % 256).toString(16)).slice(-2)).join('');
}

function equal_(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string') return false;
  let mismatch = a.length ^ b.length;
  for (let i = 0; i < Math.max(a.length, b.length); i++) mismatch |= (a.charCodeAt(i) || 0) ^ (b.charCodeAt(i) || 0);
  return mismatch === 0;
}

function tablet_(request) {
  if (!/^[a-f0-9]{64}$/.test(request.deviceId || '') || !/^[A-Za-z0-9_-]{43}$/.test(request.tabletToken || '')) fail_('DEVICE_REVOKED');
  const p = props_(), expectedHash = digest_(request.tabletToken);
  const key = 'TABLET_' + request.deviceId;
  const tablet = JSON.parse(p.getProperty(key) || '{}');

  // Normal operation: both stable device fingerprint and secret token must match.
  if (Object.keys(tablet).length && tablet.revokedExplicitly === true) fail_('DEVICE_REVOKED');
  if (Object.keys(tablet).length && tablet.active === true && equal_(tablet.tokenHash, expectedHash)) return tablet;
  // A recovery may never overwrite another provisioned device's secret, even when
  // the owner has accidentally approved an already occupied destination fingerprint.
  if (Object.keys(tablet).length && !equal_(tablet.tokenHash, expectedHash)) fail_('DEVICE_REVOKED');

  // Token continuity identifies a *recoverable* signing-key/ANDROID_ID change, but never
  // grants permission to migrate a tablet by itself. The owner must approve the exact
  // destination fingerprint in the script editor before any registration is moved.
  const all = p.getProperties();
  const candidates = Object.keys(all).filter(k => /^TABLET_[a-f0-9]{64}$/.test(k)).map(k => {
    let value = {}; try { value = JSON.parse(all[k] || '{}'); } catch (_) { return null; }
    return { key: k, id: k.slice(7), value: value };
  }).filter(x => x && x.id !== request.deviceId && x.value.active === true && equal_(x.value.tokenHash, expectedHash));

  if (candidates.length !== 1) fail_('DEVICE_REVOKED');
  const allowedTo = p.getProperty('TABLET_REBIND_TO') || '';
  const until = Number(p.getProperty('TABLET_REBIND_UNTIL') || '0');
  const now = Date.now();
  if (allowedTo !== request.deviceId || !Number.isFinite(until) || now >= until) fail_('DEVICE_REBIND_REQUIRED');

  const previous = candidates[0];
  previous.value.active = false;
  previous.value.migratedTo = request.deviceId;
  previous.value.migratedAt = now;
  previous.value.scheme = previous.value.scheme || 'legacy-random-v1';
  p.setProperty(previous.key, JSON.stringify(previous.value));
  p.deleteProperty('SESSION_' + previous.id);

  const migrated = {
    tokenHash: expectedHash,
    active: true,
    scheme: 'android-id-v2',
    migratedFrom: previous.id,
    migratedAt: now
  };
  p.setProperty(key, JSON.stringify(migrated));
  p.deleteProperty('SESSION_' + request.deviceId);
  p.deleteProperty('TABLET_REBIND_TO');
  p.deleteProperty('TABLET_REBIND_UNTIL');
  appendAudit_(auditEvent_('SYSTEM', 'TABLET_ID_MIGRATED', '', request.deviceId));
  return migrated;
}

// A PRF over a monotonically increasing counter, seeded with 256 bits from Node crypto.randomBytes.
// All callers hold the script lock. No Math.random or undocumented UUID entropy is used for secrets.
function randomToken_() {
  const p = props_(), key = p.getProperty('SERVER_RANDOM_KEY');
  if (!/^[a-f0-9]{64}$/.test(key || '')) fail_('SERVICE_ERROR');
  const counter = Number(p.getProperty('RANDOM_COUNTER') || '0') + 1;
  if (!Number.isSafeInteger(counter)) fail_('SERVICE_ERROR');
  p.setProperty('RANDOM_COUNTER', String(counter));
  return Utilities.base64EncodeWebSafe(Utilities.computeHmacSha256Signature('gbo-admin-v1:' + counter, key, Utilities.Charset.UTF_8)).replace(/=+$/, '');
}

function validateCredential_(credential) {
  keys_(credential, ['salt', 'pinHash', 'hashIterations']);
  if (credential.hashIterations !== 210000 || typeof credential.salt !== 'string' || typeof credential.pinHash !== 'string' ||
      !/^[A-Za-z0-9+/]{43}=$/.test(credential.salt) || !/^[A-Za-z0-9+/]{43}=$/.test(credential.pinHash) ||
      Utilities.base64Decode(credential.salt).length !== 32 || Utilities.base64Decode(credential.pinHash).length !== 32) fail_('INVALID_REQUEST');
  return credential;
}

function verifyPin_(pin, user) {
  const result = CryptoJS.PBKDF2(pin, CryptoJS.enc.Base64.parse(user.salt), {
    keySize: 8, iterations: user.hashIterations, hasher: CryptoJS.algo.SHA256
  }).toString(CryptoJS.enc.Base64);
  return equal_(result, user.pinHash);
}
