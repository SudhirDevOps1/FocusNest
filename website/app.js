const phoneScreens = {
  home: `
    <div class="p-head">
      <span class="p-title">Home Dashboard</span>
      <span class="p-badge">Measured • Today</span>
    </div>
    <div class="p-ring">
      <strong>1h 42m</strong>
      <span>of 3h goal</span>
    </div>
    <div class="p-card highlight">
      <div class="p-card-title">Daily Intention</div>
      <div class="p-card-sub">"Finish thesis draft before checking social feeds."</div>
    </div>
    <div class="p-card">
      <div class="p-card-title">Launcher Exclusion Active</div>
      <div class="p-card-sub">Pixel Launcher &amp; SystemUI excluded from distraction rank</div>
    </div>
  `,
  focus: `
    <div class="p-head">
      <span class="p-title">Focus Sanctuary</span>
      <span class="p-badge">Pomodoro 25m</span>
    </div>
    <div class="p-ring">
      <strong>21:48</strong>
      <span>Cycle 2 of 4</span>
    </div>
    <div class="p-card highlight">
      <div class="p-card-title">Offline Ambient Soundscape</div>
      <div class="p-card-sub">Procedural AudioTrack synthesis (0 KB network)</div>
      <div class="p-chips">
        <span class="p-chip">Off</span>
        <span class="p-chip active">Brown Noise</span>
        <span class="p-chip">Soft Rain</span>
        <span class="p-chip">Warm Drone</span>
      </div>
    </div>
    <div class="p-card">
      <div class="p-card-title">Today's Focus</div>
      <div class="p-card-sub">3 sessions • 75m • 7-day streak</div>
    </div>
  `,
  habits: `
    <div class="p-head">
      <span class="p-title">Habits &amp; Wind-Down</span>
      <span class="p-badge">7d Strip</span>
    </div>
    <div class="p-card highlight">
      <div class="p-card-title">Evening Wind-Down Checklist</div>
      <div class="p-check-row"><span>Dock charger away from bed</span><strong>Done</strong></div>
      <div class="p-check-row"><span>Bedtime Grayscale / Night Light</span><strong>Done</strong></div>
      <div class="p-check-row"><span>Dim alerts before sleep</span><strong>Done</strong></div>
      <div class="p-check-row"><span>Tomorrow's morning focus</span><span>Pending</span></div>
    </div>
    <div class="p-card">
      <div class="p-card-title">Daily Streaks</div>
      <div class="p-card-sub">Morning Screen-Free Hour • 11-day streak</div>
    </div>
  `,
  shield: `
    <div class="p-head">
      <span class="p-title">Shield &amp; Local VPN</span>
      <span class="p-badge">CONNECTED</span>
    </div>
    <div class="p-card highlight">
      <div class="p-card-title">Mindful Launch Pause Curtain</div>
      <div class="p-card-sub">8s breathing delay armed before opening Social &amp; Video apps</div>
      <div class="p-chips">
        <span class="p-chip active">Enabled • 8s</span>
        <span class="p-chip">3 apps armed</span>
      </div>
    </div>
    <div class="p-card">
      <div class="p-card-title">Local On-Device DNS Filter</div>
      <div class="p-card-sub">VpnService CONNECTED • 14 blocked hits • Allowlist override ready</div>
    </div>
  `,
  unlock: `
    <div class="p-head">
      <span class="p-title">Mindful Pause &amp; Unlock</span>
      <span class="p-badge">8s Curtain</span>
    </div>
    <div class="p-ring">
      <strong>Inhale…</strong>
      <span>5s left to decide</span>
    </div>
    <div class="p-card highlight">
      <div class="p-card-title">Return to Focus</div>
      <div class="p-card-sub">Primary CTA closes distracting app immediately.</div>
    </div>
    <div class="p-card">
      <div class="p-card-title">12 Friction Mechanisms</div>
      <div class="p-chips">
        <span class="p-chip active">Breathing</span>
        <span class="p-chip">Adaptive Math</span>
        <span class="p-chip">QR/NFC Token</span>
      </div>
    </div>
  `,
};

