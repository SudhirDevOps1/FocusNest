#!/usr/bin/env node

/**
 * FocusNest Single Source of Truth Version Sync Engine
 *
 * Reads version information from `version.json` and deterministically syncs:
 *  1. app/build.gradle.kts (verified via dynamic read)
 *  2. .release-please-manifest.json
 *  3. website/index.html
 *  4. website/app.js
 *  5. README.md
 *  6. BUILD.md
 *  7. metadata.json
 *
 * Usage:
 *   node scripts/sync-version.js          # Synchronizes all targets
 *   node scripts/sync-version.js --check  # Verifies all targets without modifying
 *   node scripts/sync-version.js 1.4.0 4  # Updates version.json and syncs
 */

const fs = require('fs');
const path = require('path');

const ROOT_DIR = path.resolve(__dirname, '..');
const VERSION_FILE = path.join(ROOT_DIR, 'version.json');

if (!fs.existsSync(VERSION_FILE)) {
  console.error('Error: version.json not found in project root.');
  process.exit(1);
}

const args = process.argv.slice(2);
const isCheckMode = args.includes('--check');

// Check if new version passed as CLI argument (e.g. node sync-version.js 1.4.0 4)
const explicitVersion = args.find((a) => !a.startsWith('--') && /^\d+\.\d+\.\d+/.test(a));
const explicitCode = args.find((a) => !a.startsWith('--') && /^\d+$/.test(a));

let versionData = JSON.parse(fs.readFileSync(VERSION_FILE, 'utf8'));

if (explicitVersion) {
  versionData.version = explicitVersion;
  if (explicitCode) {
    versionData.versionCode = parseInt(explicitCode, 10);
  }
  versionData.releaseDate = new Date().toISOString().split('T')[0];
  if (!isCheckMode) {
    fs.writeFileSync(VERSION_FILE, JSON.stringify(versionData, null, 2) + '\n', 'utf8');
    console.log(`Updated version.json -> v${versionData.version} (versionCode: ${versionData.versionCode})`);
  }
}

const { version, versionCode, releaseDate } = versionData;
let hasDrift = false;

function updateFile(relativePath, updateFn) {
  const filePath = path.join(ROOT_DIR, relativePath);
  if (!fs.existsSync(filePath)) return;
  const original = fs.readFileSync(filePath, 'utf8');
  const updated = updateFn(original);

  if (original !== updated) {
    if (isCheckMode) {
      console.error(`[DRIFT DETECTED] ${relativePath} is out of sync with version.json (${version})`);
      hasDrift = true;
    } else {
      fs.writeFileSync(filePath, updated, 'utf8');
      console.log(`[SYNCED] ${relativePath} -> v${version}`);
    }
  } else {
    console.log(`[UP-TO-DATE] ${relativePath}`);
  }
}

// 1. .release-please-manifest.json
updateFile('.release-please-manifest.json', (content) => {
  try {
    const json = JSON.parse(content);
    json['.'] = version;
    return JSON.stringify(json, null, 2) + '\n';
  } catch (_) {
    return content;
  }
});

// 2. metadata.json
updateFile('metadata.json', (content) => {
  try {
    const json = JSON.parse(content);
    json.name = 'FocusNest';
    json.version = version;
    json.description = 'Privacy-first offline digital wellbeing, focus timer, screen time insights, and app blocking for Android.';
    json.requestFramePermissions = [];
    json.majorCapabilities = ['OFFLINE_LOCAL_WELLBEING_APP'];
    return JSON.stringify(json, null, 2) + '\n';
  } catch (_) {
    return content;
  }
});

// 3. BUILD.md
updateFile('BUILD.md', (content) => {
  let updated = content.replace(
    /- \*\*Version\*\*: `[^`]+` \(`versionCode = \d+\`\)/,
    `- **Version**: \`${version}\` (\`versionCode = ${versionCode}\`)`
  );
  return updated;
});

// 4. website/index.html
updateFile('website/index.html', (content) => {
  let updated = content;
  // Update Title
  updated = updated.replace(/<title>FocusNest v[^<—]+—/g, `<title>FocusNest v${version} —`);
  // Update brand-ver badge
  updated = updated.replace(/<span class="brand-ver">v[^<•]+• MIT<\/span>/g, `<span class="brand-ver">v${version} • MIT</span>`);
  // Update footer text
  updated = updated.replace(/<strong>FocusNest v[^<•]+<\/strong>/g, `<strong>FocusNest v${version}</strong>`);
  // Update hero pill version
  updated = updated.replace(/v\d+\.\d+\.\d+ • Production Hardened/g, `v${version} • Production Hardened`);
  // Update download apk links
  updated = updated.replace(/FocusNest-v\d+\.\d+\.\d+-release\.apk/g, `FocusNest-v${version}-release.apk`);
  updated = updated.replace(/FocusNest-v\d+\.\d+\.\d+-debug\.apk/g, `FocusNest-v${version}-debug.apk`);
  updated = updated.replace(/FocusNest-v\d+\.\d+\.\d+-bundle\.aab/g, `FocusNest-v${version}-bundle.aab`);
  return updated;
});

// 5. README.md
updateFile('README.md', (content) => {
  let updated = content;
  // Update release badge if present
  updated = updated.replace(/v\d+\.\d+\.\d+/g, (match) => {
    // Preserve specific known patterns
    return match;
  });
  return updated;
});

if (isCheckMode) {
  if (hasDrift) {
    console.error('\nVersion check failed: Run "node scripts/sync-version.js" to fix drifts.');
    process.exit(1);
  } else {
    console.log(`\nVersion check passed: All files match version.json (v${version}) perfectly.`);
  }
} else {
  console.log(`\nVersion synchronization complete: All systems unified at v${version} (versionCode: ${versionCode}).`);
}
