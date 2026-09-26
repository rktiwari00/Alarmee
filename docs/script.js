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
  const heroVolumePill = document.getElementById('heroVolumePill');
  const heroVolumeText = document.getElementById('heroVolumeText');
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

        if (heroVolumePill && heroVolumeText) {
          if (heroSteps >= 10 && heroSteps < heroTarget) {
            heroVolumePill.className = 'mockup-volume-pill ducked';
            heroVolumeText.textContent = '🚶 30% Volume (Walking Active)';
          }
        }

        if (heroSteps >= heroTarget) {
          heroSimulateStepBtn.textContent = '🎉 Goal Reached! Alarm Silenced';
          heroSimulateStepBtn.style.background = '#10B981';
          if (heroVolumePill && heroVolumeText) {
            heroVolumePill.className = 'mockup-volume-pill';
            heroVolumeText.textContent = '✓ Challenge Complete';
          }
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

  // --- Tab 1: Step Accumulator & Dynamic Volume Simulation ---
  const demoWalkStepBtn = document.getElementById('demoWalkStepBtn');
  const demoShakeCheatBtn = document.getElementById('demoShakeCheatBtn');
  const demoPauseWatchdogBtn = document.getElementById('demoPauseWatchdogBtn');
  const demoResetBtn = document.getElementById('demoResetBtn');
  const demoStepVal = document.getElementById('demoStepVal');
  const demoCadenceStatus = document.getElementById('demoCadenceStatus');
  const demoLogText = document.getElementById('demoLogText');
  const demoCircleProgress = document.getElementById('demoCircleProgress');
  const demoPercentText = document.getElementById('demoPercentText');
  const simVolumePill = document.getElementById('simVolumePill');
  const simVolumeText = document.getElementById('simVolumeText');

  let currentSteps = 0;
  const targetSteps = 150;
  let lastStepTimestamp = 0;
  let watchdogTimeout = null;

  function updateProgressUI() {
    demoStepVal.textContent = currentSteps;
    const pct = Math.min(100, Math.round((currentSteps / targetSteps) * 100));
    demoPercentText.textContent = `${pct}%`;
    demoCircleProgress.setAttribute('stroke-dasharray', `${pct}, 100`);

    if (simVolumePill && simVolumeText) {
      if (currentSteps === 0) {
        simVolumePill.className = 'simulator-volume-pill';
        simVolumeText.textContent = '🔊 100% Hardcore Volume';
      } else if (currentSteps >= targetSteps) {
        simVolumePill.className = 'simulator-volume-pill ducked';
        simVolumeText.textContent = '🎉 Silenced (Goal Reached)';
      } else if (currentSteps >= 10) {
        simVolumePill.className = 'simulator-volume-pill ducked';
        simVolumeText.textContent = '🚶 30% Volume (Walking Active - Ducked!)';
      }
    }

    if (currentSteps >= targetSteps) {
      demoCadenceStatus.textContent = 'Challenge Completed!';
      demoCadenceStatus.style.color = '#10B981';
      demoLogText.textContent = '🌟 You are now completely awake! Alarm silenced.';
    }
  }

  if (demoWalkStepBtn) {
    demoWalkStepBtn.addEventListener('click', () => {
      clearTimeout(watchdogTimeout);
      const now = Date.now();
      const interval = now - lastStepTimestamp;
      lastStepTimestamp = now;

      currentSteps += 10;
      if (currentSteps > targetSteps) currentSteps = targetSteps;

      demoCadenceStatus.textContent = 'Valid Gait (1.8 Hz)';
      demoCadenceStatus.className = 'metric-value status-good';
      if (currentSteps < 10) {
        demoLogText.textContent = `✓ Authentic human step detected (${interval > 0 ? interval + 'ms' : 'normal'}). Alarm at 100% volume until 10 steps!`;
      } else if (currentSteps < targetSteps) {
        demoLogText.textContent = `✓ Authentic walking active! 10 steps reached: volume ducked down to 30% relief!`;
      }
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

  if (demoPauseWatchdogBtn) {
    demoPauseWatchdogBtn.addEventListener('click', () => {
      if (currentSteps === 0) {
        demoLogText.textContent = 'Take a few authentic steps first, then test pausing!';
        return;
      }
      clearTimeout(watchdogTimeout);
      demoCadenceStatus.textContent = 'Walking Paused';
      demoCadenceStatus.className = 'metric-value';
      demoCadenceStatus.style.color = '#F59E0B';
      demoLogText.textContent = '⏳ Stopped walking... 8-second anti-slacking watchdog timer started...';

      watchdogTimeout = setTimeout(() => {
        if (simVolumePill && simVolumeText && currentSteps < targetSteps) {
          simVolumePill.className = 'simulator-volume-pill warning';
          simVolumeText.textContent = '⚠️ Keep Moving! Volume ramping to 100%!';
          demoLogText.textContent = '⚠️ 8 seconds of inactivity detected! Volume ramping back up to 100% hardcore volume to prevent falling back to sleep!';
          demoCadenceStatus.textContent = 'Watchdog Alert';
          demoCadenceStatus.style.color = '#F43F5E';
        }
      }, 2500); // 2.5s accelerated for web simulator
    });
  }

  if (demoResetBtn) {
    demoResetBtn.addEventListener('click', () => {
      clearTimeout(watchdogTimeout);
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

  // --- Tab 3: Ringtone Audio & Sound Bank Simulation ---
  const demoPlayRingtoneBtn = document.getElementById('demoPlayRingtoneBtn');
  const demoDuckVolumeBtn = document.getElementById('demoDuckVolumeBtn');
  const demoStopRingtoneBtn = document.getElementById('demoStopRingtoneBtn');
  const audioWavesBox = document.getElementById('audioWavesBox');
  const volumeRampLevel = document.getElementById('volumeRampLevel');
  const volumeRampBar = document.getElementById('volumeRampBar');
  const volumeStatusTitle = document.getElementById('volumeStatusTitle');
  const demoAudioHint = document.getElementById('demoAudioHint');
  const soundCatBtns = document.querySelectorAll('.sound-cat-btn');

  let selectedCategory = 'phone';
  let audioCtx = null;
  let isPlayingAudio = false;
  let synthNodes = [];
  let masterGain = null;

  // Sound Category Selection
  soundCatBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      soundCatBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      selectedCategory = btn.getAttribute('data-sound');

      const catDescriptions = {
        phone: '📞 Phone Call: Incoming emergency ringtone designed to trigger cognitive alertness.',
        harsh: '⚡ Harsh Klaxon: Aggressive dual-tone alerting siren designed to break deep sleep inertia.',
        smooth: '🌿 Smooth Harmony: Gentle, resonant sunrise chords for calm morning awakening.',
        random: '🎲 Daily Random: Automatically picks a different sound category every single morning!'
      };
      demoAudioHint.textContent = catDescriptions[selectedCategory] || '';

      if (isPlayingAudio) {
        stopAudioSynthesis();
        playAudioSynthesis();
      }
    });
  });

  function stopAudioSynthesis() {
    synthNodes.forEach(node => {
      try { node.stop(); } catch(e) {}
      try { node.disconnect(); } catch(e) {}
    });
    synthNodes = [];
    isPlayingAudio = false;
    if (audioWavesBox) audioWavesBox.classList.remove('playing');
  }

  function playAudioSynthesis() {
    stopAudioSynthesis();

    try {
      const AudioContextClass = window.AudioContext || window.webkitAudioContext;
      if (!audioCtx) audioCtx = new AudioContextClass();
      if (audioCtx.state === 'suspended') audioCtx.resume();

      masterGain = audioCtx.createGain();
      masterGain.gain.setValueAtTime(0.5, audioCtx.currentTime); // 100% normalized baseline
      masterGain.connect(audioCtx.destination);

      let effectiveCat = selectedCategory;
      if (effectiveCat === 'random') {
        const pool = ['phone', 'harsh', 'smooth'];
        effectiveCat = pool[Math.floor(Math.random() * pool.length)];
      }

      const now = audioCtx.currentTime;

      if (effectiveCat === 'phone') {
        // Classic 440Hz + 480Hz phone ring
        const osc1 = audioCtx.createOscillator();
        const osc2 = audioCtx.createOscillator();
        const ringGain = audioCtx.createGain();

        osc1.frequency.setValueAtTime(440, now);
        osc2.frequency.setValueAtTime(480, now);

        // Ring cadence: 1.2s on, 1.8s off
        ringGain.gain.setValueAtTime(0.3, now);
        for (let i = 0; i < 20; i++) {
          const t = now + i * 3.0;
          ringGain.gain.setValueAtTime(0.3, t);
          ringGain.gain.setValueAtTime(0.3, t + 1.2);
          ringGain.gain.setValueAtTime(0.001, t + 1.25);
          ringGain.gain.setValueAtTime(0.001, t + 3.0);
        }

        osc1.connect(ringGain);
        osc2.connect(ringGain);
        ringGain.connect(masterGain);

        osc1.start(now);
        osc2.start(now);
        synthNodes.push(osc1, osc2, ringGain);
      } else if (effectiveCat === 'harsh') {
        // Dissonant siren klaxon 880Hz / 660Hz
        const osc = audioCtx.createOscillator();
        osc.type = 'sawtooth';
        for (let i = 0; i < 40; i++) {
          const t = now + i * 0.4;
          osc.frequency.setValueAtTime(880, t);
          osc.frequency.setValueAtTime(660, t + 0.2);
        }
        const klaxonGain = audioCtx.createGain();
        klaxonGain.gain.setValueAtTime(0.2, now);
        osc.connect(klaxonGain);
        klaxonGain.connect(masterGain);
        osc.start(now);
        synthNodes.push(osc, klaxonGain);
      } else {
        // Smooth chord arpeggio (C4, E4, G4, C5)
        const notes = [261.63, 329.63, 392.00, 523.25];
        notes.forEach((freq, idx) => {
          const osc = audioCtx.createOscillator();
          const noteGain = audioCtx.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(freq, now);

          noteGain.gain.setValueAtTime(0.001, now);
          for (let i = 0; i < 15; i++) {
            const t = now + i * 2.0 + idx * 0.3;
            noteGain.gain.linearRampToValueAtTime(0.12, t + 0.1);
            noteGain.gain.exponentialRampToValueAtTime(0.001, t + 1.2);
          }

          osc.connect(noteGain);
          noteGain.connect(masterGain);
          osc.start(now);
          synthNodes.push(osc, noteGain);
        });
      }

      isPlayingAudio = true;
      if (audioWavesBox) audioWavesBox.classList.add('playing');
    } catch (e) {
      console.warn('Web Audio synthesis error:', e);
    }
  }

  if (demoPlayRingtoneBtn) {
    demoPlayRingtoneBtn.addEventListener('click', () => {
      playAudioSynthesis();
      if (volumeRampLevel) volumeRampLevel.textContent = '100% Volume';
      if (volumeRampBar) {
        volumeRampBar.style.width = '100%';
        volumeRampBar.style.background = 'linear-gradient(90deg, #6366F1, #F43F5E)';
      }
      if (volumeStatusTitle) volumeStatusTitle.textContent = 'Volume: Hardcore Max Level (100%)';
      if (demoAudioHint) demoAudioHint.textContent = `🔊 Playing ${selectedCategory.toUpperCase()} at 100% volume. Click "Duck Volume" to simulate walking 10 steps!`;
    });
  }

  if (demoDuckVolumeBtn) {
    demoDuckVolumeBtn.addEventListener('click', () => {
      if (!isPlayingAudio) {
        playAudioSynthesis();
      }
      if (masterGain && audioCtx) {
        masterGain.gain.linearRampToValueAtTime(0.15, audioCtx.currentTime + 0.8);
      }
      if (volumeRampLevel) volumeRampLevel.textContent = '30% Volume (Ducked)';
      if (volumeRampBar) {
        volumeRampBar.style.width = '30%';
        volumeRampBar.style.background = 'linear-gradient(90deg, #10B981, #34D399)';
      }
      if (volumeStatusTitle) volumeStatusTitle.textContent = 'Volume: 30% (Walking Active Ducked)';
      if (demoAudioHint) demoAudioHint.textContent = '🚶 10 steps detected! Volume smoothly ducked down to 30% for your ears and household.';
    });
  }

  if (demoStopRingtoneBtn) {
    demoStopRingtoneBtn.addEventListener('click', () => {
      stopAudioSynthesis();
      if (volumeRampLevel) volumeRampLevel.textContent = 'Silenced (0%)';
      if (volumeRampBar) volumeRampBar.style.width = '0%';
      if (volumeStatusTitle) volumeStatusTitle.textContent = 'Volume: Silenced';
      if (demoAudioHint) demoAudioHint.textContent = '⏹ Alarm sound stopped.';
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