const featuresCatalog = [
  {
    id: 'focus-sanctuary',
    title: 'Focus Sanctuary & Monotonic Timer',
    category: 'Focus',
    summary: 'Pomodoro, Countdown, and Stopwatch sessions that survive process death and device reboots via elapsedRealtime persistence.',
    what: 'Foreground-service countdown engine across Study, Work, Reading, Coding, Creative, and Custom modes with +5m Extend, Restart, and Break Skip.',
    why: 'Keeps focus locks tamper-resistant even if system wall clock shifts or Android trims background processes.',
    when: 'Use anytime you need structured deep-work intervals paired with automatic profile blocking.',
    how: 'Open Focus tab → Select mode/preset → Toggle ambient audio or profile link → Tap Start Focus.',
    permissions: 'Optional POST_NOTIFICATIONS (API 33+) for live timer notification; RECEIVE_BOOT_COMPLETED for reboot restore.',
    compat: 'Android 8.0 – 16+ (API 26–36) including Realme UI, MIUI, OneUI, Pixel.',
    status: 'Verified Production Ready',
  },
  {
    id: 'ambient-soundscapes',
    title: 'Offline Ambient Soundscapes',
    category: 'Focus',
    summary: 'Procedural Kotlin PCM AudioTrack synthesis for Brown Noise, Soft Rain, Warm Drone, and Study Wash with zero audio asset bloat.',
    what: 'Synthesizes calming acoustic soundscapes directly in memory while a focus session runs.',
    why: 'Eliminates 40+ MB media downloads and removes any need for streaming network permissions.',
    when: 'Turn on during Focus sessions in noisy libraries, transit, or shared study rooms.',
    how: 'Open Focus tab → Pick soundscape chip & volume slider before or during a session.',
    permissions: 'Zero special permissions required.',
    compat: 'Android 8.0 – 16+ (AudioTrack hardware agnostic).',
    status: 'Verified Production Ready',
  },
  {
    id: 'modular-blocker',
    title: 'Modular App, Group & Category Blocker',
    category: 'App Blocking',
    summary: 'Deterministic RuleEvaluator + BlockingScheduler + EmergencyAccessManager protecting daily caps, grace periods, and schedules.',
    what: 'Combines app daily/weekly limits, shared app groups, category caps, and Study/Work/Sleep/Reading/Weekend profiles with immediate edits.',
    why: 'Prevents loophole bypasses while always shielding Dialer, Contacts, Settings, and your home launcher.',
    when: 'Arm daily allowances during normal days or enforce strict schedules overnight and during classes.',
    how: 'Open Shield → Apps & Limits / Profiles / Schedules → Select distracting apps and limits.',
    permissions: 'PACKAGE_USAGE_STATS + AccessibilityService (plus UsageEvents foreground fallback check during Focus/Shield ticks).',
    compat: 'Android 8.0 – 16+ (includes OEM battery lock guidance in Permissions).',
    status: 'Hardened & Verified',
  },
  {
    id: 'launch-pause-curtain',
    title: 'Mindful Launch Pause Curtain',
    category: 'App Blocking',
    summary: '5–15 second guided breathing buffer before armed apps open, giving conscious intention a chance to beat reflex taps.',
    what: 'Soft breathing pause curtain with animated orb, Return to Focus primary button, and 5-minute proceed cooldown.',
    why: 'Breaks automatic thumb muscle memory without aggressive permanent lockouts.',
    when: 'Ideal for social feeds or video apps you still open occasionally but want intentional friction on.',
    how: 'Open Shield → Apps & Limits → Enable Launch Pause Curtain slider & arm target apps.',
    permissions: 'AccessibilityService for instant app-open detection.',
    compat: 'Android 8.0 – 16+.',
    status: 'Verified Production Ready',
  },
  {
    id: 'reels-shorts-pause',
    title: 'Reels & Shorts Reduction',
    category: 'App Blocking',
    summary: 'Best-effort local view-ID pause overlay targeting infinite short-form vertical feeds while keeping normal messaging usable.',
    what: 'Monitors configured short-form video containers while enabled in Accessibility.',
    why: 'Stops endless Doom-Scrolling inside Reels/Shorts without blocking DMs or tutorials.',
    when: 'Enable per-app in Shield → Reels & Shorts.',
    how: 'Toggle short-form rules and set daily minute cap or intervention pause seconds.',
    permissions: 'Optional AccessibilityService (view-ID check fails safely if upstream social UI updates).',
    compat: 'Best-effort across supported host app versions.',
    status: 'Best-Effort & Transparently Documented',
  },
  {
    id: 'launcher-aware-analytics',
    title: 'Dynamic Launcher & System Package Exclusion',
    category: 'Screen Time',
    summary: 'Automatically excludes active CATEGORY_HOME launcher, SystemUI, IME keyboards, and permission controllers from distracting metrics.',
    what: 'SystemPackageClassifier queries PackageManager and InputMethodManager dynamically with an explicit Show system & launcher apps toggle.',
    why: 'Prevents home screen navigation or Gboard typing from distorting your distracting app charts.',
    when: 'Active automatically on every install; toggle anytime in Insights, Blocker, or Settings.',
    how: 'Toggle Show System chip in Insights/Blocker or Switch in More → Settings.',
    permissions: 'Scoped <queries> for CATEGORY_LAUNCHER + CATEGORY_HOME on Android 11+.',
    compat: 'Android 8.0 – 16+ (Play-compliant package visibility).',
    status: 'Hardened & Verified',
  },
  {
    id: 'weekly-pdf-report',
    title: 'Weekly PDF / Printable Wellbeing Report Card',
    category: 'Analytics',
    summary: 'Offline PrintManager HTML-to-PDF wellbeing card with HTML-escaped app stats, category totals, goal badge, and session notes.',
    what: 'Creates a crisp print/PDF wellbeing report plus portable CSV and JSON exports.',
    why: 'Lets students and individuals archive weekly progress without third-party cloud dashboards.',
    when: 'Run weekly or monthly from the Insights tab.',
    how: 'Open Insights → Tap Print / PDF Report Card, Export CSV, or Export JSON.',
    permissions: 'Zero extra permissions; uses standard Android PrintManager.',
    compat: 'Android 8.0 – 16+.',
    status: 'Verified Production Ready',
  },
  {
    id: 'local-vpn-website-blocker',
    title: 'Local On-Device DNS Website Blocker + Rule Tester',
    category: 'Website Blocking',
    summary: 'Zero-server VpnService finite state machine (DISCONNECTED..CONNECTED/ERROR) with allowlist > blocklist priority and live domain tester.',
    what: 'Captures local UDP/53 DNS requests on 10.88.0.1 and replies NXDOMAIN for matched domains, honoring per-app network scoping.',
    why: 'Blocks distracting websites cleanly without sending DNS queries or HTTPS traffic off-device.',
    when: 'Enable in Shield → Websites (VPN) and verify any URL right inside the built-in Rule Tester.',
    how: 'Add blocked/allowlisted domains → Test domain in Verify Domain Rule Matching → Start Local VPN.',
    permissions: 'Android VpnService consent (one active VPN allowed system-wide; Private DNS/DoH bypasses UDP/53).',
    compat: 'Android 8.0 – 16+.',
    status: 'Hardened & Verified',
  },
  {
    id: 'unlock-12-mechanisms',
    title: '12 Unlock Challenges & Invincible Safety',
    category: 'Challenges',
    summary: 'Breathing 4-2-4, Adaptive Division Math, Sentence Typing, Intention Chips, QR/Barcode/NFC, Cooldown, Daily Cap, Strict Mode.',
    what: 'Configurable friction gate shown inside BlockedOverlayActivity whenever a blocked app is launched.',
    why: 'Matches challenge intensity to user intent without locking owners out of emergency access.',
    when: 'Assign globally or per blocking profile.',
    how: 'Open Shield → Unlock Challenges or select per-profile challenge mode.',
    permissions: 'Optional Camera for QR/Barcode; optional NFC hardware.',
    compat: 'Android 8.0 – 16+.',
    status: 'Verified Production Ready',
  },
  {
    id: 'camerax-qr-barcode',
    title: 'Stride-Safe CameraX QR & Barcode Vault',
    category: 'NFC & QR',
    summary: 'Hardware-safe YuvLuminanceExtractor, lifecycle DisposableEffect unbind, flashlight toggle, retry recovery, and SHA-256 one-time tokens.',
    what: 'Generates & scans replay-protected offline QR codes and physical product barcodes without internet calls.',
    why: 'Fixes YUV rowStride buffer crashes across diverse camera sensors cleanly.',
    when: 'Pair physical desk cards or barcodes you must scan across the room to unlock feeds.',
    how: 'Open More → NFC, QR & Barcode Vault → Generate or Scan Token.',
    permissions: 'Optional CAMERA permission with manual token fallback if denied/unavailable.',
    compat: 'Android 8.0 – 16+.',
    status: 'Hardened & Verified',
  },
  {
    id: 'nfc-tag-triggers',
    title: 'Physical NFC Tag Triggers',
    category: 'NFC & QR',
    summary: 'Bind physical NDEF/UID NFC stickers to Start Focus, Unlock Profile, or Toggle Quick Shield via foreground dispatch.',
    what: 'Detects NfcAdapter capability cleanly and shows non-NFC fallback status on unsupported devices.',
    why: 'Turn a physical desk tag or keychain fob into a tactile focus habit anchor.',
    when: 'Hold phone near a paired NFC tag to toggle Focus Sanctuary instantaneously.',
    how: 'Open More → NFC, QR & Barcode Vault → Tap physical tag while FocusNest is open.',
    permissions: 'Optional NFC hardware; non-blocking if absent.',
    compat: 'NFC-equipped Android 8.0 – 16+ devices.',
    status: 'Verified Hardware-Dependent',
  },
  {
    id: 'smart-unlock-recovery',
    title: 'Emergency Access, Cooldowns & Biometric Guard',
    category: 'Smart Unlock',
    summary: 'Invincible Mode delayed edits, BiometricPrompt authentication, passcode verification, and dialer/contacts safe list.',
    what: 'Prevents impulsive rule deletion during weak moments while never brinking essential phone calls.',
    why: 'Avoids dangerous Device Admin traps while strengthening self-control.',
    when: 'Enable in Shield → Invincible Mode & More → Settings.',
    how: 'Set emergency passcode or biometric prompt and configure safe apps.',
    permissions: 'Optional BiometricPrompt (weak/strong/device credential).',
    compat: 'Android 8.0 – 16+.',
    status: 'Verified Production Ready',
  },
  {
    id: 'startup-permission-gateway',
    title: 'Deterministic Startup State & Reactive Permissions',
    category: 'Privacy',
    summary: 'StartupPhase machine prevents cold-start onboarding loops; PermissionStatusManager tracks runtime vs special access revocations.',
    what: 'Shows splash until DataStore loads, stores onboardingVersion & completion timestamp, adds Tutorial Replay mode, and flags Android 13+ Restricted Settings hints.',
    why: 'Eliminates annoying welcome loops and stale permission UI warnings.',
    when: 'Runs automatically on every cold start and lifecycle resume.',
    how: 'Replay tutorials anytime under More → Settings → Replay Onboarding Tutorial.',
    permissions: 'Read-only system state inspection across AppOps, Settings.Secure, PowerManager, VpnService.',
    compat: 'Android 8.0 – 16+ (including Android 13+ sideloaded Restricted Settings guidance).',
    status: 'Hardened & Verified',
  },
  {
    id: 'aes-backups-privacy',
    title: 'AES-256-GCM Backups & Browser Update Handoff',
    category: 'Privacy',
    summary: 'PBKDF2WithHmacSHA256 portable encrypted archives, allowBackup=false, and zero-internet browser ACTION_VIEW release check.',
    what: 'Password-encrypted JSON restore packages plus zero-socket release link to SudhirDevOps1/FocusNest.',
    why: 'Zero cloud accounts, zero telemetry leaks, full data portability across devices.',
    when: 'Export backups before device migration or check GitHub Releases offline-safely.',
    how: 'More → Backup & Restore / More → Settings → Check Releases on GitHub.',
    permissions: 'Storage Access Framework document picker (zero broad storage permissions).',
    compat: 'Android 8.0 – 16+.',
    status: 'Verified Production Ready',
  },
  {
    id: 'adaptive-two-pane-ui',
    title: 'Adaptive Foldables, Tablets & Bilingual M3 UI',
    category: 'Customization',
    summary: '6 accent palettes, Light/Dark/Dynamic Material You themes, English + Hindi runtime switching, NavigationRail & AdaptiveTwoPane.',
    what: 'Fluid Compose UI scaling from compact 360dp phones to expanded 720dp+ foldables and Chromebooks.',
    why: 'Respects system accessibility scaling, high contrast palettes, and multilingual wellbeing.',
    when: 'Customize anytime under More → Settings.',
    how: 'Pick accent palette, theme mode, or English/हिन्दी language live without restarting.',
    permissions: 'None.',
    compat: 'Android 8.0 – 16+ (Material You dynamic color on Android 12+).',
    status: 'Verified Production Ready',
  },
  {
    id: 'bedtime-habits-widgets',
    title: 'Habits, Evening Wind-Down & 3 Home Widgets',
    category: 'Customization',
    summary: 'Room-backed habits with 7-day charts, auto-resetting Bedtime Wind-Down Checklist, Quick Settings Tile, and 3 home widgets.',
    what: 'Includes Display Brightness & Night Light shortcuts plus daily goal/focus/shield Glance widgets.',
    why: 'Keeps daily reflection and focus toggles visible without opening distractions.',
    when: 'Use every morning for habits and before bed for wind-down hygiene.',
    how: 'Open More → Daily Habits or Mindful Usage Tools.',
    permissions: 'Optional ACCESS_NOTIFICATION_POLICY for scheduled Do Not Disturb.',
    compat: 'Android 8.0 – 16+.',
    status: 'Verified Production Ready',
  },
];

