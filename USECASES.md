## Use Cases
### Profile Management (PM)
1. The main screen shows a list of profiles.
2. For each profile the name of the profile and number of timers is shown.
3. The name of the profile should be editable from within the profile details
4. Swipe-to-delete a profile from the list item 
5. In the profile details screen, it is possible to delete the profile using the trash icon
6. After deleting on the bottom is an notification shown indicating the delete and an option to undelete.
7. A new profile can be created directly from the profile list screen
8. Save profile with blank name → inline error below name field, save blocked
9. Save profile with no timers → error message "Add at least one timer", save blocked
10. Two profiles may share the same name (no uniqueness constraint)
11. It should not be possible to delete a profile while its session is active → disbled; prevent deletion until session is stopped
12. Profiles and there configuration persists across device restart

### Timer Management (TM)
1. Add timer with 0-second duration → inline error, save blocked
2. A timer can be made either reoccuring or fires once.
3. A timer can be configured in hours and minutes.

### Session Lifecycle (SL)
1. Start the profile from the profile list → navigates to Active Session → all timer countdowns are visible
2. When Tap Stop → confirmation dialog "Stop session?"  before navigating away (prevents accidental stop) to ProfileListScreen
3. Pause button on `ActiveSessionScreen` → all countdowns freeze in place → Resume → countdowns continue from exact position
4. Notification "Stop" action → session ends, notification dismissed; opening app returns to `ProfileListScreen`
5. System back button pressed on `ActiveSessionScreen` → confirmation dialog "Stop session?" before navigating away (prevents accidental stop)
6. Session runs for several hours → no memory leak; timer drift is corrected per cycle using wall-clock subtraction (`System.currentTimeMillis()`)

### Audio Behaviour (AB)
1. Pre warning At T-10s a pre-warning beep heard
2. Warning At T-0 (20s), "Drink" fires, cycle count increments, resets to 20s
3. Two timers with identical duration → both fire simultaneously; both alerts play and both cycle counts increment independently
4. Pre-warning beep fires while a previous alert is still playing → both play without interruption (`SoundPool` handles concurrent streams)
5. Phone in silent/vibrate mode → `USAGE_ALARM` audio attribute ensures alerts still play
6. Headphones connected → sound routes through headphones

### Resilience & State Restoration (RS)
1. When the Screen is off, at T-60s "Gel" fires, sound heard through speaker
2. App process killed mid-session → reopen app → `ActiveSessionScreen` is restored with correct countdown state (service is `START_STICKY`, re-reads profile from DB)
3. Screen rotation while session is active → session continues uninterrupted; UI reconnects to service and countdowns remain accurate

### Notification Interactions (NI)
1. Background app notification shows live countdown next timer
2. Tap notification → opens `ActiveSessionScreen` with live state (even if app was killed)

### Configuration Settings (CS)
1. In the configuration screen it should be possible to switch the UI theme using the options, Dark, Light and 'Operating System Theme'


