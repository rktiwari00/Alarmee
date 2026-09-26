// WakeWalk Interactive Landing Page Logic

document.addEventListener('DOMContentLoaded', () => {
  // --- Mobile Navigation Toggle ---
  const mobileToggle = document.getElementById('mobileToggle');
  const navLinks = document.getElementById('navLinks');

  if (mobileToggle && navLinks) {
    mobileToggle.addEventListener('click', () => {
      navLinks.classList.toggle('open');
    });

    navLinks.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', () => {
        navLinks.classList.remove('open');
      });
    });
  }

  // --- Hero Phone Mockup Simulation ---
  const heroSimulateStepBtn = document.getElementById('heroSimulateStepBtn');
  const heroStepCount = document.getElementById('heroStepCount');
  const heroProgressBar = document.getElementById('heroProgressBar');
  let heroSteps = 48;
  const heroTarget = 150;

  if (heroSimulateStepBtn && heroStepCount && heroProgressBar) {
    heroSimulateStepBtn.addEventListener('click', () => {
      if (heroSteps < heroTarget) {
        heroSteps += 5;
        if (heroSteps > heroTarget) heroSteps = heroTarget;
        heroStepCount.textContent = heroSteps;
        const pct = (heroSteps / heroTarget) * 100;
        heroProgressBar.style.width = `${pct}%`;

        if (heroSteps >= heroTarget) {
          heroSimulateStepBtn.textContent = '🎉 Goal Reached! Alarm Silenced';
          heroSimulateStepBtn.style.background = '#10B981';
        }
      }
    });
  }

  // --- Interactive Tabs ---
  const tabBtns = document.querySelectorAll('.demo-tab-btn');
  const tabPanes = document.querySelectorAll('.demo-tab-pane');

  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      tabBtns.forEach(b => b.classList.remove('active'));
      tabPanes.forEach(p => p.classList.remove('active'));

      btn.classList.add('active');
      const targetId = btn.getAttribute('data-tab');
      const targetPane = document.getElementById(targetId);
      if (targetPane) {
        targetPane.classList.add('active');
      }
    });
  });

  // --- Tab 1: Step Accumulator Simulation ---
  const demoWalkStepBtn = document.getElementById('demoWalkStepBtn');
  const demoShakeCheatBtn = document.getElementById('demoShakeCheatBtn');
  const demoResetBtn = document.getElementById('demoResetBtn');
  const demoStepVal = document.getElementById('demoStepVal');
  const demoCadenceStatus = document.getElementById('demoCadenceStatus');
  const demoLogText = document.getElementById('demoLogText');
  const demoCircleProgress = document.getElementById('demoCircleProgress');
  const demoPercentText = document.getElementById('demoPercentText');

  let currentSteps = 0;
  const targetSteps = 150;
  let lastStepTimestamp = 0;

  function updateProgressUI() {
    demoStepVal.textContent = currentSteps;
    const pct = Math.min(100, Math.round((currentSteps / targetSteps) * 100));
    demoPercentText.textContent = `${pct}%`;
    demoCircleProgress.setAttribute('stroke-dasharray', `${pct}, 100`);

    if (currentSteps >= targetSteps) {
      demoCadenceStatus.textContent = 'Challenge Completed!';
      demoCadenceStatus.style.color = '#10B981';
      demoLogText.textContent = '🌟 You are now completely awake! Alarm silenced.';
    }
  }

  if (demoWalkStepBtn) {
    demoWalkStepBtn.addEventListener('click', () => {
      const now = Date.now();
      const interval = now - lastStepTimestamp;
      lastStepTimestamp = now;

      currentSteps += 10;
      if (currentSteps > targetSteps) currentSteps = targetSteps;

      demoCadenceStatus.textContent = 'Valid Gait (1.8 Hz)';
      demoCadenceStatus.className = 'metric-value status-good';
      demoLogText.textContent = `✓ Authentic human step detected (${interval > 0 ? interval + 'ms' : 'normal'}). Step accepted!`;
      updateProgressUI();
    });
  }

  if (demoShakeCheatBtn) {
    demoShakeCheatBtn.addEventListener('click', () => {
      demoCadenceStatus.textContent = 'Cheat Detected!';
      demoCadenceStatus.className = 'metric-value';
      demoCadenceStatus.style.color = '#F43F5E';
      demoLogText.textContent = '⚠️ Interval < 320ms or chaotic 3D spike! Rapid shake rejected by anti-cheat filter.';
    });
  }

  if (demoResetBtn) {
    demoResetBtn.addEventListener('click', () => {
      currentSteps = 0;
      lastStepTimestamp = 0;
      demoCadenceStatus.textContent = 'Ready';
      demoCadenceStatus.className = 'metric-value status-good';
      demoLogText.textContent = 'Simulator reset. Click "Take Authentic Step" to test.';
      updateProgressUI();
    });
  }

  // --- Tab 2: QR Scanner Simulation ---
  const demoScanQrBtn = document.getElementById('demoScanQrBtn');
  const scannerLaser = document.getElementById('scannerLaser');
  const scannerStatusText = document.getElementById('scannerStatusText');
  const demoQrStatus = document.getElementById('demoQrStatus');
  const scannerVisualFrame = document.getElementById('scannerVisualFrame');

  if (demoScanQrBtn && scannerLaser && scannerStatusText && demoQrStatus) {
    demoScanQrBtn.addEventListener('click', () => {
      scannerLaser.style.display = 'block';
      scannerStatusText.textContent = 'Scanning Toothpaste Code...';
      demoQrStatus.textContent = 'Camera active: evaluating barcode pattern...';

      setTimeout(() => {
        scannerLaser.style.display = 'none';
        scannerStatusText.textContent = '✓ CODE VERIFIED!';
        scannerStatusText.style.color = '#10B981';
        if (scannerVisualFrame) {
          scannerVisualFrame.style.borderColor = '#10B981';
        }
        demoQrStatus.innerHTML = '<strong style="color:#10B981;">✓ Toothpaste Code 012000046452 matches!</strong> Physical location confirmed. Alarm silenced!';
      }, 1800);
    });
  }

  // --- Tab 3: Ringtone Audio Simulation ---
  const demoPlayRingtoneBtn = document.getElementById('demoPlayRingtoneBtn');
  const demoStopRingtoneBtn = document.getElementById('demoStopRingtoneBtn');
  const audioWavesBox = document.getElementById('audioWavesBox');
  const volumeRampLevel = document.getElementById('volumeRampLevel');
  const volumeRampBar = document.getElementById('volumeRampBar');
  const demoAudioHint = document.getElementById('demoAudioHint');

  let rampInterval = null;
  let currentVol = 20;

  if (demoPlayRingtoneBtn && audioWavesBox && volumeRampLevel && volumeRampBar) {
    demoPlayRingtoneBtn.addEventListener('click', () => {
      clearInterval(rampInterval);
      audioWavesBox.classList.add('playing');
      currentVol = 20;
      volumeRampLevel.textContent = `${currentVol}% Volume`;
      volumeRampBar.style.width = `${currentVol}%`;
      demoAudioHint.textContent = '📞 Incoming phone call tone ringing on STREAM_ALARM. Ramping up...';

      rampInterval = setInterval(() => {
        if (currentVol < 100) {
          currentVol += 10;
          volumeRampLevel.textContent = `${currentVol}% Volume`;
          volumeRampBar.style.width = `${currentVol}%`;
        } else {
          clearInterval(rampInterval);
          demoAudioHint.textContent = '🔊 Max volume reached! Playing continuously until challenge completion.';
        }
      }, 600);
    });
  }

  if (demoStopRingtoneBtn && audioWavesBox) {
    demoStopRingtoneBtn.addEventListener('click', () => {
      clearInterval(rampInterval);
      audioWavesBox.classList.remove('playing');
      currentVol = 20;
      volumeRampLevel.textContent = 'Silenced (0%)';
      volumeRampBar.style.width = '0%';
      demoAudioHint.textContent = '⏹ Alarm audio silenced.';
    });
  }

  // --- GA4 Event Tracking for Conversions ---
  function trackEvent(eventName, eventParams = {}) {
    if (typeof gtag === 'function') {
      gtag('event', eventName, eventParams);
    }
  }

  const trackBtn = (id, eventName, params) => {
    const el = document.getElementById(id);
    if (el) {
      el.addEventListener('click', () => trackEvent(eventName, params));
    }
  };

  trackBtn('nav-download-btn', 'download_apk', { location: 'navbar' });
  trackBtn('hero-download-btn', 'download_apk', { location: 'hero' });
  trackBtn('hero-source-btn', 'view_source', { location: 'hero' });
  trackBtn('github-star-btn', 'star_github', { location: 'navbar' });
  trackBtn('coffee-btn', 'support_coffee_click', { method: 'buymeacoffee' });
  trackBtn('github-sponsor-btn', 'support_sponsor_click', { method: 'github_sponsors' });

  // --- FAQ Accordion Logic ---
  const faqItems = document.querySelectorAll('.faq-item');
  faqItems.forEach(item => {
    const questionBtn = item.querySelector('.faq-question');
    if (questionBtn) {
      questionBtn.addEventListener('click', () => {
        const isActive = item.classList.contains('active');
        // Close other open FAQ items
        faqItems.forEach(other => {
          if (other !== item) {
            other.classList.remove('active');
            const otherBtn = other.querySelector('.faq-question');
            if (otherBtn) otherBtn.setAttribute('aria-expanded', 'false');
          }
        });

        // Toggle current item
        item.classList.toggle('active', !isActive);
        questionBtn.setAttribute('aria-expanded', (!isActive).toString());
        if (!isActive) {
          trackEvent('faq_expand', { question: questionBtn.querySelector('span')?.textContent || 'FAQ' });
        }
      });
    }
  });
});