function escapeHtml(str) {
  return String(str)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

// 1. Phone Preview Tabs
const screenEl = document.getElementById('phoneScreen');
const tabButtons = document.querySelectorAll('.phone-tab');
function renderPhoneTab(key) {
  if (!screenEl || !phoneScreens[key]) return;
  screenEl.innerHTML = phoneScreens[key];
  tabButtons.forEach((btn) => {
    const isActive = btn.dataset.screen === key;
    btn.classList.toggle('active', isActive);
    btn.setAttribute('aria-selected', String(isActive));
  });
}
tabButtons.forEach((btn) => btn.addEventListener('click', () => renderPhoneTab(btn.dataset.screen)));
renderPhoneTab('home');

// 2. Theme & Palette Switchers
const themeBtn = document.getElementById('themeToggle');
const themeIcon = document.getElementById('themeIcon');
const themeLabel = document.getElementById('themeLabel');
if (themeBtn) {
  themeBtn.addEventListener('click', () => {
    const root = document.documentElement;
    const next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    root.setAttribute('data-theme', next);
    themeIcon.textContent = next === 'dark' ? '☀' : '☾';
    themeLabel.textContent = next === 'dark' ? 'Light' : 'Dark';
  });
}

document.querySelectorAll('[data-set-palette]').forEach((btn) => {
  btn.addEventListener('click', () => {
    const palette = btn.getAttribute('data-set-palette');
    document.documentElement.setAttribute('data-palette', palette);
    document.querySelectorAll('[data-set-palette]').forEach((b) => b.classList.toggle('active', b === btn));
  });
});

// 3. Mobile Nav Drawer
const mobileNavToggle = document.getElementById('mobileNavToggle');
const primaryNav = document.getElementById('primaryNav');
if (mobileNavToggle && primaryNav) {
  mobileNavToggle.addEventListener('click', () => {
    const isOpen = primaryNav.classList.toggle('open');
    mobileNavToggle.setAttribute('aria-expanded', String(isOpen));
  });
  primaryNav.querySelectorAll('a').forEach((link) => {
    link.addEventListener('click', () => {
      primaryNav.classList.remove('open');
      mobileNavToggle.setAttribute('aria-expanded', 'false');
    });
  });
}

// 4. Searchable + 10-Category Filterable Feature Gallery + Detail Modal
const galleryEl = document.getElementById('featureGalleryGrid');
const emptyMsgEl = document.getElementById('featureEmptyMsg');
const searchEl = document.getElementById('featureSearch');
const filterBtns = document.querySelectorAll('.filter-pill');
const modalEl = document.getElementById('featureModal');
const modalCategory = document.getElementById('modalCategory');
const modalTitle = document.getElementById('modalTitle');
const modalBody = document.getElementById('modalBody');
const modalCloseBtn = document.getElementById('modalCloseBtn');

let activeCategory = 'all';
let searchQuery = '';

function openFeatureModal(feature) {
  if (!modalEl || !feature) return;
  modalCategory.textContent = feature.category;
  modalTitle.textContent = feature.title;
  modalBody.innerHTML = `
    <p><strong>What it does:</strong> ${escapeHtml(feature.what)}</p>
    <p><strong>Why it matters:</strong> ${escapeHtml(feature.why)}</p>
    <p><strong>When to use:</strong> ${escapeHtml(feature.when)}</p>
    <p><strong>How to configure:</strong> ${escapeHtml(feature.how)}</p>
    <p><strong>Required permissions:</strong> ${escapeHtml(feature.permissions)}</p>
    <p><strong>Compatibility:</strong> ${escapeHtml(feature.compat)}</p>
    <p><strong>Current status:</strong> ${escapeHtml(feature.status)}</p>
  `;
  modalEl.hidden = false;
  modalCloseBtn?.focus();
}

function closeFeatureModal() {
  if (modalEl) modalEl.hidden = true;
}

modalCloseBtn?.addEventListener('click', closeFeatureModal);
modalEl?.addEventListener('click', (e) => {
  if (e.target === modalEl) closeFeatureModal();
});
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape' && modalEl && !modalEl.hidden) closeFeatureModal();
});

