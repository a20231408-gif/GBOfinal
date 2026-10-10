'use strict';
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '..');
let clock = 1_800_000_000_000;
const properties = new Map();
let sheets = [];
const propService = {
  getProperty: key => properties.get(key) ?? null,
  setProperty: (key, value) => { properties.set(key, value); },
  deleteProperty: key => { properties.delete(key); },
  getProperties: () => Object.fromEntries(properties)
};
function getSheet(name) {
  const sheet = sheets.find(s => s.name === name);
  if (!sheet) return null;
  return {
    getSheetId: () => sheet.id,
    getLastRow: () => sheet.rows.length,
    getRange: (row, column, count = 1, cols = 1) => {
      if (row === 'A1') { row = 1; column = 1; }
      return {
        isBlank: () => sheet.rows[row - 1]?.[column - 1] == null,
        setValue: value => { sheet.rows[row - 1] ??= []; sheet.rows[row - 1][column - 1] = value; },
        getValue: () => sheet.rows[row - 1]?.[column - 1],
        getValues: () => Array.from({ length: count }, (_, i) => Array.from({ length: cols }, (_, j) => sheet.rows[row + i - 1]?.[column + j - 1] ?? ''))
      };
    }
  };
}
const book = { getId: () => 'private-test-sheet', getSheetByName: getSheet,
  insertSheet: name => { sheets.push({ id: sheets.length + 1, name, rows: [] }); return getSheet(name); } };
