# App, Category & Profile Blocking

## Rule Evaluation Priority
`RulePriorityResolver.resolve` evaluates every foreground app launch in deterministic order:
1. **Emergency Whitelisted** (`InstalledAppEntity.isEmergencyWhitelisted == true`) -> Allowed.
2. **Temporary Unlock Window** (active `tempUnlockUntilEpochMs > now`) -> Allowed.
3. **Active Focus Session** (focus phase active & package matches linked profile or distracting list) -> Blocked (Priority 90).
4. **Manual Quick Shield / Active or Scheduled Profile** -> Blocked (Priority 70–89).
5. **App Daily Limit + Grace Period** / **Shared App Group Cap** -> Blocked (Priority 60).
6. **Category Daily Limit** (`CategoryLimitEntity`) -> Blocked (Priority 58).
7. **Weekly Limit** -> Blocked (Priority 55).

## Threshold Notifications (80% / 90% / 100%)
During periodic WorkManager sync (`FocusUsageMonitorWorker`), `BlockerRepository.evaluateAndMarkAppLimitWarnings` tracks daily limit consumption once per date key (`warned80DateKey`, `warned90DateKey`, `warned100DateKey`) to prevent duplicate alerts.