function renderFeatureGallery() {
  if (!galleryEl) return;
  const q = searchQuery.trim().toLowerCase();
  const filtered = featuresCatalog.filter((item) => {
    const matchesCat = activeCategory === 'all' || item.category === activeCategory;
    if (!matchesCat) return false;
    if (!q) return true;
    return [item.title, item.category, item.summary, item.what, item.how, item.permissions]
      .join(' ')
      .toLowerCase()
      .includes(q);
  });

  galleryEl.innerHTML = '';
  if (filtered.length === 0) {
    if (emptyMsgEl) emptyMsgEl.hidden = false;
    return;
  }
  if (emptyMsgEl) emptyMsgEl.hidden = true;

  filtered.forEach((item) => {
    const card = document.createElement('article');
    card.className = 'card feature-card';
    card.tabIndex = 0;
    card.setAttribute('role', 'button');
    card.setAttribute('aria-label', `Inspect ${item.title}`);
    card.innerHTML = `
      <div>
        <div class="card-tag">${escapeHtml(item.category)}</div>
        <h3>${escapeHtml(item.title)}</h3>
        <p>${escapeHtml(item.summary)}</p>
      </div>
      <div class="feature-card-foot">
        <span>${escapeHtml(item.status)}</span>
        <span>Inspect Guide →</span>
      </div>
    `;
    card.addEventListener('click', () => openFeatureModal(item));
    card.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        openFeatureModal(item);
      }
    });
    galleryEl.appendChild(card);
  });
}