function batchUpdate(batch) {
  const next = structuredClone(sheets);
  for (const request of batch.requests) {
    const change = request.updateCells || request.appendCells;
    const sheet = next.find(s => s.id === (change.sheetId || change.start.sheetId));
    const start = change.start?.rowIndex ?? sheet.rows.length;
    change.rows.forEach((row, index) => { sheet.rows[start + index] = row.values.map(cell => cell.userEnteredValue.stringValue ?? cell.userEnteredValue.numberValue); });
  }
  sheets = next;
}
const context = vm.createContext({
  Date: class extends Date { static now() { return clock; } },
  PropertiesService: { getScriptProperties: () => propService },
  LockService: { getScriptLock: () => ({ tryLock: () => true, waitLock() {}, releaseLock() {} }) },
  SpreadsheetApp: { openById: () => book, flush() {} },
  Sheets: { Spreadsheets: { batchUpdate } },
  Utilities: {
    getUuid: crypto.randomUUID,
    DigestAlgorithm: { SHA_256: 'sha256' }, Charset: { UTF_8: 'utf8' },
    computeDigest: (algorithm, value) => [...crypto.createHash(algorithm).update(value).digest()],
    computeHmacSha256Signature: (value, key) => [...crypto.createHmac('sha256', key).update(value).digest()],
    base64EncodeWebSafe: bytes => Buffer.from(bytes).toString('base64url'),
    base64Decode: value => [...Buffer.from(value, 'base64')]
  }
});
for (const file of ['Crypto.gs', 'Code.gs', 'Users.gs', 'Security.gs', 'Auth.gs', 'Audit.gs', 'TabletAdmin.gs']) vm.runInContext(fs.readFileSync(path.join(root, file), 'utf8'), context, { filename: file });
const fixturePin = '783294'; // Synthetic test credential, never provisioned.
function credential(pin = fixturePin) {
  const salt = crypto.randomBytes(32);
  return { salt: salt.toString('base64'), pinHash: crypto.pbkdf2Sync(pin, salt, 210000, 32, 'sha256').toString('base64'), hashIterations: 210000 };
}
const deviceId = 'a'.repeat(64), tabletToken = crypto.randomBytes(32).toString('base64url');
properties.set('SERVER_RANDOM_KEY', crypto.randomBytes(32).toString('hex'));
properties.set('USERS_SPREADSHEET_ID', 'private-test-sheet');
properties.set('BOOTSTRAP_ENABLED', 'true');
properties.set('BOOTSTRAP_ADMIN', JSON.stringify({ username: 'OWNER', displayName: 'Owner test', credential: credential() }));
context.setupUserService(); context.bootstrapFirstAdmin();
properties.set('TABLET_PROVISION', JSON.stringify({ deviceId, tokenHash: crypto.createHash('sha256').update(tabletToken).digest('hex') }));
context.provisionTablet();
const request = (action, fields = {}) => context.dispatch_({ action, deviceId, tabletToken, ...fields });
const rejects = (fn, code) => assert.throws(fn, error => error.safeCode === code);
let passed = 0;
function test(name, fn) { fn(); passed++; console.log('PASS ' + name); }
let adminToken, operatorId, ownerId;
test('bootstrap is explicit, central, disabled after use', () => {
  assert.equal(properties.get('BOOTSTRAP_ENABLED'), 'false');
  assert.equal(properties.has('BOOTSTRAP_ADMIN'), false);
  rejects(() => context.bootstrapFirstAdmin(), 'ADMIN_REQUIRED');
});
test('sync changed and no PIN or tokens in response', () => {
  const result = request('syncUsers', { clientVersion: -1 });
  assert.equal(result.changed, true); assert.equal(result.version, 1);
  assert.equal(result.users[0].username, 'owner'); ownerId = result.users[0].id;
  assert.equal('pin' in result.users[0], false); assert.equal('tabletToken' in result, false);
});
test('same version does not download users', () => {
  const result = request('syncUsers', { clientVersion: 1 });
  assert.equal(result.changed, false); assert.equal('users' in result, false);
});
test('invalid tablet token and unknown fields rejected', () => {
  rejects(() => request('syncUsers', { clientVersion: 1, tabletToken: 'X'.repeat(43) }), 'DEVICE_REVOKED');
  rejects(() => request('syncUsers', { clientVersion: 1, role: 'ADMIN' }), 'INVALID_REQUEST');
});
test('PBKDF2 interoperates with native Node and rejects wrong PIN', () => {
  assert.equal(context.verifyPin_(fixturePin, context.readState_().users[0]), true);
  assert.equal(context.verifyPin_('000000', context.readState_().users[0]), false);
});
test('online admin authentication issues short session', () => {
  const result = request('adminAuthenticate', { username: 'OWNER', pin: fixturePin });
  assert.equal(result.expiresInSeconds, 300); assert.match(result.adminToken, /^[A-Za-z0-9_-]{43}$/);
  adminToken = result.adminToken;
});
test('create operator increments version', () => {
  const result = request('createUser', { adminToken, username: 'field.user', displayName: 'Field User', role: 'OPERATOR', credential: credential() });
  assert.equal(result.version, 2); operatorId = result.id;
});
test('duplicate username is case insensitive without version change', () => {
  rejects(() => request('createUser', { adminToken, username: 'FIELD.USER', displayName: 'Other', role: 'OPERATOR', credential: credential() }), 'DUPLICATE_USERNAME');
  assert.equal(context.readState_().version, 2);
});
test('operator cannot authorize administration', () => {
  rejects(() => request('adminAuthenticate', { username: 'field.user', pin: fixturePin }), 'INVALID_CREDENTIALS');
});
test('last admin cannot be disabled or demoted; snapshot unchanged', () => {
  rejects(() => request('setUserActive', { adminToken, id: ownerId, active: false }), 'LAST_ADMIN');
  rejects(() => request('changeUserRole', { adminToken, id: ownerId, role: 'OPERATOR' }), 'LAST_ADMIN');
  const state = context.readState_(); assert.equal(state.version, 2); assert.equal(state.users[0].active, true);
});
test('disable and enable user increments version', () => {
  assert.equal(request('setUserActive', { adminToken, id: operatorId, active: false }).version, 3);
  assert.equal(context.readState_().users[1].active, false);
  assert.equal(request('setUserActive', { adminToken, id: operatorId, active: true }).version, 4);
});
test('role and name changes are central', () => {
  assert.equal(request('changeUserRole', { adminToken, id: operatorId, role: 'ADMIN' }).version, 5);
  assert.equal(request('updateUser', { adminToken, id: operatorId, displayName: '=not-a-formula' }).version, 6);
  assert.equal(context.readState_().users[1].displayName, '=not-a-formula');
});
test('reset PIN replaces verifier', () => {
  const changed = credential('124578');
  assert.equal(request('resetUserPin', { adminToken, id: operatorId, credential: changed }).version, 7);
  assert.equal(context.verifyPin_('124578', context.readState_().users[1]), true);
  assert.equal(context.verifyPin_(fixturePin, context.readState_().users[1]), false);
});
test('central audit does not contain secret material', () => {
  const serialized = JSON.stringify(request('getAudit', { adminToken }));
  for (const secret of [fixturePin, tabletToken, adminToken, 'pinHash', 'salt']) assert.equal(serialized.includes(secret), false);
  assert.equal(serialized.includes('USER_PIN_RESET'), true);
});
test('expired admin session rejected', () => {
  clock += 300001;
  rejects(() => request('setUserActive', { adminToken, id: operatorId, active: false }), 'SESSION_EXPIRED');
});
test('five failed attempts temporarily block administrative reauthentication', () => {
  request('adminAuthenticate', { username: 'owner', pin: fixturePin });
  for (let i = 0; i < 5; i++) rejects(() => request('adminAuthenticate', { username: 'owner', pin: '000000' }), 'INVALID_CREDENTIALS');
  rejects(() => request('adminAuthenticate', { username: 'owner', pin: fixturePin }), 'RATE_LIMITED');
  clock += 300001;
  adminToken = request('adminAuthenticate', { username: 'owner', pin: fixturePin }).adminToken;
});
test('tablet identity change requires owner-approved exact fingerprint before rebind', () => {
  const legacyId = 'b'.repeat(64), stableId = 'c'.repeat(64), nextStableId = 'd'.repeat(64);
  const migrationToken = crypto.randomBytes(32).toString('base64url');
  const tokenHash = crypto.createHash('sha256').update(migrationToken).digest('hex');
  properties.set('TABLET_' + legacyId, JSON.stringify({ tokenHash, active: true }));

  // Even with the secret token, an unapproved fingerprint cannot hijack the binding.
  rejects(() => context.dispatch_({ action: 'syncUsers', deviceId: stableId, tabletToken: migrationToken, clientVersion: 7 }), 'DEVICE_REBIND_REQUIRED');
  assert.equal(properties.has('TABLET_' + stableId), false);
  properties.set('TABLET_REBIND_REQUEST', JSON.stringify({ deviceId: stableId, minutes: 5 }));
  context.authorizeTabletRebind();
  const first = context.dispatch_({ action: 'syncUsers', deviceId: stableId, tabletToken: migrationToken, clientVersion: 7 });
  assert.equal(first.success, true);
  let oldRow = JSON.parse(properties.get('TABLET_' + legacyId));
  let stableRow = JSON.parse(properties.get('TABLET_' + stableId));
  assert.equal(oldRow.active, false); assert.equal(oldRow.migratedTo, stableId);
  assert.equal(stableRow.active, true); assert.equal(stableRow.scheme, 'android-id-v2'); assert.equal(stableRow.migratedFrom, legacyId);
  assert.equal(properties.has('TABLET_REBIND_TO'), false);
  assert.equal(properties.has('TABLET_REBIND_UNTIL'), false);

  // A later signing-key change also requires new explicit permission.
  rejects(() => context.dispatch_({ action: 'syncUsers', deviceId: nextStableId, tabletToken: migrationToken, clientVersion: 7 }), 'DEVICE_REBIND_REQUIRED');
  properties.set('TABLET_REBIND_REQUEST', JSON.stringify({ deviceId: nextStableId, minutes: 5 }));
  context.authorizeTabletRebind();
  const second = context.dispatch_({ action: 'syncUsers', deviceId: nextStableId, tabletToken: migrationToken, clientVersion: 7 });
  assert.equal(second.success, true);
  stableRow = JSON.parse(properties.get('TABLET_' + stableId));
  const nextRow = JSON.parse(properties.get('TABLET_' + nextStableId));
  assert.equal(stableRow.active, false); assert.equal(stableRow.migratedTo, nextStableId);
  assert.equal(nextRow.active, true); assert.equal(nextRow.migratedFrom, stableId);

  // Migrated-away != explicitly revoked; nevertheless it also needs fresh approval.
  rejects(() => context.dispatch_({ action: 'syncUsers', deviceId: stableId, tabletToken: migrationToken, clientVersion: 7 }), 'DEVICE_REBIND_REQUIRED');
  properties.set('TABLET_REBIND_REQUEST', JSON.stringify({ deviceId: stableId, minutes: 5 }));
  context.authorizeTabletRebind();
  const third = context.dispatch_({ action: 'syncUsers', deviceId: stableId, tabletToken: migrationToken, clientVersion: 7 });
  assert.equal(third.success, true);
  assert.equal(JSON.parse(properties.get('TABLET_' + stableId)).active, true);
  assert.equal(JSON.parse(properties.get('TABLET_' + nextStableId)).active, false);
});

