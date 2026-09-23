// ResidentSleeper Interactive Web Simulator Engine
(function () {
  'use strict';

  // --- State & Storage ---
  const STORAGE_KEY = 'residentsleeper_sim_state';

  let state = {
    activeProfileId: 1,
    simTimeOffsetMs: 0,
    selectedDayStart: getStartOfDay(Date.now(), 7),
    activeView: 'dashboard', // 'dashboard', 'reviews', 'settings'
    reviewPeriod: 'daily',   // 'daily', 'weekly', 'monthly'
    selectedMetric: 'sleep', // 'sleep', 'wake', 'feed', 'diaper'
    profiles: [
      {
        id: 1,
        name: 'Emma',
        birthTimestamp: Date.now() - (28 * 24 * 60 * 60 * 1000), // 4 weeks old
        isActive: true,
        dayStartHour: 7,
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
        dayStartHour: 7,
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
          state.profiles = saved.profiles.map(p => ({
            ...p,
            dayStartHour: (p.dayStartHour !== undefined) ? p.dayStartHour : 7
          }));
          state.events = saved.events || [];
          state.activeProfileId = saved.activeProfileId || 1;
        }
      } catch (e) {
        console.error('Failed to parse saved state:', e);
      }
    }
    const prof = getActiveProfile();
    state.selectedDayStart = getStartOfDay(getSimulatedNow(), prof ? (prof.dayStartHour || 7) : 7);
    if (state.events.length < 20) {
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

  function getStartOfDay(timestamp, startHour = 7) {
    const d = new Date(timestamp);
    if (d.getHours() < startHour) {
      d.setDate(d.getDate() - 1);
    }
    d.setHours(startHour, 0, 0, 0);
    return d.getTime();
  }

  function getEndOfDay(timestamp, startHour = 7) {
    const s = getStartOfDay(timestamp, startHour);
    return s + (24 * 60 * 60 * 1000) - 1;
  }

  // --- Seed Realistic 30-Day Newborn Data ---
  function seedRealisticData() {
    const now = getSimulatedNow();
    const d = new Date(now);
    d.setHours(0, 0, 0, 0);
    const startOfCalendarDay = d.getTime();

    const evs = [];
    let idCounter = 1;

    for (let day = 29; day >= 0; day--) {
      const dayBase = startOfCalendarDay - (day * 24 * 60 * 60 * 1000);
      const randOffset = Math.sin(day * 1.7) * 20 * 60 * 1000;

      // Night sleep: 21:00 previous evening to 07:00 this morning
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'SLEEP',
        startTime: dayBase - (3 * 60 * 60 * 1000) + randOffset,
        endTime: dayBase + (7 * 60 * 60 * 1000) + randOffset
      });

      // 07:15 Diaper Pee
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'DIAPER',
        startTime: dayBase + (7 * 60 * 60 * 1000) + (15 * 60 * 1000),
        diaperType: 'PEE'
      });

      // 07:20 Nursing
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'NURSING',
        startTime: dayBase + (7 * 60 * 60 * 1000) + (20 * 60 * 1000),
        endTime: dayBase + (7 * 60 * 60 * 1000) + (45 * 60 * 1000),
        nursingType: 'LEFT_BREAST'
      });

      // Nap 1: 08:30 to 10:00 (1.5h)
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'SLEEP',
        startTime: dayBase + (8 * 60 * 60 * 1000) + (30 * 60 * 1000) + randOffset,
        endTime: dayBase + (10 * 60 * 60 * 1000) + randOffset
      });

      // 10:05 Diaper Poo
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'DIAPER',
        startTime: dayBase + (10 * 60 * 60 * 1000) + (5 * 60 * 1000),
        diaperType: 'POO'
      });

      // 10:10 Feeding
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'NURSING',
        startTime: dayBase + (10 * 60 * 60 * 1000) + (10 * 60 * 1000),
        endTime: dayBase + (10 * 60 * 60 * 1000) + (35 * 60 * 1000),
        nursingType: 'RIGHT_BREAST'
      });

      // Nap 2: 11:30 to 13:00 (1.5h)
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'SLEEP',
        startTime: dayBase + (11 * 60 * 60 * 1000) + (30 * 60 * 1000),
        endTime: dayBase + (13 * 60 * 60 * 1000)
      });

      // 13:05 Diaper Pee
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'DIAPER',
        startTime: dayBase + (13 * 60 * 60 * 1000) + (5 * 60 * 1000),
        diaperType: 'PEE'
      });

      // 13:10 Bottle
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'NURSING',
        startTime: dayBase + (13 * 60 * 60 * 1000) + (10 * 60 * 1000),
        endTime: dayBase + (13 * 60 * 60 * 1000) + (30 * 60 * 1000),
        nursingType: 'BOTTLE',
        amountMl: 90
      });

      // Nap 3: 14:45 to 16:00
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'SLEEP',
        startTime: dayBase + (14 * 60 * 60 * 1000) + (45 * 60 * 1000),
        endTime: dayBase + (16 * 60 * 60 * 1000)
      });

      // 16:15 Diapers
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'DIAPER',
        startTime: dayBase + (16 * 60 * 60 * 1000) + (15 * 60 * 1000),
        diaperType: 'PEE'
      });
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'DIAPER',
        startTime: dayBase + (16 * 60 * 60 * 1000) + (16 * 60 * 1000),
        diaperType: 'POO'
      });

      // 16:20 Nursing
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'NURSING',
        startTime: dayBase + (16 * 60 * 60 * 1000) + (20 * 60 * 1000),
        endTime: dayBase + (16 * 60 * 60 * 1000) + (45 * 60 * 1000),
        nursingType: 'LEFT_BREAST'
      });

      // Nap 4: 17:45 to 18:30 (catnap)
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'SLEEP',
        startTime: dayBase + (17 * 60 * 60 * 1000) + (45 * 60 * 1000),
        endTime: dayBase + (18 * 60 * 60 * 1000) + (30 * 60 * 1000)
      });

      // 19:00 Diaper & Feed
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'DIAPER',
        startTime: dayBase + (19 * 60 * 60 * 1000),
        diaperType: 'PEE'
      });
      evs.push({
        id: idCounter++,
        babyProfileId: 1,
        type: 'NURSING',
        startTime: dayBase + (19 * 60 * 60 * 1000) + (10 * 60 * 1000),
        endTime: dayBase + (19 * 60 * 60 * 1000) + (35 * 60 * 1000),
        nursingType: 'RIGHT_BREAST'
      });
    }

    state.events = evs;
    saveState();
  }

  // --- Calculations: Wake Window & Feeding Predictions ---
  function calculateAgeInWeeks(birthTimestamp) {
    const diff = Math.max(0, getSimulatedNow() - birthTimestamp);
    return Math.floor(diff / (7 * 24 * 60 * 60 * 1000));
  }

  const LITTLE_ONES_SCHEDULES = [
    {
      label: "0–4 Weeks (Newborn)",
      minW: 0, maxW: 4,
      targetSleepH: 16.5, targetDaySleepH: 7.0, targetNightH: 9.5,
      targetNaps: 5,
      morningWW: 50, middayWW: 60, afternoonWW: 60, bedtimeWW: 75,
      morningNapM: 60, middayNapM: 120, catnapM: 45,
      bedtimeH: 19, bedtimeM: 0,
      trivia: [
        "Newborns do not yet have a circadian rhythm. Daylight during awake times helps calibrate their developing body clock.",
        "At 1 month old, wake windows include feeding, changing, interaction, and soothing. Sticking close to 50–60m prevents overtiredness.",
        "Limiting individual daytime naps to 2–2.5 hours helps protect important nighttime sleep stretches."
      ]
    },
    {
      label: "5–8 Weeks (~2 Months)",
      minW: 5, maxW: 8,
      targetSleepH: 15.5, targetDaySleepH: 4.5, targetNightH: 11.0,
      targetNaps: 4,
      morningWW: 65, middayWW: 80, afternoonWW: 80, bedtimeWW: 90,
      morningNapM: 45, middayNapM: 120, catnapM: 35,
      bedtimeH: 18, bedtimeM: 45,
      trivia: [
        "Between 6 and 8 weeks, infant circadian rhythm begins to emerge. Anchoring a 7:00 AM morning wake time helps establish consistent days.",
        "At 2 months, the first stretch of night sleep naturally begins to lengthen, especially with a solid 2-hour midday nap in place.",
        "Wake windows that are too long cause cortisol spikes. If baby is fussy or resisting sleep, tighten the wake window by 15 minutes."
      ]
    },
    {
      label: "9–12 Weeks (~3 Months)",
      minW: 9, maxW: 12,
      targetSleepH: 15.5, targetDaySleepH: 3.5, targetNightH: 12.0,
      targetNaps: 3,
      morningWW: 80, middayWW: 100, afternoonWW: 105, bedtimeWW: 120,
      morningNapM: 40, middayNapM: 120, catnapM: 35,
      bedtimeH: 18, bedtimeM: 30,
      trivia: [
        "By 12 weeks, babies consolidate sleep into a short morning nap, a long 2-hour lunchtime sleep, and a short afternoon catnap.",
        "Consistency in your 3-month daytime structure lays the foundation for navigating the upcoming 4-month sleep regression smoothly.",
        "A calming 20-minute wind-down routine in dim lighting signals melatonin release, helping your baby settle without overtiredness."
      ]
    },
    {
      label: "13–16 Weeks (~4 Months)",
      minW: 13, maxW: 16,
      targetSleepH: 14.5, targetDaySleepH: 3.5, targetNightH: 11.0,
      targetNaps: 3,
      morningWW: 105, middayWW: 120, afternoonWW: 120, bedtimeWW: 135,
      morningNapM: 40, middayNapM: 120, catnapM: 30,
      bedtimeH: 18, bedtimeM: 30,
      trivia: [
        "The 4-Month Regression is a permanent biological milestone: infant sleep cycles mature into 4 adult-like stages every 45–50 minutes.",
        "Because babies surface between sleep cycles around 4 months, practicing putting baby down drowsy but awake builds crucial independent settling skills.",
        "Maintaining an age-appropriate 2-hour wake window prevents overtiredness, which is the #1 cause of frequent night wakes."
      ]
    },
    {
      label: "17–21 Weeks (~5 Months)",
      minW: 17, maxW: 21,
      targetSleepH: 14.5, targetDaySleepH: 3.25, targetNightH: 11.25,
      targetNaps: 3,
      morningWW: 120, middayWW: 135, afternoonWW: 135, bedtimeWW: 145,
      morningNapM: 45, middayNapM: 120, catnapM: 25,
      bedtimeH: 18, bedtimeM: 30,
      trivia: [
        "Five months often brings real predictability: a 45m morning nap, a 2h restorative lunchtime sleep, and a short 20–30m bridge catnap.",
        "Ensure the late afternoon catnap ends no later than 5:00 PM to protect sleep pressure for a 7:00 PM bedtime.",
        "Self-settling skills practiced during daytime naps carry over directly to reducing midnight wakeups."
      ]
    },
    {
      label: "22–25 Weeks (~6 Months)",
      minW: 22, maxW: 25,
      targetSleepH: 14.25, targetDaySleepH: 2.75, targetNightH: 11.5,
      targetNaps: 3,
      morningWW: 135, middayWW: 150, afternoonWW: 165, bedtimeWW: 180,
      morningNapM: 35, middayNapM: 110, catnapM: 15,
      bedtimeH: 18, bedtimeM: 30,
      trivia: [
        "At 6 months, the 3-to-2 nap transition begins. The late afternoon nap shrinks to a 10–15 minute bridge catnap.",
        "If your 6-month-old skips or refuses the 3rd nap, pull bedtime forward to 6:30 PM to avoid an overtired bedtime meltdown.",
        "Introducing solid foods and learning to roll can temporarily cause nighttime restlessness; keep the bedtime routine steady."
      ]
    },
    {
      label: "26–30 Weeks (~7 Months)",
      minW: 26, maxW: 30,
      targetSleepH: 14.0, targetDaySleepH: 2.5, targetNightH: 11.5,
      targetNaps: 2,
      morningWW: 140, middayWW: 165, afternoonWW: 180, bedtimeWW: 195,
      morningNapM: 35, middayNapM: 110, catnapM: 15,
      bedtimeH: 18, bedtimeM: 30,
      trivia: [
        "Fighting the 3rd nap is the primary sign baby is ready for a 2-nap routine (morning nap ~9:30 AM, lunchtime nap ~12:30 PM).",
        "Wake windows naturally lengthen to 2.5–3 hours, with the shortest window in the morning and longest before bed.",
        "If morning wake is early (before 6:00 AM), check if bedtime was too late; overtiredness causes early morning waking."
      ]
    },
    {
      label: "31–34 Weeks (~8 Months)",
      minW: 31, maxW: 34,
      targetSleepH: 14.0, targetDaySleepH: 2.5, targetNightH: 11.5,
      targetNaps: 2,
      morningWW: 150, middayWW: 180, afternoonWW: 195, bedtimeWW: 210,
      morningNapM: 30, middayNapM: 110, catnapM: 0,
      bedtimeH: 18, bedtimeM: 30,
      trivia: [
        "The 8-Month Sleep Regression is driven by major physical leaps: crawling, pulling to stand, and emerging separation anxiety.",
        "At 8 months, 2 naps totaling ~2.5 hours of day sleep provides the sweet spot for 11–12 hours of consolidated night sleep.",
        "Keep bedtime between 6:30 and 7:00 PM. A consistent sequence of bath, feeding, and lullaby anchors baby through separation anxiety."
      ]
    },
    {
      label: "35–38 Weeks (~9 Months)",
      minW: 35, maxW: 38,
      targetSleepH: 13.75, targetDaySleepH: 2.25, targetNightH: 11.5,
      targetNaps: 2,
      morningWW: 155, middayWW: 180, afternoonWW: 200, bedtimeWW: 210,
      morningNapM: 30, middayNapM: 105, catnapM: 0,
      bedtimeH: 18, bedtimeM: 45,
      trivia: [
        "At 9 months, overtiredness often disguises itself as hyperactivity, standing in the cot, or loud babbling rather than yawning!",
        "Most 9-month-olds thrive on: 7:00 AM wake, 9:30 AM nap 1 (30m), 12:30 PM nap 2 (1.5–2h), 6:45 PM bedtime.",
        "Avoid letting the afternoon nap run past 2:30–2:45 PM so that sleep pressure rebuilds adequately for bedtime."
      ]
    },
    {
      label: "39–43 Weeks (~10 Months)",
      minW: 39, maxW: 43,
      targetSleepH: 13.75, targetDaySleepH: 2.25, targetNightH: 11.5,
      targetNaps: 2,
      morningWW: 165, middayWW: 195, afternoonWW: 210, bedtimeWW: 225,
      morningNapM: 30, middayNapM: 95, catnapM: 0,
      bedtimeH: 18, bedtimeM: 45,
      trivia: [
        "At 10 months, if your baby resists nap 1, cap it to 20–30 minutes to preserve their sleep pressure for the longer midday nap.",
        "Separation awareness peaks at 10 months. Spending 5 minutes playing peek-a-boo in the nursery builds confidence before naptime.",
        "Do not drop to 1 nap yet! 10-month-olds who drop to 1 nap quickly accumulate severe sleep debt resulting in split nights."
      ]
    },
    {
      label: "44–47 Weeks (~11 Months)",
      minW: 44, maxW: 47,
      targetSleepH: 13.5, targetDaySleepH: 2.0, targetNightH: 11.5,
      targetNaps: 2,
      morningWW: 180, middayWW: 205, afternoonWW: 215, bedtimeWW: 240,
      morningNapM: 30, middayNapM: 90, catnapM: 0,
      bedtimeH: 18, bedtimeM: 45,
      trivia: [
        "Beware the 11-Month False 1-Nap Trap! Babies often test boundaries by resisting nap 2, but genuine 1-nap readiness rarely occurs before 14–15 months.",
        "Wake windows now comfortably reach 3.5 to 4 hours before bedtime.",
        "Balancing daytime sleep to around 2 hours total is key to preventing 5:00 AM early morning rising."
      ]
    },
    {
      label: "48+ Weeks (12+ Months)",
      minW: 48, maxW: 150,
      targetSleepH: 13.25, targetDaySleepH: 1.75, targetNightH: 11.5,
      targetNaps: 1,
      morningWW: 210, middayWW: 240, afternoonWW: 250, bedtimeWW: 270,
      morningNapM: 30, middayNapM: 90, catnapM: 0,
      bedtimeH: 19, bedtimeM: 0,
      trivia: [
        "The 2-to-1 nap transition happens between 12 and 18 months, most commonly around 14–15 months when baby handles a 5-hour morning window.",
        "When transitioning to 1 nap, schedule the single nap in the middle of the day (~12:00–12:30 PM) for 2 to 2.5 hours.",
        "During nap transitions, bring bedtime 30–45 minutes earlier to avoid overtiredness until the new schedule consolidates."
      ]
    }
  ];

  function getLittleOnesSchedule(ageWeeks) {
    const safeW = Math.max(0, ageWeeks || 0);
    return LITTLE_ONES_SCHEDULES.find(s => safeW >= s.minW && safeW <= s.maxW) || LITTLE_ONES_SCHEDULES[LITTLE_ONES_SCHEDULES.length - 1];
  }

  function getRecommendedWakeWindow(ageWeeks) {
    const s = getLittleOnesSchedule(ageWeeks);
    return s.middayWW;
  }

  function computeWakeWindowState(profile) {
    const now = getSimulatedNow();
    const ageWeeks = calculateAgeInWeeks(profile.birthTimestamp);
    const schedule = getLittleOnesSchedule(ageWeeks);

    const profileEvents = state.events.filter(e => e.babyProfileId === profile.id);
    const sleepEvents = profileEvents.filter(e => e.type === 'SLEEP').sort((a, b) => b.startTime - a.startTime);
    const ongoingSleep = sleepEvents.find(e => !e.endTime);

    // Completed naps today
    const dayStart = state.selectedDayStart;
    const dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1;
    const completedSleepsToday = profileEvents.filter(e => e.type === 'SLEEP' && e.endTime && e.startTime >= dayStart && e.startTime <= dayEnd);
    const napsCount = completedSleepsToday.length;
    const daySleepMinutes = Math.round(completedSleepsToday.reduce((acc, ev) => acc + Math.max(0, (ev.endTime - ev.startTime)), 0) / 60000);

    const simDate = new Date(now);
    const currentMinOfDay = simDate.getHours() * 60 + simDate.getMinutes();
    const bedtimeStartMin = schedule.bedtimeH * 60 + schedule.bedtimeM;

    // Determine category
    let nextCategory = 'midday';
    let recTitle = 'Next: Restorative Midday Nap';
    let recDuration = `${Math.floor(schedule.middayNapM / 60)}h ${schedule.middayNapM % 60 ? (schedule.middayNapM % 60) + 'm' : ''} (Restorative Nap)`;
    let baseWindow = schedule.middayWW;

    if (napsCount >= schedule.targetNaps && currentMinOfDay >= 16 * 60) {
      nextCategory = 'bedtime';
      recTitle = 'Next: Bedtime Ritual';
      recDuration = `${Math.round(schedule.targetNightH)} hours (Consolidated Night Sleep)`;
      baseWindow = schedule.bedtimeWW;
    } else if (currentMinOfDay >= bedtimeStartMin - 60) {
      nextCategory = 'bedtime';
      recTitle = 'Next: Bedtime Ritual';
      recDuration = `${Math.round(schedule.targetNightH)} hours (Consolidated Night Sleep)`;
      baseWindow = schedule.bedtimeWW;
    } else if (napsCount === 0) {
      nextCategory = 'morning';
      recTitle = 'Next: Morning Nap';
      recDuration = `${schedule.morningNapM} min (Morning Nap)`;
      baseWindow = schedule.morningWW;
    } else if (napsCount === 1) {
      if (schedule.targetNaps === 1) {
        nextCategory = 'bedtime';
        recTitle = 'Next: Bedtime Ritual';
        recDuration = `${Math.round(schedule.targetNightH)} hours (Consolidated Night Sleep)`;
        baseWindow = schedule.bedtimeWW;
      } else {
        nextCategory = 'midday';
        recTitle = 'Next: Restorative Midday Nap';
        recDuration = `${Math.floor(schedule.middayNapM / 60)}h ${schedule.middayNapM % 60 ? (schedule.middayNapM % 60) + 'm' : ''} (Restorative Nap)`;
        baseWindow = schedule.middayWW;
      }
    } else if (napsCount === 2) {
      if (schedule.targetNaps >= 3) {
        nextCategory = 'catnap';
        recTitle = 'Next: Bridge Catnap';
        recDuration = `${schedule.catnapM} min (Bridge Catnap — end before 5:00 PM)`;
        baseWindow = schedule.afternoonWW;
      } else {
        nextCategory = 'bedtime';
        recTitle = 'Next: Bedtime Ritual';
        recDuration = `${Math.round(schedule.targetNightH)} hours (Consolidated Night Sleep)`;
        baseWindow = schedule.bedtimeWW;
      }
    } else {
      if (currentMinOfDay >= 16 * 60 + 30) {
        nextCategory = 'bedtime';
        recTitle = 'Next: Bedtime Ritual';
        recDuration = `${Math.round(schedule.targetNightH)} hours (Consolidated Night Sleep)`;
        baseWindow = schedule.bedtimeWW;
      } else {
        nextCategory = 'catnap';
        recTitle = 'Next: Bridge Catnap';
        recDuration = `${schedule.catnapM} min (Bridge Catnap — end before 5:00 PM)`;
        baseWindow = schedule.afternoonWW;
      }
    }

    if (profile.customWakeWindowMinutes) {
      baseWindow = profile.customWakeWindowMinutes;
    }

    // Dynamic adjustment based on last nap
    const latestSleep = sleepEvents[0];
    const lastSleepDurationMin = latestSleep && latestSleep.endTime ? Math.round((latestSleep.endTime - latestSleep.startTime) / 60000) : 0;
    let targetMinutes = baseWindow;
    let recReason = '';

    if (!profile.customWakeWindowMinutes) {
      if (nextCategory === 'bedtime') {
        recReason = `Bedtime window approaching (${String(schedule.bedtimeH).padStart(2,'0')}:${String(schedule.bedtimeM).padStart(2,'0')}–19:30). Longer wake window builds overnight sleep pressure.`;
      } else if (lastSleepDurationMin >= 1 && lastSleepDurationMin < 40) {
        const reduction = Math.round(baseWindow * 0.18);
        targetMinutes = Math.max(35, baseWindow - reduction);
        recReason = `Last nap was short (${lastSleepDurationMin}m). Wake window shortened by ${reduction}m to prevent overtiredness.`;
      } else if (lastSleepDurationMin >= 90) {
        recReason = `Last nap was restorative (${lastSleepDurationMin}m). Full age-appropriate wake window supported.`;
      } else {
        recReason = nextCategory === 'morning'
          ? "Morning wake window is naturally shorter as circadian alertness ramps up."
          : (nextCategory === 'midday' ? "Midday sleep window builds pressure for the core restorative nap." : "Bridge catnap to prevent overtiredness before evening bedtime.");
      }
    } else {
      recReason = `Using custom wake window setting (${profile.customWakeWindowMinutes}m).`;
    }

    const triviaTip = schedule.trivia[0] || "Consistency in daytime rhythms protects restorative nighttime sleep.";

    if (ongoingSleep) {
      const durMin = Math.floor(Math.max(0, now - ongoingSleep.startTime) / 60000);
      return {
        isSleeping: true,
        durationMinutes: durMin,
        targetMinutes,
        remainingMinutes: null,
        statusText: `Sleeping (${durMin}m)`,
        isAlert10m: false,
        nextCategory,
        recTitle: "Sleep In Progress",
        recDuration,
        recReason: `Target duration: ${recDuration}`,
        triviaTip,
        napsCount,
        targetNaps: schedule.targetNaps,
        daySleepMinutes
      };
    }

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
      isAlert10m: diffMinutes <= 10 && diffMinutes >= 0,
      nextCategory,
      recTitle,
      recDuration,
      recReason,
      triviaTip,
      napsCount,
      targetNaps: schedule.targetNaps,
      daySleepMinutes
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
    const startHour = (profile && profile.dayStartHour !== undefined) ? profile.dayStartHour : 7;
    const profileEvents = state.events.filter(e => e.babyProfileId === profile.id);

    const dayStart = state.selectedDayStart;
    const dayEnd = dayStart + (24 * 60 * 60 * 1000) - 1;

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

    // 2. Draw Hour Ticks & Labels (startHour at top, +6 at right, +12 at bottom, +18 at left)
    for (let step = 0; step < 24; step++) {
      const h = (startHour + step) % 24;
      const angle = (step / 24) * 2 * Math.PI - Math.PI / 2;
      const isMajor = step % 3 === 0;
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

      if (step % 6 === 0) {
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

      const startOffsetMin = (clampedStart - dayStart) / 60000;
      const endOffsetMin = (clampedEnd - dayStart) / 60000;

      const startAngle = (startOffsetMin / 1440) * 2 * Math.PI - Math.PI / 2;
      const endAngle = (endOffsetMin / 1440) * 2 * Math.PI - Math.PI / 2;

      ctx.lineWidth = strokeWidth;
      ctx.lineCap = 'butt';

      const p1X = centerX + radius * Math.cos(startAngle);
      const p1Y = centerY + radius * Math.sin(startAngle);
      const p2X = centerX + radius * Math.cos(endAngle);
      const p2Y = centerY + radius * Math.sin(endAngle);

      const grad = ctx.createLinearGradient(p1X, p1Y, p2X, p2Y);
      const sleepGrad = isSleep || color === '#4f46e5';
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
      drawTimeArc(s.startTime, s.endTime || getSimulatedNow(), '#4f46e5', true);
    }

    // 4. Draw Markers (Nursing & Diapers)
    for (const e of profileEvents) {
      if (e.startTime >= dayStart && e.startTime <= dayEnd) {
        const offsetMin = (e.startTime - dayStart) / 60000;
        const angle = (offsetMin / 1440) * 2 * Math.PI - Math.PI / 2;

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
    const simNow = getSimulatedNow();
    if (simNow >= dayStart && simNow <= dayEnd) {
      const nowOffsetMin = (simNow - dayStart) / 60000;
      const nowAngle = (nowOffsetMin / 1440) * 2 * Math.PI - Math.PI / 2;

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
    }

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
    const isToday = state.selectedDayStart === getStartOfDay(getSimulatedNow(), profile.dayStartHour || 7);
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
      subElem.textContent = `${wakeState.statusText} • ${wakeState.recTitle.replace('Next: ', '')}`;
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

    // Populate Little Ones Sleep Recommendation Card
    const recCard = document.getElementById('recommendation-card');
    if (recCard) {
      document.getElementById('rec-title').textContent = wakeState.recTitle;
      const badge = document.getElementById('rec-nap-badge');
      if (wakeState.nextCategory === 'bedtime') {
        badge.textContent = 'Bedtime';
      } else if (wakeState.isSleeping) {
        badge.textContent = 'Sleeping';
      } else {
        badge.textContent = `Nap ${wakeState.napsCount + 1}/${wakeState.targetNaps}`;
      }
      document.getElementById('rec-target-duration').textContent = `Target: ${wakeState.recDuration}`;
      document.getElementById('rec-reason').textContent = wakeState.recReason;
    }

    // Populate Little Ones Pediatric Insight / Trivia Card
    const triviaCard = document.getElementById('trivia-card');
    if (triviaCard) {
      const sched = getLittleOnesSchedule(ageWeeks);
      document.getElementById('trivia-title').textContent = `${sched.label} Pediatric Insight`;
      document.getElementById('trivia-text').textContent = wakeState.triviaTip;
    }

    // Simulated alerts check
    if (profile.enablePushNotifications && wakeState.isAlert10m) {
      triggerNotification(
        `Wake Window Ending: ${wakeState.recTitle}`,
        `${profile.name}'s wake window ends in ${wakeState.remainingMinutes} min (${wakeState.recReason}).\n\n💡 Little Ones Tip: ${wakeState.triviaTip}`
      );
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

    const dailyData = [];
    const now = getSimulatedNow();

    for (let i = daysToInclude - 1; i >= 0; i--) {
      const dStart = getStartOfDay(now - (i * 24 * 60 * 60 * 1000), profile.dayStartHour || 7);
      const dEnd = dStart + (24 * 60 * 60 * 1000) - 1;
      const dayEvs = profileEvents.filter(e => e.startTime >= dStart && e.startTime <= dEnd);

      let dayTotalSleep = 0;
      let dayNaps = 0;
      let dayFeeds = 0;
      let dayPee = 0;
      let dayPoo = 0;

      dayEvs.forEach(e => {
        if (e.type === 'SLEEP') {
          napsCount++;
          dayNaps++;
          const dur = Math.floor(Math.max(0, (e.endTime || now) - e.startTime) / 60000);
          totalSleepMin += dur;
          dayTotalSleep += dur;
          const h = new Date(e.startTime).getHours();
          if (h >= 19 || h < 7) nightSleepMin += dur; else daySleepMin += dur;
        } else if (e.type === 'NURSING') {
          feedsCount++;
          dayFeeds++;
          feedsMin += Math.floor(Math.max(0, (e.endTime || e.startTime) - e.startTime) / 60000);
        } else if (e.type === 'DIAPER') {
          if (e.diaperType === 'PEE') { peeCount++; dayPee++; }
          else if (e.diaperType === 'POO') { pooCount++; dayPoo++; }
          else { peeCount++; dayPee++; pooCount++; dayPoo++; }
        }
      });

      const dayName = new Date(dStart).toLocaleDateString('en-GB', { weekday: 'narrow' });
      const dateLabel = new Date(dStart).toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });
      const sleepHours = dayTotalSleep / 60;
      const wakeWindow = (dayNaps > 0)
        ? Math.round(Math.max(30, (1440 - dayTotalSleep) / (dayNaps + 1)))
        : (profile.customWakeWindowMinutes || 60);

      dailyData.push({
        dateEpoch: dStart,
        dayName,
        dateLabel,
        sleep: sleepHours,
        wake: wakeWindow,
        feed: dayFeeds,
        diaper: dayPee + dayPoo,
        hasSleep: dayTotalSleep > 0,
        hasFeed: dayFeeds > 0,
        hasDiaper: (dayPee + dayPoo) > 0,
        hasAnyData: (dayTotalSleep > 0 || dayFeeds > 0 || (dayPee + dayPoo) > 0)
      });
    }

    // Exclude missing days from averages (per requirement: missing data from past days shouldn't be counted towards average)
    const daysWithSleep = dailyData.filter(d => d.hasSleep);
    const nSleep = daysWithSleep.length > 0 ? daysWithSleep.length : 1;

    const daysWithFeeds = dailyData.filter(d => d.hasFeed);
    const nFeeds = daysWithFeeds.length > 0 ? daysWithFeeds.length : 1;

    const daysWithDiapers = dailyData.filter(d => d.hasDiaper);
    const nDiapers = daysWithDiapers.length > 0 ? daysWithDiapers.length : 1;

    const avgSleep = Math.round(totalSleepMin / nSleep);
    const avgSleepH = Math.floor(avgSleep / 60);
    const avgSleepM = avgSleep % 60;

    document.getElementById('metric-total-sleep').textContent = `${avgSleepH}h ${avgSleepM}m`;
    document.getElementById('metric-sleep-sub').textContent = `Day: ${Math.floor((daySleepMin / nSleep) / 60)}h • Night: ${Math.floor((nightSleepMin / nSleep) / 60)}h (${(napsCount / nSleep).toFixed(1)} naps/day)`;
    document.getElementById('metric-avg-wake').textContent = `${profile.customWakeWindowMinutes || getRecommendedWakeWindow(calculateAgeInWeeks(profile.birthTimestamp))} min`;
    document.getElementById('metric-feedings-count').textContent = `${(feedsCount / nFeeds).toFixed(1)} sessions`;
    document.getElementById('metric-feedings-sub').textContent = `${Math.round(feedsMin / nFeeds)}m total nursing time/day`;
    document.getElementById('metric-diaper-count').textContent = `${((peeCount + pooCount) / nDiapers).toFixed(1)} changes`;
    document.getElementById('metric-diaper-sub').textContent = `💧 Wet: ${(peeCount / nDiapers).toFixed(1)}  |  💩 Dirty: ${(pooCount / nDiapers).toFixed(1)}`;

    // Highlight selected metric card
    const metric = state.selectedMetric || 'sleep';
    const cardMap = {
      sleep: document.getElementById('metric-card-sleep'),
      wake: document.getElementById('metric-card-wake'),
      feed: document.getElementById('metric-card-feed'),
      diaper: document.getElementById('metric-card-diaper')
    };

    Object.keys(cardMap).forEach(key => {
      const card = cardMap[key];
      if (card) {
        card.classList.remove('active', 'sleep', 'wake', 'feed', 'diaper');
        if (key === metric) {
          card.classList.add('active', key);
        }
      }
    });

    // Metric configuration
    const metricConfig = {
      sleep: {
        title: 'Sleep Trend (Hours / Day)',
        badge: 'Sleep',
        color: '#6366f1',
        grad0: 'rgba(99, 102, 241, 0.45)',
        grad1: 'rgba(99, 102, 241, 0.02)',
        unit: 'h',
        getValue: d => d.sleep,
        formatVal: v => v.toFixed(1) + 'h'
      },
      wake: {
        title: 'Wake Window Trend (Minutes / Day)',
        badge: 'Wake Window',
        color: '#10b981',
        grad0: 'rgba(16, 185, 129, 0.45)',
        grad1: 'rgba(16, 185, 129, 0.02)',
        unit: 'm',
        getValue: d => d.wake,
        formatVal: v => Math.round(v) + 'm'
      },
      feed: {
        title: 'Feedings Trend (Sessions / Day)',
        badge: 'Feedings',
        color: '#ec4899',
        grad0: 'rgba(236, 72, 153, 0.45)',
        grad1: 'rgba(236, 72, 153, 0.02)',
        unit: '',
        getValue: d => d.feed,
        formatVal: v => Math.round(v)
      },
      diaper: {
        title: 'Diapers Trend (Changes / Day)',
        badge: 'Diapers',
        color: '#38bdf8',
        grad0: 'rgba(56, 189, 248, 0.45)',
        grad1: 'rgba(56, 189, 248, 0.02)',
        unit: '',
        getValue: d => d.diaper,
        formatVal: v => Math.round(v)
      }
    }[metric];

    document.getElementById('trend-chart-title').textContent = metricConfig.title;
    const badgeElem = document.getElementById('trend-metric-badge');
    badgeElem.textContent = `${metricConfig.badge} • ${state.reviewPeriod === 'monthly' ? '30 Days Graph' : (state.reviewPeriod === 'weekly' ? '7 Days' : 'Daily')}`;
    badgeElem.style.color = metricConfig.color;

    const barsContainer = document.getElementById('trend-bars-container');
    const graphWrapper = document.getElementById('trend-graph-container');

    if (state.reviewPeriod === 'monthly') {
      // Monthly View: Smooth Continuous Line Graph instead of a bar chart
      barsContainer.classList.add('hidden');
      graphWrapper.classList.remove('hidden');

      const canvas = document.getElementById('monthly-trend-canvas');
      const ctx = canvas.getContext('2d');
      const width = canvas.width;
      const height = canvas.height;
      ctx.clearRect(0, 0, width, height);

      // Check which days have logged data for this metric
      const rawData = dailyData.map(d => {
        const val = metricConfig.getValue(d);
        const hasData = (metric === 'wake') ? d.hasAnyData : val > 0;
        return { val, hasData };
      });
      const hasAnyData = rawData.some(r => r.hasData);

      // Interpolate values across untracked / missing days so graph doesn't unrealistically plunge to 0
      const interpolatedValues = rawData.map((item, idx) => {
        if (item.hasData || !hasAnyData) return item.val;
        let prevIdx = -1;
        for (let j = idx - 1; j >= 0; j--) {
          if (rawData[j].hasData) { prevIdx = j; break; }
        }
        let nextIdx = -1;
        for (let j = idx + 1; j < rawData.length; j++) {
          if (rawData[j].hasData) { nextIdx = j; break; }
        }
        if (prevIdx !== -1 && nextIdx !== -1) {
          const ratio = (idx - prevIdx) / (nextIdx - prevIdx);
          return rawData[prevIdx].val + ratio * (rawData[nextIdx].val - rawData[prevIdx].val);
        } else if (prevIdx !== -1) {
          return rawData[prevIdx].val;
        } else if (nextIdx !== -1) {
          return rawData[nextIdx].val;
        }
        return 0;
      });

      const maxVal = Math.max(1, Math.max(...interpolatedValues) * 1.15);
      const minVal = Math.max(0, Math.min(...interpolatedValues) * 0.85);
      const range = (maxVal - minVal) || 1;

      const padTop = 16;
      const padBottom = 22;
      const padLeft = 14;
      const padRight = 14;
      const chartW = width - padLeft - padRight;
      const chartH = height - padTop - padBottom;
      const stepX = chartW / Math.max(1, dailyData.length - 1);

      // Horizontal reference grid lines
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.08)';
      ctx.lineWidth = 1;

      [0, 0.5, 1].forEach(ratio => {
        const y = padTop + chartH * (1 - ratio);
        ctx.beginPath();
        ctx.moveTo(padLeft, y);
        ctx.lineTo(width - padRight, y);
        ctx.stroke();
      });

      const points = dailyData.map((d, idx) => {
        const x = padLeft + idx * stepX;
        const v = interpolatedValues[idx];
        const norm = (v - minVal) / range;
        const y = padTop + chartH * (1 - norm);
        return { x, y, val: v, hasData: rawData[idx].hasData };
      });

      if (points.length >= 2) {
        // Gradient Area Fill under smooth curve
        ctx.beginPath();
        ctx.moveTo(points[0].x, padTop + chartH);
        ctx.lineTo(points[0].x, points[0].y);
        for (let i = 0; i < points.length - 1; i++) {
          const p0 = points[i];
          const p1 = points[i + 1];
          const cx = (p0.x + p1.x) / 2;
          ctx.bezierCurveTo(cx, p0.y, cx, p1.y, p1.x, p1.y);
        }
        ctx.lineTo(points[points.length - 1].x, padTop + chartH);
        ctx.closePath();

        const areaGrad = ctx.createLinearGradient(0, padTop, 0, padTop + chartH);
        areaGrad.addColorStop(0, metricConfig.grad0);
        areaGrad.addColorStop(1, metricConfig.grad1);
        ctx.fillStyle = areaGrad;
        ctx.fill();

        // Stroke line
        ctx.beginPath();
        ctx.moveTo(points[0].x, points[0].y);
        for (let i = 0; i < points.length - 1; i++) {
          const p0 = points[i];
          const p1 = points[i + 1];
          const cx = (p0.x + p1.x) / 2;
          ctx.bezierCurveTo(cx, p0.y, cx, p1.y, p1.x, p1.y);
        }
        ctx.strokeStyle = metricConfig.color;
        ctx.lineWidth = 2.5;
        ctx.lineCap = 'round';
        ctx.stroke();

        // Data points (only highlight days with actual tracked data)
        points.forEach((pt) => {
          if (pt.hasData) {
            ctx.beginPath();
            ctx.arc(pt.x, pt.y, 2.5, 0, 2 * Math.PI);
            ctx.fillStyle = '#ffffff';
            ctx.fill();

            ctx.beginPath();
            ctx.arc(pt.x, pt.y, 1.5, 0, 2 * Math.PI);
            ctx.fillStyle = metricConfig.color;
            ctx.fill();
          }
        });

        // X-Axis date labels (5 dates evenly spaced)
        const axisContainer = document.getElementById('graph-x-axis');
        axisContainer.innerHTML = '';
        const labelIndices = [0, Math.floor(dailyData.length / 4), Math.floor(dailyData.length / 2), Math.floor((3 * dailyData.length) / 4), dailyData.length - 1];
        labelIndices.forEach(idx => {
          const span = document.createElement('span');
          span.textContent = dailyData[idx].dateLabel;
          axisContainer.appendChild(span);
        });
      }
    } else {
      // Weekly View (or Daily): Discrete Bar Chart for 7 days
      graphWrapper.classList.add('hidden');
      barsContainer.classList.remove('hidden');
      barsContainer.innerHTML = '';

      const subset = dailyData.slice(-7);
      const values = subset.map(b => metricConfig.getValue(b));
      const maxVal = Math.max(1, Math.max(...values));

      subset.forEach(b => {
        const val = metricConfig.getValue(b);
        const heightPercent = Math.max(8, (val / maxVal) * 100);
        const col = document.createElement('div');
        col.className = 'trend-bar-col';
        col.innerHTML = `
          <span class="bar-val" style="color: ${metricConfig.color};">${metricConfig.formatVal(val)}</span>
          <div class="bar-pillar" style="height: ${heightPercent}%; background: linear-gradient(180deg, ${metricConfig.color} 0%, rgba(30, 41, 59, 0.8) 100%);"></div>
          <span class="bar-day">${b.dayName}</span>
        `;
        barsContainer.appendChild(col);
      });
    }
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

    // Day Start Hour Select
    const hourSelect = document.getElementById('select-day-start-hour');
    if (hourSelect) {
      hourSelect.innerHTML = '';
      const currentStartHour = (profile && profile.dayStartHour !== undefined) ? profile.dayStartHour : 7;
      for (let h = 0; h < 24; h++) {
        const opt = document.createElement('option');
        opt.value = h;
        const hStr = (h < 10 ? '0' : '') + h + ':00';
        opt.textContent = `${hStr}${h === 7 ? ' (Default)' : ''}`;
        if (h === currentStartHour) {
          opt.selected = true;
        }
        hourSelect.appendChild(opt);
      }
    }

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

  // Selectable Metric Cards in Reviews
  ['sleep', 'wake', 'feed', 'diaper'].forEach(m => {
    const card = document.getElementById(`metric-card-${m}`);
    if (card) {
      card.addEventListener('click', () => {
        state.selectedMetric = m;
        updateReviewsView();
      });
    }
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
        dayStartHour: 7,
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
    const profile = getActiveProfile();
    const next = state.selectedDayStart + 24 * 60 * 60 * 1000;
    if (next <= getStartOfDay(getSimulatedNow(), profile.dayStartHour || 7)) {
      state.selectedDayStart = next;
      renderClockChart();
    }
  });

  // Settings Controls Event Listeners
  document.querySelectorAll('#wake-window-chips .choice-chip').forEach(chip => {
    chip.addEventListener('click', (e) => {
      const profile = getActiveProfile();
      const val = e.target.dataset.val;
      profile.customWakeWindowMinutes = val === 'auto' ? null : parseInt(val, 10);
      saveState();
      updateDashboardUI();
      updateSettingsView();
    });
  });

  document.querySelectorAll('#feed-interval-chips .choice-chip').forEach(chip => {
    chip.addEventListener('click', (e) => {
      const profile = getActiveProfile();
      profile.feedingIntervalMinutes = parseInt(e.target.dataset.val, 10);
      saveState();
      updateDashboardUI();
      updateSettingsView();
    });
  });

  const hourSelectElem = document.getElementById('select-day-start-hour');
  if (hourSelectElem) {
    hourSelectElem.addEventListener('change', (e) => {
      const val = parseInt(e.target.value, 10);
      const profile = getActiveProfile();
      profile.dayStartHour = val;
      state.selectedDayStart = getStartOfDay(getSimulatedNow(), val);
      saveState();
      renderClockChart();
      updateSettingsView();
    });
  }

  const birthInput = document.getElementById('input-profile-birthdate');
  if (birthInput) {
    birthInput.addEventListener('change', (e) => {
      const profile = getActiveProfile();
      if (e.target.value) {
        profile.birthTimestamp = new Date(e.target.value).getTime();
        saveState();
        updateDashboardUI();
        updateSettingsView();
      }
    });
  }

  const notifToggle = document.getElementById('toggle-push-notifs');
  if (notifToggle) {
    notifToggle.addEventListener('change', (e) => {
      const profile = getActiveProfile();
      profile.enablePushNotifications = e.target.checked;
      saveState();
    });
  }

  const calToggle = document.getElementById('toggle-calendar-sync');
  if (calToggle) {
    calToggle.addEventListener('change', (e) => {
      const profile = getActiveProfile();
      profile.enableCalendarSync = e.target.checked;
      saveState();
    });
  }

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