filterBtns.forEach((btn) => {
  btn.addEventListener('click', () => {
    activeCategory = btn.dataset.category || 'all';
    filterBtns.forEach((b) => b.classList.toggle('active', b === btn));
    renderFeatureGallery();
  });
});

searchEl?.addEventListener('input', (e) => {
  searchQuery = e.target.value || '';
  renderFeatureGallery();
});

renderFeatureGallery();

// 5. Interactive Launch Pause Curtain Simulator
const curtainOrb = document.getElementById('curtainOrb');
const curtainStatus = document.getElementById('curtainStatus');
const simulateCurtainBtn = document.getElementById('simulateCurtainBtn');
const returnFocusBtn = document.getElementById('returnFocusBtn');
let curtainTimer = null;

simulateCurtainBtn?.addEventListener('click', () => {
  if (curtainTimer) clearInterval(curtainTimer);
  let left = 5;
  curtainOrb?.classList.add('inhale');
  if (curtainOrb) curtainOrb.textContent = `${left}s`;
  if (curtainStatus) curtainStatus.textContent = 'Take one slow breath… Impulse wait active before opening distracting app.';
  curtainTimer = setInterval(() => {
    left -= 1;
    if (left <= 0) {
      clearInterval(curtainTimer);
      curtainTimer = null;
      curtainOrb?.classList.remove('inhale');
      if (curtainOrb) curtainOrb.textContent = 'Ready';
      if (curtainStatus) curtainStatus.textContent = 'Choice unlocked: Proceed with intention (5m cooldown) or Return to Focus.';
    } else {
      curtainOrb?.classList.toggle('inhale');
      if (curtainOrb) curtainOrb.textContent = `${left}s`;
    }
  }, 1000);
});