test('rebind cannot be redirected, replayed after expiry, or completed with a wrong token', () => {
  const sourceId = '1'.repeat(64), destinationId = '2'.repeat(64), otherId = '3'.repeat(64);
  const tok = crypto.randomBytes(32).toString('base64url');
  const wrongTok = crypto.randomBytes(32).toString('base64url');
  const hash = crypto.createHash('sha256').update(tok).digest('hex');
  properties.set('TABLET_' + sourceId, JSON.stringify({ tokenHash: hash, active: true }));
  properties.set('TABLET_REBIND_REQUEST', JSON.stringify({ deviceId: destinationId, minutes: 1 }));
  context.authorizeTabletRebind();
  rejects(() => context.dispatch_({ action: 'syncUsers', deviceId: otherId, tabletToken: tok, clientVersion: 7 }), 'DEVICE_REBIND_REQUIRED');
  rejects(() => context.dispatch_({ action: 'syncUsers', deviceId: destinationId, tabletToken: wrongTok, clientVersion: 7 }), 'DEVICE_REVOKED');
  assert.equal(JSON.parse(properties.get('TABLET_' + sourceId)).active, true);
  clock += 60001;
  rejects(() => context.dispatch_({ action: 'syncUsers', deviceId: destinationId, tabletToken: tok, clientVersion: 7 }), 'DEVICE_REBIND_REQUIRED');
  assert.equal(properties.has('TABLET_' + destinationId), false);
  properties.delete('TABLET_REBIND_TO'); properties.delete('TABLET_REBIND_UNTIL');
});

