const phoneScreens = {
  home: `
    <div class="p-head">
      <span class="p-title">Home Dashboard</span>
      <span class="p-badge">Measured • Today</span>
    </div>
    <div class="p-ring">
      <strong>1h 42m</strong>
      <span>of 3h daily goal</span>
    </div>
    <div class="p-card highlight">
      <div class="p-card-title">Daily Intention</div>
      <div class="p-card-sub">"Finish thesis draft before checking social feeds."</div>
    </div>
    <div class="p-card">
      <div class="p-card-title">Quick Sanctuary Actions</div>
      <div class="p-chips">
        <span class="p-chip active">25m Study</span>
        <span class="p-chip">45m Coding</span>
        <span class="p-chip">Shield On</span>
      </div>
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
      <div class="p-card-sub">3 sessions completed • 75m • 7-day streak</div>
    </div>
  `,
  habits: `
    <div class="p-head">
      <span class="p-title">Offline Habits &amp; Wind-Down</span>
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
      <span class="p-title">Shield &amp; Launch Pause</span>
      <span class="p-badge">5 rules active</span>
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
      <div class="p-card-sub">VpnService active • Short-Form &amp; Social pack • 14 blocked DNS hits</div>
    </div>
    <div class="p-card">
      <div class="p-card-title">App Limits + Grace</div>
      <div class="p-card-sub">Daily caps + 5m grace buffer • Zero telemetry</div>
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

tabButtons.forEach((btn) => {
  btn.addEventListener('click', () => renderPhoneTab(btn.dataset.screen));
});
renderPhoneTab('home');

// Theme switcher (Light / Dark)
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

// Accent Palette switcher
document.querySelectorAll('[data-set-palette]').forEach((btn) => {
  btn.addEventListener('click', () => {
    const palette = btn.getAttribute('data-set-palette');
    document.documentElement.setAttribute('data-palette', palette);
    document.querySelectorAll('[data-set-palette]').forEach((b) => {
      b.classList.toggle('active', b === btn);
    });
  });
});

// FAQ Accordion
document.querySelectorAll('.faq-q').forEach((btn) => {
  btn.addEventListener('click', () => {
    const answer = btn.nextElementSibling;
    const isOpen = answer.classList.toggle('open');
    btn.setAttribute('aria-expanded', String(isOpen));
    btn.querySelector('.faq-chevron').textContent = isOpen ? '−' : '+';
  });
});