returnFocusBtn?.addEventListener('click', () => {
  if (curtainTimer) clearInterval(curtainTimer);
  curtainTimer = null;
  curtainOrb?.classList.remove('inhale');
  if (curtainOrb) curtainOrb.textContent = 'Breathe';
  if (curtainStatus) curtainStatus.textContent = 'Session protected — Returned to Focus without opening distracting feed.';
});

// 6. Interactive DNS Domain Rule Tester (matches RuleEvaluator.evaluateDomain)
const dnsInput = document.getElementById('dnsInput');
const dnsTestBtn = document.getElementById('dnsTestBtn');
const dnsResult = document.getElementById('dnsResult');
const sampleBlocklist = ['youtube.com', 'instagram.com', 'tiktok.com', 'reddit.com'];
const sampleAllowlist = ['music.youtube.com'];

function normalizeDomain(raw) {
  let clean = (raw || '').trim().toLowerCase();
  clean = clean.replace(/^https?:\/\//, '');
  clean = clean.split('/')[0].split('?')[0].split(':')[0];
  return clean.replace(/\.+$/, '');
}

function domainMatches(query, rule) {
  return query === rule || query.endsWith('.' + rule);
}

function runDnsSimulation() {
  if (!dnsInput || !dnsResult) return;
  const normalized = normalizeDomain(dnsInput.value);
  if (!normalized || !normalized.includes('.')) {
    dnsResult.className = 'dns-result allowed';
    dnsResult.textContent = 'Enter a valid hostname such as youtube.com, m.youtube.com, or music.youtube.com.';
    return;
  }
  const allowMatch = sampleAllowlist.find((r) => domainMatches(normalized, r));
  if (allowMatch) {
    dnsResult.className = 'dns-result allowed';
    dnsResult.innerHTML = `ALLOWLISTED — <code>${escapeHtml(normalized)}</code> allowed by exception rule <code>${escapeHtml(allowMatch)}</code>.`;
    return;
  }
  const blockMatch = sampleBlocklist.find((r) => domainMatches(normalized, r));
  if (blockMatch) {
    dnsResult.className = 'dns-result blocked';
    dnsResult.innerHTML = `BLOCKED — <code>${escapeHtml(normalized)}</code> matches block rule <code>${escapeHtml(blockMatch)}</code> (local NXDOMAIN sinkhole).`;
    return;
  }
  dnsResult.className = 'dns-result allowed';
  dnsResult.innerHTML = `ALLOWED_NO_MATCH — <code>${escapeHtml(normalized)}</code> is not matched by active block rules.`;
}

dnsTestBtn?.addEventListener('click', runDnsSimulation);
dnsInput?.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') runDnsSimulation();
});