test('owner permission cannot overwrite an occupied destination with a different tablet token', () => {
  const sourceId = '4'.repeat(64), occupiedId = '5'.repeat(64);
  const sourceToken = crypto.randomBytes(32).toString('base64url');
  const otherToken = crypto.randomBytes(32).toString('base64url');
  properties.set('TABLET_' + sourceId, JSON.stringify({ active: true, tokenHash: crypto.createHash('sha256').update(sourceToken).digest('hex') }));
  const occupied = { active: true, tokenHash: crypto.createHash('sha256').update(otherToken).digest('hex') };
  properties.set('TABLET_' + occupiedId, JSON.stringify(occupied));
  properties.set('TABLET_REBIND_REQUEST', JSON.stringify({ deviceId: occupiedId, minutes: 5 }));
  context.authorizeTabletRebind();
  rejects(() => context.dispatch_({ action: 'syncUsers', deviceId: occupiedId, tabletToken: sourceToken, clientVersion: 7 }), 'DEVICE_REVOKED');
  assert.equal(JSON.parse(properties.get('TABLET_' + occupiedId)).tokenHash, occupied.tokenHash);
  assert.equal(JSON.parse(properties.get('TABLET_' + sourceId)).active, true);
  properties.delete('TABLET_REBIND_TO'); properties.delete('TABLET_REBIND_UNTIL');
});

test('explicitly revoked exact tablet cannot self-reactivate through token continuity', () => {
  const id1 = 'e'.repeat(64), id2 = 'f'.repeat(64), tok = crypto.randomBytes(32).toString('base64url');
  const tokenHash = crypto.createHash('sha256').update(tok).digest('hex');
  properties.set('TABLET_' + id1, JSON.stringify({ tokenHash, active: true, scheme: 'android-id-v2' }));
  properties.set('TABLET_REVOKE_ID', id1); context.revokeTablet();
  const revoked = JSON.parse(properties.get('TABLET_' + id1));
  assert.equal(revoked.active, false); assert.equal(revoked.revokedExplicitly, true);
  assert.throws(() => context.dispatch_({ action: 'syncUsers', deviceId: id1, tabletToken: tok, clientVersion: 7 }),
    error => error.safeCode === 'DEVICE_REVOKED');
  assert.throws(() => context.dispatch_({ action: 'syncUsers', deviceId: id2, tabletToken: tok, clientVersion: 7 }),
    error => error.safeCode === 'DEVICE_REVOKED');
});

