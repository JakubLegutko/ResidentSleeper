// ResidentSleeper Interactive Web Simulator Engine
(function () {
  'use strict';

  // --- State & Storage ---
  const STORAGE_KEY = 'residentsleeper_sim_state';

  let state = {
    activeProfileId: 1,
    simTimeOffsetMs: 0,
    selectedDayStart: getStartOfDay(Date.now()),
    activeView: 'dashboard', // 'dashboard', 'reviews', 'settings'
    reviewPeriod: 'daily',   // 'daily', 'weekly', 'monthly'
    profiles: [
      {
        id: 1,
        name: 'Emma',
        birthTimestamp: Date.now() - (28 * 24 * 60 * 60 * 1000), // 4 weeks old
        isActive: true,
        customWakeWindowMinutes: null,
        feedingIntervalMinutes: 150,
        selectedCalendarId: 1,
        notifyBeforeMinutes: 10,
        enableCalendarSync: true,
        enablePushNotifications: true
      },
      {
        id: 2,
        name: 'Lucas',
        birthTimestamp: Date.now() - (60 * 24 * 60 * 60 * 1000), // ~8 weeks old
        isActive: false,
        customWakeWindowMinutes: null,
        feedingIntervalMinutes: 180,
        selectedCalendarId: null,
        notifyBeforeMinutes: 10,
        enableCalendarSync: false,
        enablePushNotifications: true
      }
    ],
    events: []
  };

  // Load from LocalStorage if available
  function loadState() {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      try {
        const saved = JSON.parse(raw);
        if (saved && saved.profiles && saved.profiles.length) {
          state.profiles = saved.profiles;
          state.events = saved.events || [];
          state.activeProfileId = saved.activeProfileId || 1;
        }
      } catch (e) {
        console.error('Failed to parse saved state:', e);
      }
    }
    if (state.events.length === 0) {
      seedRealisticData();
    }
  }

  function saveState() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      profiles: state.profiles,
      events: state.events,
      activeProfileId: state.activeProfileId
    }));
  }

  function getSimulatedNow() {
    return Date.now() + state.simTimeOffsetMs;
  }

  function getActiveProfile() {
    return state.profiles.find(p => p.id === state.activeProfileId) || state.profiles[0];
  }

  function getStartOfDay(timestamp) {
    const d = new Date(timestamp);
    d.setHours(0, 0, 0, 0);
    return d.getTime();
  }

  function getEndOfDay(timestamp) {
    const d = new Date(timestamp);
    d.setHours(23, 59, 59, 999);
    return d.getTime();
  }

  // --- Seed Realistic 24-Hour Newborn Data ---
  function seedRealisticData() {
    const now = getSimulatedNow();
    const startOfToday = getStartOfDay(now);

    state.events = [
      // Night sleep: 21:00 yesterday to 07:00 today (split at midnight)
      {
        id: 1,
        babyProfileId: 1,
        type: 'SLEEP',
        startTime: startOfToday - (3 * 60 * 60 * 1000), // 21:00 yesterday
        endTime: startOfToday + (7 * 60 * 60 * 1000)    // 07:00 today
      },
      // 07:00 Wakeup diaper & nursing
      {
        id: 2,
        babyProfileId: 1,
        type: 'DIAPER',
        startTime: startOfToday + (7 * 60 * 60 * 1000) + (5 * 60 * 1000),
        diaperType: 'PEE'
      },
      {
        id: 3,
        babyProfileId: 1,
        type: 'NURSING',
        startTime: startOfToday + (7 * 60 * 60 * 1000) + (10 * 60 * 1000),
        endTime: startOfToday + (7 * 60 * 60 * 1000) + (35 * 60 * 1000),
        nursingType: 'LEFT_BREAST'
      },
      // Nap 1: 08:00 to 09:30
      {
        id: 4,
        babyProfileId: 1,
        type: 'SLEEP',
        startTime: startOfToday + (8 * 60 * 60 * 1000),
        endTime: startOfToday + (9 * 60 * 60 * 1000) + (30 * 60 * 1000)
      },
      // 09:35 Diaper Poo
      {
        id: 5,
        babyProfileId: 1,
        type: 'DIAPER',
        startTime: startOfToday + (9 * 60 * 60 * 1000) + (35 * 60 * 1000),
        diaperType: 'POO'
      },
      // 09:40 Feeding (Right Breast)
      {
        id: 6,
        babyProfileId: 1,
        type: 'NURSING',
        startTime: startOfToday + (9 * 60 * 60 * 1000) + (40 * 60 * 1000),
        endTime: startOfToday + (10 * 60 * 60 * 1000) + (10 * 60 * 1000),
        nursingType: 'RIGHT_BREAST'
      },
      // Nap 2: 10:30 to 12:30
      {
        id: 7,
        babyProfileId: 1,
        type: 'SLEEP',
        startTime: startOfToday + (10 * 60 * 60 * 1000) + (30 * 60 * 1000),
        endTime: startOfToday + (12 * 60 * 60 * 1000) + (30 * 60 * 1000)
      },
      // 12:35 Diaper Pee
      {
        id: 8,
        babyProfileId: 1,
        type: 'DIAPER',
        startTime: startOfToday + (12 * 60 * 60 * 1000) + (35 * 60 * 1000),
        diaperType: 'PEE'
      },
      // 12:40 Bottle Feeding (90ml)
      {
        id: 9,
        babyProfileId: 1,
        type: 'NURSING',
        startTime: startOfToday + (12 * 60 * 60 * 1000) + (40 * 60 * 1000),
        endTime: startOfToday + (13 * 60 * 60 * 1000) + (0 * 60 * 1000),
        nursingType: 'BOTTLE',
        amountMl: 90
      }
    ];
    saveState();
  }

  // --- Calculations: Wake Window & Feeding Predictions ---
  function calculateAgeInWeeks(birthTimestamp) {
    const diff = Math.max(0, getSimulatedNow() - birthTimestamp);
    return Math.floor(diff / (7 * 24 * 60 * 60 * 1000));
  }

  function getRecommendedWakeWindow(ageWeeks) {
    if (ageWeeks < 5) return 50;
    if (ageWeeks < 9) return 65;
    if (ageWeeks < 13) return 80;
    if (ageWeeks < 17) return 100;
    return 120;
  }

  function computeWakeWindowState(profile) {
    const now = getSimulatedNow();
    const ageWeeks = calculateAgeInWeeks(profile.birthTimestamp);
    const targetMinutes = profile.customWakeWindowMinutes || getRecommendedWakeWindow(ageWeeks);

    const profileEvents = state.events.filter(e => e.babyProfileId === profile.id);
    const sleepEvents = profileEvents.filter(e => e.type === 'SLEEP').sort((a, b) => b.startTime - a.startTime);
    const ongoingSleep = sleepEvents.find(e => !e.endTime);

    if (ongoingSleep) {
      const durMin = Math.floor(Math.max(0, now - ongoingSleep.startTime) / 60000);
      return {
        isSleeping: true,
        durationMinutes: durMin,
        targetMinutes,
        remainingMinutes: null,
        statusText: `Sleeping (${durMin}m)`,
        isAlert10m: false
      };
    }

    const latestSleep = sleepEvents[0];
    const wakeStart = latestSleep ? (latestSleep.endTime || latestSleep.startTime) : (now - 25 * 60 * 1000);
    const wakeDurationMinutes = Math.floor(Math.max(0, now - wakeStart) / 60000);
    const expectedEnd = wakeStart + (targetMinutes * 60 * 1000);
    const diffMinutes = Math.floor((expectedEnd - now) / 60000);

    return {
      isSleeping: false,
      durationMinutes: wakeDurationMinutes,
      targetMinutes,
      remainingMinutes: diffMinutes,
      statusText: diffMinutes >= 0 ? `Window ends in ${diffMinutes}m` : `Overdue by ${-diffMinutes}m`,
      isAlert10m: diffMinutes <= 10 && diffMinutes >= 0
    };
  }

  function computeFeedingState(profile) {
    const now = getSimulatedNow();
    const profileEvents = state.events.filter(e => e.babyProfileId === profile.id);
    const nursingEvents = profileEvents.filter(e => e.type === 'NURSING').sort((a, b) => b.startTime - a.startTime);
    const ongoing = nursingEvents.find(e => !e.endTime);

    if (ongoing) {
      const dur = Math.floor((now - ongoing.startTime) / 60000);
      return {
        isNursing: true,
        durationMinutes: dur,
        text: `Nursing now (${dur}m)`,
        isAlert10m: false
      };
    }

    const latest = nursingEvents[0];
    if (!latest) {
      return { isNursing: false, text: 'No feeds logged', isAlert10m: false };
    }

    const intervalMs = profile.feedingIntervalMinutes * 60 * 1000;
    const nextFeedEstimate = latest.startTime + intervalMs;
    const diffMinutes = Math.floor((nextFeedEstimate - now) / 60000);

    return {
      isNursing: false,
      diffMinutes,
      text: diffMinutes >= 0 ? `Next feed in ${diffMinutes}m` : `Feed due now!`,
      isAlert10m: diffMinutes <= 10 && diffMinutes >= 0
    };
  }

  // --- 24-Hour Donut Chart Rendering (Canvas) ---
  const canvas = document.getElementById('clock-chart-canvas');
  const ctx = canvas.getContext('2d');

  function renderClockChart() {
    const profile = getActiveProfile();
    const profileEvents = state.events.filter(e => e.babyProfileId === profile.id);

    const dayStart = state.selectedDayStart;
    const dayEnd = getEndOfDay(dayStart);

    const width = canvas.width;
    const height = canvas.height;
    const centerX = width / 2;
    const centerY = height / 2;
    const strokeWidth = 28;
    const radius = (width - strokeWidth - 28) / 2;

    ctx.clearRect(0, 0, width, height);

    // 1. Draw Ring Background Track
    ctx.beginPath();
    ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI);
    ctx.strokeStyle = '#1e293b';
    ctx.lineWidth = strokeWidth;
    ctx.lineCap = 'butt';
    ctx.stroke();

    // 2. Draw Hour Ticks & Labels (00, 06, 12, 18)
    for (let h = 0; h < 24; h++) {
      // 00:00 at top (-PI/2), moving clockwise
      const angle = (h / 24) * 2 * Math.PI - Math.PI / 2;
      const isMajor = h % 3 === 0;
      const innerR = isMajor ? radius - strokeWidth / 2 - 6 : radius - strokeWidth / 2 - 2;
      const outerR = radius - strokeWidth / 2 + 1;

      const x1 = centerX + innerR * Math.cos(angle);
      const y1 = centerY + innerR * Math.sin(angle);
      const x2 = centerX + outerR * Math.cos(angle);
      const y2 = centerY + outerR * Math.sin(angle);

      ctx.beginPath();
      ctx.moveTo(x1, y1);
      ctx.lineTo(x2, y2);
      ctx.strokeStyle = isMajor ? 'rgba(255, 255, 255, 0.4)' : 'rgba(255, 255, 255, 0.15)';
      ctx.lineWidth = isMajor ? 2 : 1;
      ctx.stroke();

      if (h % 6 === 0) {
        const labelR = radius + strokeWidth / 2 + 10;
        const lx = centerX + labelR * Math.cos(angle);
        const ly = centerY + labelR * Math.sin(angle) + 4;
        ctx.fillStyle = '#94a3b8';
        ctx.font = '10px Plus Jakarta Sans, sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(h < 10 ? '0' + h : '' + h, lx, ly);
      }
    }

    // Helper to draw an arc between two timestamps
    function drawTimeArc(startTime, endTime, color, isSleep = false) {
      const clampedStart = Math.max(dayStart, startTime);
      const clampedEnd = Math.min(dayEnd, endTime);
      if (clampedStart >= clampedEnd) return;

      const startMin = (new Date(clampedStart).getHours() * 60) + new Date(clampedStart).getMinutes();
      const endMin = (new Date(clampedEnd).getHours() * 60) + new Date(clampedEnd).getMinutes();

      ctx.lineWidth = strokeWidth;
      ctx.lineCap = 'butt';

      const sleepGrad = isSleep || color === '#4f46e5';

      if (endMin >= startMin) {
        const startAngle = (startMin / 1440) * 2 * Math.PI - Math.PI / 2;
        const endAngle = (endMin / 1440) * 2 * Math.PI - Math.PI / 2;

        const p1X = centerX + radius * Math.cos(startAngle);
        const p1Y = centerY + radius * Math.sin(startAngle);
        const p2X = centerX + radius * Math.cos(endAngle);
        const p2Y = centerY + radius * Math.sin(endAngle);

        const grad = ctx.createLinearGradient(p1X, p1Y, p2X, p2Y);
        if (sleepGrad) {
          grad.addColorStop(0, '#6366f1'); // Luminous smooth indigo
          grad.addColorStop(1, '#3730a3'); // Deep night indigo
        } else {
          grad.addColorStop(0, '#34d399'); // Vibrant emerald mint
          grad.addColorStop(1, '#059669'); // Rich deep mint
        }

        ctx.strokeStyle = grad;
        ctx.beginPath();
        ctx.arc(centerX, centerY, radius, startAngle, endAngle);
        ctx.stroke();
      } else {
        const a1 = (startMin / 1440) * 2 * Math.PI - Math.PI / 2;
        const a2 = 2 * Math.PI - Math.PI / 2;
        const p1X = centerX + radius * Math.cos(a1);
        const p1Y = centerY + radius * Math.sin(a1);
        const p2X = centerX + radius * Math.cos(a2);
        const p2Y = centerY + radius * Math.sin(a2);

        const grad1 = ctx.createLinearGradient(p1X, p1Y, p2X, p2Y);
        if (sleepGrad) {
          grad1.addColorStop(0, '#6366f1');
          grad1.addColorStop(1, '#4338ca');
        } else {
          grad1.addColorStop(0, '#34d399');
          grad1.addColorStop(1, '#059669');
        }

        ctx.strokeStyle = grad1;
        ctx.beginPath();
        ctx.arc(centerX, centerY, radius, a1, a2);
        ctx.stroke();

        const b1 = -Math.PI / 2;
        const b2 = (endMin / 1440) * 2 * Math.PI - Math.PI / 2;
        const pb1X = centerX + radius * Math.cos(b1);
        const pb1Y = centerY + radius * Math.sin(b1);
        const pb2X = centerX + radius * Math.cos(b2);
        const pb2Y = centerY + radius * Math.sin(b2);

        const grad2 = ctx.createLinearGradient(pb1X, pb1Y, pb2X, pb2Y);
        if (sleepGrad) {
          grad2.addColorStop(0, '#4338ca');
          grad2.addColorStop(1, '#3730a3');
        } else {
          grad2.addColorStop(0, '#10b981');
          grad2.addColorStop(1, '#047857');
        }

        ctx.strokeStyle = grad2;
        ctx.beginPath();
        ctx.arc(centerX, centerY, radius, b1, b2);
        ctx.stroke();
      }
    }

    // 3. Draw Activity & Sleep Intervals
    const sleepEvents = profileEvents.filter(e => e.type === 'SLEEP');

    // 3A. Draw Activity / Wake Intervals in Mint (#10b981)
    const effectiveLimit = Math.min(dayEnd, dayEnd > getSimulatedNow() ? getSimulatedNow() : dayEnd);
    const sortedSleeps = sleepEvents.slice().sort((a, b) => a.startTime - b.startTime);

    if (sortedSleeps.length === 0) {
      if (dayStart < effectiveLimit) {
        drawTimeArc(dayStart, effectiveLimit, '#10b981');
      }
    } else {
      if (sortedSleeps[0].startTime > dayStart) {
        drawTimeArc(dayStart, Math.min(sortedSleeps[0].startTime, effectiveLimit), '#10b981');
      }
      for (let i = 0; i < sortedSleeps.length - 1; i++) {
        const currentEnd = sortedSleeps[i].endTime || getSimulatedNow();
        const nextStart = sortedSleeps[i + 1].startTime;
        if (nextStart > currentEnd && currentEnd < effectiveLimit) {
          drawTimeArc(currentEnd, Math.min(nextStart, effectiveLimit), '#10b981');
        }
      }
      const lastSleep = sortedSleeps[sortedSleeps.length - 1];
      const lastEnd = lastSleep.endTime;
      if (lastEnd && lastEnd < effectiveLimit) {
        drawTimeArc(lastEnd, effectiveLimit, '#10b981');
      }
    }

    // 3B. Draw Sleep Intervals in Indigo (#4f46e5)
    for (const s of sleepEvents) {
      drawTimeArc(s.startTime, s.endTime || getSimulatedNow(), '#4f46e5');
    }


    // 4. Draw Markers (Nursing & Diapers)
    for (const e of profileEvents) {
      if (e.startTime >= dayStart && e.startTime <= dayEnd) {
        const d = new Date(e.startTime);
        const min = d.getHours() * 60 + d.getMinutes();
        const angle = (min / 1440) * 2 * Math.PI - Math.PI / 2;

        if (e.type === 'NURSING') {
          const markerX = centerX + radius * Math.cos(angle);
          const markerY = centerY + radius * Math.sin(angle);
          const color = '#ec4899';

          // Outer white halo outline
          ctx.beginPath();
          ctx.arc(markerX, markerY, 6.5, 0, 2 * Math.PI);
          ctx.fillStyle = '#ffffff';
          ctx.fill();

          // Colored center
          ctx.beginPath();
          ctx.arc(markerX, markerY, 4.8, 0, 2 * Math.PI);
          ctx.fillStyle = color;
          ctx.fill();

          // Crisp dark border outline
          ctx.beginPath();
          ctx.arc(markerX, markerY, 6.5, 0, 2 * Math.PI);
          ctx.lineWidth = 1;
          ctx.strokeStyle = '#0f172a';
          ctx.stroke();
        } else if (e.type === 'DIAPER') {
          const outerR = radius + strokeWidth / 2 - 3;
          const markerX = centerX + outerR * Math.cos(angle);
          const markerY = centerY + outerR * Math.sin(angle);
          const color = e.diaperType === 'POO' ? '#f59e0b' : '#38bdf8';

          // Outer white halo outline
          ctx.beginPath();
          ctx.arc(markerX, markerY, 5.5, 0, 2 * Math.PI);
          ctx.fillStyle = '#ffffff';
          ctx.fill();

          // Colored center
          ctx.beginPath();
          ctx.arc(markerX, markerY, 3.8, 0, 2 * Math.PI);
          ctx.fillStyle = color;
          ctx.fill();

          // Crisp dark border outline
          ctx.beginPath();
          ctx.arc(markerX, markerY, 5.5, 0, 2 * Math.PI);
          ctx.lineWidth = 1;
          ctx.strokeStyle = '#0f172a';
          ctx.stroke();
        }
      }
    }

    // 5. Draw Current Time Needle (Red Needle)
    const simNow = new Date(getSimulatedNow());
    const nowMin = simNow.getHours() * 60 + simNow.getMinutes() + (simNow.getSeconds() / 60);
    const nowAngle = (nowMin / 1440) * 2 * Math.PI - Math.PI / 2;

    const needleInner = radius - strokeWidth / 2 - 4;
    const needleOuter = radius + strokeWidth / 2 + 5;
    const nx1 = centerX + needleInner * Math.cos(nowAngle);
    const ny1 = centerY + needleInner * Math.sin(nowAngle);
    const nx2 = centerX + needleOuter * Math.cos(nowAngle);
    const ny2 = centerY + needleOuter * Math.sin(nowAngle);

    ctx.beginPath();
    ctx.moveTo(nx1, ny1);
    ctx.lineTo(nx2, ny2);
    ctx.strokeStyle = '#ef4444';
    ctx.lineWidth = 3;
    ctx.lineCap = 'round';
    ctx.stroke();

    // 6. Update Center Overlay & Statuses
    updateDashboardUI();
  }

  // --- UI Update Function ---
  function updateDashboardUI() {
    const profile = getActiveProfile();
    const ageWeeks = calculateAgeInWeeks(profile.birthTimestamp);
    const simDate = new Date(getSimulatedNow());

    // Top Bar Chip
    document.getElementById('header-profile-name').textContent = `${profile.name} (${ageWeeks}w)`;

    // Status Time
    const hours = String(simDate.getHours()).padStart(2, '0');
    const mins = String(simDate.getMinutes()).padStart(2, '0');
    document.getElementById('phone-status-time').textContent = `${hours}:${mins}`;
    document.getElementById('sim-clock-display').textContent = `${hours}:${mins}`;

    // Date Label
    const selectedDate = new Date(state.selectedDayStart);
    const isToday = state.selectedDayStart === getStartOfDay(getSimulatedNow());
    const dateStr = selectedDate.toLocaleDateString('en-GB', { day: 'numeric', month: 'long' });
    document.getElementById('date-display-label').textContent = isToday ? `Today, ${dateStr}` : dateStr;

    // Wake Window & Feed States
    const wakeState = computeWakeWindowState(profile);
    const feedState = computeFeedingState(profile);

    const statusBadge = document.getElementById('chart-center-status');
    const timerElem = document.getElementById('chart-center-timer');
    const subElem = document.getElementById('chart-center-sub');
    const feedElem = document.getElementById('chart-center-feed');

    if (wakeState.isSleeping) {
      statusBadge.textContent = '😴 Sleeping';
      statusBadge.style.color = '#818cf8';
      timerElem.textContent = `${wakeState.durationMinutes} min`;
      subElem.textContent = 'Nap in progress';
    } else {
      statusBadge.textContent = '👶 Awake';
      statusBadge.style.color = '#10b981';
      timerElem.textContent = `${wakeState.durationMinutes} min`;
      subElem.textContent = wakeState.statusText;
      subElem.style.color = wakeState.remainingMinutes !== null && wakeState.remainingMinutes < 10 ? '#ef4444' : '#94a3b8';
    }

    feedElem.textContent = feedState.text;

    // Action button states
    const sleepBtn = document.getElementById('btn-action-sleep');
    const sleepTitle = document.getElementById('sleep-btn-title');
    if (wakeState.isSleeping) {
      sleepBtn.classList.add('active-sleeping');
      sleepTitle.textContent = 'Wake Up';
    } else {
      sleepBtn.classList.remove('active-sleeping');
      sleepTitle.textContent = 'Start Sleep';
    }

    const nursingBtn = document.getElementById('btn-action-nursing');
    const nursingTitle = document.getElementById('nursing-btn-title');
    if (feedState.isNursing) {
      nursingBtn.classList.add('active-nursing');
      nursingTitle.textContent = 'End Nursing';
    } else {
      nursingBtn.classList.remove('active-nursing');
      nursingTitle.textContent = 'Start Nursing';
    }

    // Quick Summary
    document.getElementById('summary-target-wake').textContent = `${wakeState.targetMinutes} min`;
    document.getElementById('summary-feed-interval').textContent = `${(profile.feedingIntervalMinutes / 60).toFixed(1)} hrs`;
    const dayEvents = state.events.filter(e => e.babyProfileId === profile.id && e.startTime >= state.selectedDayStart && e.startTime <= getEndOfDay(state.selectedDayStart));
    document.getElementById('summary-today-logs').textContent = `${dayEvents.length} items`;

    // Simulated alerts check
    if (profile.enablePushNotifications && wakeState.isAlert10m) {
      triggerNotification('Activity Cycle Ending Soon', `${profile.name}'s wake window ends in ${wakeState.remainingMinutes} minutes. Time to start the wind-down ritual!`);
    }
  }

  // --- Push Notification Display Helper ---
  let notifTimeout = null;
  function triggerNotification(title, message) {
    const banner = document.getElementById('push-notification-banner');
    document.getElementById('notif-title').textContent = title;
    document.getElementById('notif-body').textContent = message;
    banner.classList.remove('hidden');

    clearTimeout(notifTimeout);
    notifTimeout = setTimeout(() => {
      banner.classList.add('hidden');
    }, 6000);
  }

  document.getElementById('notif-dismiss').addEventListener('click', () => {
    document.getElementById('push-notification-banner').classList.add('hidden');
  });

  // --- Reviews View Rendering ---
  function updateReviewsView() {
    const profile = getActiveProfile();
    const profileEvents = state.events.filter(e => e.babyProfileId === profile.id);

    // Calculate totals for selected period
    let daysToInclude = 1;
    if (state.reviewPeriod === 'weekly') daysToInclude = 7;
    if (state.reviewPeriod === 'monthly') daysToInclude = 30;

    let totalSleepMin = 0;
    let daySleepMin = 0;
    let nightSleepMin = 0;
    let napsCount = 0;
    let feedsCount = 0;
    let feedsMin = 0;
    let peeCount = 0;
    let pooCount = 0;

    const dailyBars = [];
    const now = getSimulatedNow();

    for (let i = daysToInclude - 1; i >= 0; i--) {
      const dStart = getStartOfDay(now - (i * 24 * 60 * 60 * 1000));
      const dEnd = getEndOfDay(dStart);
      const dayEvs = profileEvents.filter(e => e.startTime >= dStart && e.startTime <= dEnd);

      let dayTotalSleep = 0;
      dayEvs.forEach(e => {
        if (e.type === 'SLEEP') {
          napsCount++;
          const dur = Math.floor(Math.max(0, (e.endTime || now) - e.startTime) / 60000);
          totalSleepMin += dur;
          dayTotalSleep += dur;
          const h = new Date(e.startTime).getHours();
          if (h >= 19 || h < 7) nightSleepMin += dur; else daySleepMin += dur;
        } else if (e.type === 'NURSING') {
          feedsCount++;
          feedsMin += Math.floor(Math.max(0, (e.endTime || e.startTime) - e.startTime) / 60000);
        } else if (e.type === 'DIAPER') {
          if (e.diaperType === 'PEE') peeCount++;
          else if (e.diaperType === 'POO') pooCount++;
          else { peeCount++; pooCount++; }
        }
      });

      const dayName = new Date(dStart).toLocaleDateString('en-GB', { weekday: 'narrow' });
      dailyBars.push({ day: dayName, hours: (dayTotalSleep / 60).toFixed(1) });
    }

    const n = daysToInclude;
    const avgSleep = Math.round(totalSleepMin / n);
    const avgSleepH = Math.floor(avgSleep / 60);
    const avgSleepM = avgSleep % 60;

    document.getElementById('metric-total-sleep').textContent = `${avgSleepH}h ${avgSleepM}m`;
    document.getElementById('metric-sleep-sub').textContent = `Day: ${Math.floor((daySleepMin / n) / 60)}h • Night: ${Math.floor((nightSleepMin / n) / 60)}h (${(napsCount / n).toFixed(1)} naps/day)`;
    document.getElementById('metric-avg-wake').textContent = `${profile.customWakeWindowMinutes || getRecommendedWakeWindow(calculateAgeInWeeks(profile.birthTimestamp))} min`;
    document.getElementById('metric-feedings-count').textContent = `${(feedsCount / n).toFixed(1)} sessions`;
    document.getElementById('metric-feedings-sub').textContent = `${Math.round(feedsMin / n)}m total nursing time/day`;
    document.getElementById('metric-diaper-count').textContent = `${((peeCount + pooCount) / n).toFixed(1)} changes`;
    document.getElementById('metric-diaper-sub').textContent = `💧 Wet: ${(peeCount / n).toFixed(1)}  |  💩 Dirty: ${(pooCount / n).toFixed(1)}`;

    // Render trend bars
    const barsContainer = document.getElementById('trend-bars-container');
    barsContainer.innerHTML = '';
    const maxH = Math.max(1, ...dailyBars.map(b => parseFloat(b.hours)));
    dailyBars.slice(-7).forEach(b => {
      const heightPercent = Math.max(10, (parseFloat(b.hours) / maxH) * 100);
      const col = document.createElement('div');
      col.className = 'trend-bar-col';
      col.innerHTML = `
        <span class="bar-val">${b.hours}h</span>
        <div class="bar-pillar" style="height: ${heightPercent}%;"></div>
        <span class="bar-day">${b.day}</span>
      `;
      barsContainer.appendChild(col);
    });
  }

  // --- Settings View Rendering ---
  function updateSettingsView() {
    const profile = getActiveProfile();
    document.getElementById('settings-active-name').textContent = profile.name;

    const bDate = new Date(profile.birthTimestamp);
    document.getElementById('input-profile-birthdate').value = bDate.toISOString().split('T')[0];
    document.getElementById('settings-age-weeks-label').textContent = `Baby's age: ${calculateAgeInWeeks(profile.birthTimestamp)} weeks`;

    // Profiles List
    const pContainer = document.getElementById('settings-profiles-list');
    pContainer.innerHTML = '';
    state.profiles.forEach(p => {
      const isActive = p.id === state.activeProfileId;
      const row = document.createElement('div');
      row.className = `profile-row-item ${isActive ? 'active' : ''}`;
      row.innerHTML = `
        <div>
          <strong>${p.name} ${isActive ? '✨ (Active)' : ''}</strong>
          <p style="font-size: 11px; color: var(--text-muted);">${calculateAgeInWeeks(p.birthTimestamp)} weeks old</p>
        </div>
        ${!isActive ? `<button class="ctrl-btn" style="width: auto; padding: 4px 8px;" data-switch-id="${p.id}">Switch</button>` : ''}
      `;
      row.addEventListener('click', (e) => {
        if (!e.target.dataset.switchId) {
          state.activeProfileId = p.id;
          saveState();
          updateDashboardUI();
          updateSettingsView();
        }
      });
      const btn = row.querySelector('[data-switch-id]');
      if (btn) {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          state.activeProfileId = p.id;
          saveState();
          updateDashboardUI();
          updateSettingsView();
        });
      }
      pContainer.appendChild(row);
    });

    // Wake chips
    const wakeChips = document.querySelectorAll('#wake-window-chips .choice-chip');
    wakeChips.forEach(chip => {
      const val = chip.dataset.val;
      if ((val === 'auto' && !profile.customWakeWindowMinutes) || (val === String(profile.customWakeWindowMinutes))) {
        chip.classList.add('active');
      } else {
        chip.classList.remove('active');
      }
    });

    // Feed chips
    const feedChips = document.querySelectorAll('#feed-interval-chips .choice-chip');
    feedChips.forEach(chip => {
      if (Number(chip.dataset.val) === profile.feedingIntervalMinutes) {
        chip.classList.add('active');
      } else {
        chip.classList.remove('active');
      }
    });

    // Switches
    document.getElementById('toggle-push-notifs').checked = profile.enablePushNotifications;
    document.getElementById('toggle-calendar-sync').checked = profile.enableCalendarSync;
  }

  // --- View Switching ---
  function showView(viewId) {
    state.activeView = viewId;
    document.querySelectorAll('.screen-view').forEach(v => v.classList.remove('active'));
    document.getElementById(`view-${viewId}`).classList.add('active');

    if (viewId === 'dashboard') renderClockChart();
    if (viewId === 'reviews') updateReviewsView();
    if (viewId === 'settings') updateSettingsView();
  }

  document.getElementById('btn-nav-reviews').addEventListener('click', () => showView('reviews'));
  document.getElementById('btn-nav-settings').addEventListener('click', () => showView('settings'));
  document.getElementById('btn-reviews-back').addEventListener('click', () => showView('dashboard'));
  document.getElementById('btn-settings-back').addEventListener('click', () => showView('dashboard'));

  // Segmented Tabs in Reviews
  document.querySelectorAll('.seg-tab').forEach(tab => {
    tab.addEventListener('click', (e) => {
      document.querySelectorAll('.seg-tab').forEach(t => t.classList.remove('active'));
      e.target.classList.add('active');
      state.reviewPeriod = e.target.id.replace('tab-review-', '');
      updateReviewsView();
    });
  });

  // --- Action Button Handlers (Tap = Immediate, Hold / Long-press = Edit Time) ---
  function setupHoldButton(btnId, onClick, onHold) {
    const btn = document.getElementById(btnId);
    if (!btn) return;

    let holdTimer = null;
    let didHold = false;
    const HOLD_DURATION = 420; // ms

    function startHold(e) {
      if (e.button !== undefined && e.button !== 0) return; // left click only
      didHold = false;
      clearTimeout(holdTimer);
      holdTimer = setTimeout(() => {
        didHold = true;
        btn.classList.add('btn-holding');
        setTimeout(() => btn.classList.remove('btn-holding'), 200);
        onHold();
      }, HOLD_DURATION);
    }

    function cancelHold() {
      clearTimeout(holdTimer);
    }

    btn.addEventListener('mousedown', startHold);
    btn.addEventListener('mouseup', cancelHold);
    btn.addEventListener('mouseleave', cancelHold);

    btn.addEventListener('touchstart', (e) => {
      startHold(e);
    }, { passive: true });
    btn.addEventListener('touchend', cancelHold);
    btn.addEventListener('touchcancel', cancelHold);

    // Right-click triggers hold instantly on PC
    btn.addEventListener('contextmenu', (e) => {
      e.preventDefault();
      clearTimeout(holdTimer);
      didHold = true;
      onHold();
    });

    btn.addEventListener('click', (e) => {
      if (didHold) {
        didHold = false;
        e.preventDefault();
        e.stopPropagation();
        return;
      }
      onClick();
    });
  }

  // 1. Sleep Action Button
  setupHoldButton(
    'btn-action-sleep',
    () => {
      const profile = getActiveProfile();
      const sleepEvents = state.events.filter(e => e.babyProfileId === profile.id && e.type === 'SLEEP');
      const ongoing = sleepEvents.find(e => !e.endTime);

      if (ongoing) {
        ongoing.endTime = getSimulatedNow();
      } else {
        state.events.push({
          id: Date.now(),
          babyProfileId: profile.id,
          type: 'SLEEP',
          startTime: getSimulatedNow(),
          endTime: null
        });
      }
      saveState();
      renderClockChart();
    },
    () => {
      const profile = getActiveProfile();
      const ongoing = state.events.find(ev => ev.babyProfileId === profile.id && ev.type === 'SLEEP' && !ev.endTime);
      const title = ongoing ? 'Adjust Wake Up Time' : 'Adjust Sleep Start Time';
      openTimeAdjust(title, (adjustedTime) => {
        if (ongoing) {
          ongoing.endTime = adjustedTime;
        } else {
          state.events.push({
            id: Date.now(),
            babyProfileId: profile.id,
            type: 'SLEEP',
            startTime: adjustedTime,
            endTime: null
          });
        }
        saveState();
        renderClockChart();
      });
    }
  );

  // 2. Nursing Action Button
  setupHoldButton(
    'btn-action-nursing',
    () => {
      const profile = getActiveProfile();
      const ongoing = state.events.find(ev => ev.babyProfileId === profile.id && ev.type === 'NURSING' && !ev.endTime);

      if (ongoing) {
        ongoing.endTime = getSimulatedNow();
        saveState();
        renderClockChart();
      } else {
        openNursingDialog((nursingType, amountMl) => {
          state.events.push({
            id: Date.now(),
            babyProfileId: profile.id,
            type: 'NURSING',
            startTime: getSimulatedNow(),
            endTime: null,
            nursingType,
            amountMl
          });
          saveState();
          renderClockChart();
        });
      }
    },
    () => {
      const profile = getActiveProfile();
      const ongoing = state.events.find(ev => ev.babyProfileId === profile.id && ev.type === 'NURSING' && !ev.endTime);
      const title = ongoing ? 'Adjust Nursing End Time' : 'Adjust Nursing Start Time';
      openTimeAdjust(title, (adjustedTime) => {
        if (ongoing) {
          ongoing.endTime = adjustedTime;
          saveState();
          renderClockChart();
        } else {
          openNursingDialog((nursingType, amountMl) => {
            state.events.push({
              id: Date.now(),
              babyProfileId: profile.id,
              type: 'NURSING',
              startTime: adjustedTime,
              endTime: null,
              nursingType,
              amountMl
            });
            saveState();
            renderClockChart();
          });
        }
      });
    }
  );

  // 3. Diaper Pee Button
  setupHoldButton(
    'btn-action-pee',
    () => {
      state.events.push({
        id: Date.now(),
        babyProfileId: getActiveProfile().id,
        type: 'DIAPER',
        startTime: getSimulatedNow(),
        diaperType: 'PEE'
      });
      saveState();
      renderClockChart();
    },
    () => {
      openTimeAdjust('Adjust Pee Time', (adjustedTime) => {
        state.events.push({
          id: Date.now(),
          babyProfileId: getActiveProfile().id,
          type: 'DIAPER',
          startTime: adjustedTime,
          diaperType: 'PEE'
        });
        saveState();
        renderClockChart();
      });
    }
  );

  // 4. Diaper Poo Button
  setupHoldButton(
    'btn-action-poo',
    () => {
      state.events.push({
        id: Date.now(),
        babyProfileId: getActiveProfile().id,
        type: 'DIAPER',
        startTime: getSimulatedNow(),
        diaperType: 'POO'
      });
      saveState();
      renderClockChart();
    },
    () => {
      openTimeAdjust('Adjust Poo Time', (adjustedTime) => {
        state.events.push({
          id: Date.now(),
          babyProfileId: getActiveProfile().id,
          type: 'DIAPER',
          startTime: adjustedTime,
          diaperType: 'POO'
        });
        saveState();
        renderClockChart();
      });
    }
  );

  // --- Modals Logic ---
  // 1. Time Adjust Modal
  let onTimeConfirmCallback = null;
  let selectedAdjustTime = null;

  function openTimeAdjust(title, onConfirm) {
    document.getElementById('time-adjust-title').textContent = title;
    selectedAdjustTime = getSimulatedNow();
    onTimeConfirmCallback = onConfirm;

    // Reset offset chips to "Now" (offset 0)
    document.querySelectorAll('.offset-chip').forEach(c => {
      if (c.dataset.offset === '0') {
        c.classList.add('active');
      } else {
        c.classList.remove('active');
      }
    });
    const customInput = document.getElementById('input-custom-time');
    if (customInput) customInput.value = '';

    const d = new Date(selectedAdjustTime);
    document.getElementById('time-adjust-current').textContent = `Selected: ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
    document.getElementById('modal-time-adjust').classList.remove('hidden');
  }

  document.querySelectorAll('.offset-chip').forEach(chip => {
    chip.addEventListener('click', (e) => {
      document.querySelectorAll('.offset-chip').forEach(c => c.classList.remove('active'));
      e.target.classList.add('active');
      const offsetMin = parseInt(e.target.dataset.offset);
      selectedAdjustTime = getSimulatedNow() + (offsetMin * 60 * 1000);
      const d = new Date(selectedAdjustTime);
      document.getElementById('time-adjust-current').textContent = `Selected: ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
    });
  });

  document.getElementById('btn-time-adjust-confirm').addEventListener('click', () => {
    const customVal = document.getElementById('input-custom-time').value;
    if (customVal) {
      const parts = customVal.split(':');
      const d = new Date(getSimulatedNow());
      d.setHours(parseInt(parts[0]), parseInt(parts[1]), 0, 0);
      selectedAdjustTime = d.getTime();
    }
    if (onTimeConfirmCallback) onTimeConfirmCallback(selectedAdjustTime);
    document.getElementById('modal-time-adjust').classList.add('hidden');
  });

  document.getElementById('btn-time-adjust-cancel').addEventListener('click', () => {
    document.getElementById('modal-time-adjust').classList.add('hidden');
  });

  // 2. Nursing Modal
  let onNursingConfirmCallback = null;
  let selectedNursingType = 'LEFT_BREAST';

  function openNursingDialog(onConfirm) {
    onNursingConfirmCallback = onConfirm;
    document.getElementById('modal-nursing').classList.remove('hidden');
  }

  document.querySelectorAll('.nursing-choice-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
      document.querySelectorAll('.nursing-choice-btn').forEach(b => b.classList.remove('active'));
      e.target.classList.add('active');
      selectedNursingType = e.target.dataset.type;

      if (selectedNursingType === 'BOTTLE') {
        document.getElementById('bottle-amount-group').classList.remove('hidden');
      } else {
        document.getElementById('bottle-amount-group').classList.add('hidden');
      }
    });
  });

  document.getElementById('btn-nursing-confirm').addEventListener('click', () => {
    const amount = selectedNursingType === 'BOTTLE' ? parseInt(document.getElementById('input-bottle-ml').value) : null;
    if (onNursingConfirmCallback) onNursingConfirmCallback(selectedNursingType, amount);
    document.getElementById('modal-nursing').classList.add('hidden');
  });

  document.getElementById('btn-nursing-cancel').addEventListener('click', () => {
    document.getElementById('modal-nursing').classList.add('hidden');
  });

  // 3. Profile Switcher Modal
  document.getElementById('btn-active-profile-chip').addEventListener('click', () => {
    openProfileSwitcher();
  });

  function openProfileSwitcher() {
    const container = document.getElementById('modal-profiles-container');
    container.innerHTML = '';
    document.getElementById('add-profile-form').classList.add('hidden');
    document.getElementById('btn-modal-add-profile').textContent = '+ Add New Child';

    state.profiles.forEach(p => {
      const isSelected = p.id === state.activeProfileId;
      const card = document.createElement('div');
      card.className = `profile-row-item ${isSelected ? 'active' : ''}`;
      card.innerHTML = `
        <div style="display:flex; align-items:center; gap:8px;">
          <span style="font-size: 20px;">👶</span>
          <div>
            <strong>${p.name}</strong>
            <p style="font-size: 11px; color: var(--text-muted);">${calculateAgeInWeeks(p.birthTimestamp)} weeks old</p>
          </div>
        </div>
        ${isSelected ? '<span style="color:var(--primary); font-weight:bold;">✓ Active</span>' : ''}
      `;
      card.addEventListener('click', () => {
        state.activeProfileId = p.id;
        saveState();
        renderClockChart();
        document.getElementById('modal-profile-switcher').classList.add('hidden');
      });
      container.appendChild(card);
    });

    document.getElementById('modal-profile-switcher').classList.remove('hidden');
  }

  document.getElementById('btn-modal-close-profile').addEventListener('click', () => {
    document.getElementById('modal-profile-switcher').classList.add('hidden');
  });

  document.getElementById('btn-modal-add-profile').addEventListener('click', () => {
    const form = document.getElementById('add-profile-form');
    if (form.classList.contains('hidden')) {
      form.classList.remove('hidden');
      document.getElementById('btn-modal-add-profile').textContent = 'Save Child';
    } else {
      const name = document.getElementById('input-new-profile-name').value.trim() || 'Baby';
      const birth = new Date(document.getElementById('input-new-profile-birth').value || Date.now()).getTime();
      const newId = Date.now();
      state.profiles.push({
        id: newId,
        name,
        birthTimestamp: birth,
        isActive: true,
        customWakeWindowMinutes: null,
        feedingIntervalMinutes: 150,
        selectedCalendarId: null,
        notifyBeforeMinutes: 10,
        enableCalendarSync: false,
        enablePushNotifications: true
      });
      state.activeProfileId = newId;
      saveState();
      renderClockChart();
      document.getElementById('modal-profile-switcher').classList.add('hidden');
    }
  });

  // --- Export & Import Actions ---
  // Export JSON Backup
  document.getElementById('btn-export-json').addEventListener('click', () => {
    const backupObj = {
      version: 1,
      exportTimestamp: getSimulatedNow(),
      profiles: state.profiles,
      events: state.events
    };
    downloadFile('ResidentSleeper_Backup.json', JSON.stringify(backupObj, null, 2), 'application/json');
  });

  // Export CSV Spreadsheet
  document.getElementById('btn-export-csv').addEventListener('click', () => {
    let csv = 'ProfileId,BabyName,EventType,StartTime,EndTime,DurationMinutes,NursingType,AmountMl,DiaperType,Notes\n';
    const profileMap = {};
    state.profiles.forEach(p => profileMap[p.id] = p.name);

    state.events.forEach(e => {
      const babyName = profileMap[e.babyProfileId] || 'Baby';
      const sDate = new Date(e.startTime).toISOString().replace('T', ' ').substring(0, 19);
      const eDate = e.endTime ? new Date(e.endTime).toISOString().replace('T', ' ').substring(0, 19) : '';
      const dur = e.endTime ? Math.floor((e.endTime - e.startTime) / 60000) : '';
      csv += `${e.babyProfileId},"${babyName}",${e.type},"${sDate}","${eDate}",${dur},${e.nursingType || ''},${e.amountMl || ''},${e.diaperType || ''},""\n`;
    });

    downloadFile('ResidentSleeper_Events.csv', csv, 'text/csv');
  });

  function downloadFile(filename, content, type) {
    const blob = new Blob([content], { type });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  // Import JSON File
  let pendingImportData = null;
  document.getElementById('input-import-json').addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        pendingImportData = JSON.parse(event.target.result);
        document.getElementById('modal-import-mode').classList.remove('hidden');
      } catch (err) {
        alert('Invalid JSON file format.');
      }
    };
    reader.readAsText(file);
  });

  document.getElementById('btn-import-confirm').addEventListener('click', () => {
    const mode = document.querySelector('input[name="import-mode"]:checked').value;
    if (pendingImportData) {
      if (mode === 'replace') {
        state.profiles = pendingImportData.profiles || [];
        state.events = pendingImportData.events || [];
      } else {
        // Merge
        const existingProfileIds = new Set(state.profiles.map(p => p.id));
        (pendingImportData.profiles || []).forEach(p => {
          if (!existingProfileIds.has(p.id)) state.profiles.push(p);
        });
        const existingEventIds = new Set(state.events.map(ev => ev.id));
        (pendingImportData.events || []).forEach(ev => {
          if (!existingEventIds.has(ev.id)) state.events.push(ev);
        });
      }
      saveState();
      renderClockChart();
      updateSettingsView();
      alert('Data imported successfully!');
    }
    document.getElementById('modal-import-mode').classList.add('hidden');
  });

  document.getElementById('btn-import-cancel').addEventListener('click', () => {
    document.getElementById('modal-import-mode').classList.add('hidden');
  });

  // --- Desktop Sidebar Utilities ---
  document.getElementById('btn-time-minus-30').addEventListener('click', () => {
    state.simTimeOffsetMs -= 30 * 60 * 1000;
    renderClockChart();
  });
  document.getElementById('btn-time-plus-15').addEventListener('click', () => {
    state.simTimeOffsetMs += 15 * 60 * 1000;
    renderClockChart();
  });
  document.getElementById('btn-time-plus-60').addEventListener('click', () => {
    state.simTimeOffsetMs += 60 * 60 * 1000;
    renderClockChart();
  });
  document.getElementById('btn-time-reset').addEventListener('click', () => {
    state.simTimeOffsetMs = 0;
    renderClockChart();
  });

  document.getElementById('btn-trigger-wake-alert').addEventListener('click', () => {
    triggerNotification('Activity Cycle Ending Soon', `${getActiveProfile().name}'s wake window ends in 10 minutes. Time to start the soothing ritual!`);
  });
  document.getElementById('btn-trigger-feed-alert').addEventListener('click', () => {
    triggerNotification('Feeding Approaching', `Approximated feeding time for ${getActiveProfile().name} in 10 minutes.`);
  });

  document.getElementById('btn-load-seed-data').addEventListener('click', () => {
    seedRealisticData();
    renderClockChart();
    alert('Loaded realistic 24-hour newborn routine data!');
  });

  document.getElementById('btn-clear-all').addEventListener('click', () => {
    if (confirm('Clear all logs and profiles?')) {
      state.events = [];
      saveState();
      renderClockChart();
    }
  });

  // Date Navigation
  document.getElementById('btn-date-prev').addEventListener('click', () => {
    state.selectedDayStart -= 24 * 60 * 60 * 1000;
    renderClockChart();
  });
  document.getElementById('btn-date-next').addEventListener('click', () => {
    const next = state.selectedDayStart + 24 * 60 * 60 * 1000;
    if (next <= getStartOfDay(getSimulatedNow())) {
      state.selectedDayStart = next;
      renderClockChart();
    }
  });

  // Live Timer Ticker every 5 seconds
  setInterval(() => {
    if (state.activeView === 'dashboard') {
      renderClockChart();
    }
  }, 5000);

  // Initialize
  loadState();
  renderClockChart();

})();