// 7. FAQ Accordion
document.querySelectorAll('.faq-q').forEach((btn) => {
  btn.addEventListener('click', () => {
    const answer = btn.nextElementSibling;
    const isOpen = answer.classList.toggle('open');
    btn.setAttribute('aria-expanded', String(isOpen));
    btn.querySelector('.faq-chevron').textContent = isOpen ? '−' : '+';
  });
});

// ==========================================================================
// 8. 3D Interactive Phone Tilt (Parallax & Depth)
// ==========================================================================
const phoneMockup = document.querySelector('.phone-mockup');
const phoneFrameEl = document.getElementById('phoneFrame');

if (phoneMockup && phoneFrameEl) {
  let isHoveringPhone = false;

  phoneMockup.addEventListener('mouseenter', () => {
    isHoveringPhone = true;
  });

  phoneMockup.addEventListener('mousemove', (e) => {
    if (!isHoveringPhone) return;
    const rect = phoneMockup.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const centerX = rect.width / 2;
    const centerY = rect.height / 2;

    const rotateY = ((x - centerX) / centerX) * 9; // -9 to +9 deg
    const rotateX = -((y - centerY) / centerY) * 9; // -9 to +9 deg

    phoneFrameEl.style.transform = `perspective(1200px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg) scale3d(1.02, 1.02, 1.02)`;
  });

  phoneMockup.addEventListener('mouseleave', () => {
    isHoveringPhone = false;
    phoneFrameEl.style.transform = 'perspective(1200px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
  });
}