test('revoked tablet loses sync and administrative access', () => {
  properties.set('TABLET_REVOKE_ID', deviceId); context.revokeTablet();
  rejects(() => request('syncUsers', { clientVersion: 7 }), 'DEVICE_REVOKED');
  rejects(() => request('getAudit', { adminToken }), 'DEVICE_REVOKED');
});
// Separate capability fixtures. The preceding 21 legacy scenarios remain unchanged.
properties.set('TABLET_'+deviceId,JSON.stringify({tokenHash:crypto.createHash('sha256').update(tabletToken).digest('hex'),active:true,scheme:'android-id-v2'}));
let engineerId, tabletAdminToken, provisionToken, rotatedToken;
const targetDevice='1234567890abcdef'.repeat(4);
const grant=(id,enabled)=>{properties.set('TABLET_ADMIN_PERMISSION_REQUEST',JSON.stringify({userId:id,enabled,reason:'Synthetic capability test'}));context.applyTabletAdminPermission();};
const ta=(action,fields={})=>request(action,{tabletAdminToken,...fields});
test('ENGINEER role is supported without user administration privileges',()=>{
  const state=context.readState_();const u=context.newUser_('engineer','Engineer test','ENGINEER',credential());engineerId=u.id;
  state.users.push(u);context.commit_(state,context.auditEvent_('OWNER','USER_CREATED',u.id,''));
  rejects(()=>request('adminAuthenticate',{username:'engineer',pin:fixturePin}),'INVALID_CREDENTIALS');
});
test('ENGINEER without explicit TABLET_ADMIN capability is denied',()=>{
  rejects(()=>request('authenticateTabletAdmin',{userId:engineerId,pin:fixturePin}),'TABLET_ADMIN_REQUIRED');
});
test('ADMIN role alone does not imply TABLET_ADMIN',()=>{
  rejects(()=>request('authenticateTabletAdmin',{userId:ownerId,pin:fixturePin}),'TABLET_ADMIN_REQUIRED');
});
test('only owner editor can grant permission and pending property is deleted',()=>{
  rejects(()=>request('applyTabletAdminPermission',{userId:engineerId,enabled:true,reason:'test'}),'INVALID_REQUEST');
  grant(engineerId,true);assert.equal(properties.has('TABLET_ADMIN_PERMISSION_REQUEST'),false);
  assert.equal(request('syncUsers',{clientVersion:context.readState_().version}).tabletAdminUserIds.includes(engineerId),true);
});
test('ENGINEER with capability and current PIN receives isolated 15 minute session',()=>{
  const r=request('authenticateTabletAdmin',{userId:engineerId,pin:fixturePin});tabletAdminToken=r.tabletAdminToken;
  assert.equal(r.scope,'TABLET_ADMIN_ONLY');assert.equal(r.expiresInSeconds,900);
  const stored=properties.get('TABLET_ADMIN_SESSION_'+deviceId);assert.equal(stored.includes(tabletAdminToken),false);
});
test('tablet admin session cannot create users or retrieve user audit',()=>{
  rejects(()=>request('createUser',{adminToken:tabletAdminToken,username:'forbidden',displayName:'Forbidden',role:'ADMIN',credential:credential()}),'SESSION_EXPIRED');
  rejects(()=>request('getAudit',{adminToken:tabletAdminToken}),'SESSION_EXPIRED');
});
test('tablet admin session cannot reset PIN or change roles',()=>{
  rejects(()=>request('resetUserPin',{adminToken:tabletAdminToken,id:ownerId,credential:credential()}),'SESSION_EXPIRED');
  rejects(()=>request('changeUserRole',{adminToken:tabletAdminToken,id:ownerId,role:'OPERATOR'}),'SESSION_EXPIRED');
});
test('tablet admin actions reject a wrong scope even with matching hash',()=>{
  const key='TABLET_ADMIN_SESSION_'+deviceId,original=properties.get(key),s=JSON.parse(original);s.scope='USER_ADMIN';properties.set(key,JSON.stringify(s));
  rejects(()=>ta('listTablets'),'SESSION_EXPIRED');properties.set(key,original);
});
test('each operation validates the current authorized device and tablet secret',()=>{
  rejects(()=>request('listTablets',{tabletAdminToken,tabletToken:'X'.repeat(43)}),'DEVICE_REVOKED');
});
test('invalid fingerprint cannot provision and existing tablet cannot be overwritten',()=>{
  rejects(()=>ta('provisionTabletAdmin',{targetDeviceId:'invalid',alias:'New'}),'INVALID_REQUEST');
  rejects(()=>ta('provisionTabletAdmin',{targetDeviceId:deviceId,alias:'Occupied'}),'TABLET_EXISTS');
});
test('server provisions high entropy token once and stores hash only',()=>{
  const r=ta('provisionTabletAdmin',{targetDeviceId:'  '+targetDevice+'  ',alias:' New tablet '});provisionToken=r.tabletToken;
  assert.match(provisionToken,/^[A-Za-z0-9_-]{43}$/);assert.equal(r.tablet.alias,'New tablet');
  const saved=properties.get('TABLET_'+targetDevice);assert.equal(saved.includes(provisionToken),false);
  assert.equal(JSON.parse(saved).scheme,'android-id-v2');
});
test('tablet list redacts hashes sessions and server keys including legacy tablets',()=>{
  const response=JSON.stringify(ta('listTablets'));
  for(const secret of [provisionToken,tabletAdminToken,tabletToken,'tokenHash','SERVER_RANDOM_KEY','SESSION_'])assert.equal(response.includes(secret),false);
  assert.equal(response.includes('Tablet sin nombre'),true);
});
test('self revocation is rejected on the server',()=>{
  rejects(()=>ta('revokeTabletAdmin',{targetDeviceId:deviceId}),'TABLET_SELF_REVOKE');
  assert.equal(JSON.parse(properties.get('TABLET_'+deviceId)).active,true);
});
test('revocation blocks target sync and invalidates both administrative sessions',()=>{
  properties.set('SESSION_'+targetDevice,'{}');properties.set('TABLET_ADMIN_SESSION_'+targetDevice,'{}');
  ta('revokeTabletAdmin',{targetDeviceId:targetDevice});
  rejects(()=>context.dispatch_({action:'syncUsers',deviceId:targetDevice,tabletToken:provisionToken,clientVersion:-1}),'DEVICE_REVOKED');
  assert.equal(properties.has('SESSION_'+targetDevice),false);assert.equal(properties.has('TABLET_ADMIN_SESSION_'+targetDevice),false);
});
test('reactivation retains the existing token and restores access',()=>{
  const hash=JSON.parse(properties.get('TABLET_'+targetDevice)).tokenHash;ta('reactivateTabletAdmin',{targetDeviceId:targetDevice});
  assert.equal(JSON.parse(properties.get('TABLET_'+targetDevice)).tokenHash,hash);
  assert.equal(context.dispatch_({action:'syncUsers',deviceId:targetDevice,tabletToken:provisionToken,clientVersion:-1}).success,true);
});
test('rotation rejects old token and accepts new token',()=>{
  rotatedToken=ta('rotateTabletToken',{targetDeviceId:targetDevice}).tabletToken;assert.notEqual(rotatedToken,provisionToken);
  rejects(()=>context.dispatch_({action:'syncUsers',deviceId:targetDevice,tabletToken:provisionToken,clientVersion:-1}),'DEVICE_REVOKED');
  assert.equal(context.dispatch_({action:'syncUsers',deviceId:targetDevice,tabletToken:rotatedToken,clientVersion:-1}).success,true);
});
test('tablet audit retains all mutations without any secret material',()=>{
  const r=JSON.stringify(ta('getTabletAudit',{targetDeviceId:targetDevice}));
  for(const action of ['TABLET_PROVISIONED','TABLET_REVOKED','TABLET_REACTIVATED','TABLET_TOKEN_ROTATED'])assert.equal(r.includes(action),true);
  for(const secret of [fixturePin,provisionToken,rotatedToken,tabletAdminToken,tabletToken,'tokenHash','pinHash'])assert.equal(r.includes(secret),false);
});
test('removing capability immediately blocks the already issued session',()=>{
  grant(engineerId,false);rejects(()=>ta('listTablets'),'SESSION_EXPIRED');
  grant(engineerId,true);tabletAdminToken=request('authenticateTabletAdmin',{userId:engineerId,pin:fixturePin}).tabletAdminToken;
});
test('inactive user invalidates tablet administration immediately',()=>{
  const state=context.readState_();state.users.find(u=>u.id===engineerId).active=false;context.commit_(state,context.auditEvent_('OWNER','USER_DISABLED',engineerId,''));
  rejects(()=>ta('listTablets'),'TABLET_ADMIN_REQUIRED');
  const restored=context.readState_();restored.users.find(u=>u.id===engineerId).active=true;context.commit_(restored,context.auditEvent_('OWNER','USER_ENABLED',engineerId,''));
});
test('tablet admin session expires without altering normal user or device state',()=>{
  clock+=900001;rejects(()=>ta('listTablets'),'SESSION_EXPIRED');assert.equal(JSON.parse(properties.get('TABLET_'+deviceId)).active,true);
  assert.equal(context.readState_().users.find(u=>u.id===engineerId).active,true);
});
console.log(`${passed} service tests passed.`);