// ==========================================================================
// 9. Staggered Scroll-Reveal Animations (IntersectionObserver)
// ==========================================================================
const revealTargets = document.querySelectorAll(
  '.card, .flow-node, .stat-card, .rule-card, .maintainer-card, .gallery-item, .faq-item, .table-wrap'
);

if ('IntersectionObserver' in window) {
  const revealObserver = new IntersectionObserver(
    (entries, observer) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-revealed');
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.12, rootMargin: '0px 0px -40px 0px' }
  );

  revealTargets.forEach((el, index) => {
    el.classList.add('reveal-init');
    // Stagger transition delay slightly within grids
    const delay = (index % 4) * 0.08;
    el.style.transitionDelay = `${delay}s`;
    revealObserver.observe(el);
  });
}

// ==========================================================================
// 10. Offline Procedural Tactile Audio Feedback (Zero Network, Web Audio API)
// ==========================================================================
let audioCtx = null;
function playTactileFeedback(freq = 600, duration = 0.04) {
  try {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextClass) return;
    if (!audioCtx) audioCtx = new AudioContextClass();
    if (audioCtx.state === 'suspended') audioCtx.resume();

    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq, audioCtx.currentTime);
    osc.frequency.exponentialRampToValueAtTime(Math.max(120, freq * 0.6), audioCtx.currentTime + duration);

    gain.gain.setValueAtTime(0.04, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.0001, audioCtx.currentTime + duration);

    osc.connect(gain);
    gain.connect(audioCtx.destination);

    osc.start();
    osc.stop(audioCtx.currentTime + duration);
  } catch (_) {
    // Graceful silent fallback
  }
}

// Attach tactile feedback to interactive controls
document.querySelectorAll('.btn, .phone-tab, .palette-btn, .theme-toggle-btn, .cat-chip').forEach((btn) => {
  btn.addEventListener('click', () => playTactileFeedback(640, 0.035), { passive: true });
});

