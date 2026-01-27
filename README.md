If it is the app that you seek to use you can find it in here (this is always latest): root\app\release -> ***[app_release.apk](https://github.com/spewedprojects/MeditationTracker/blob/master/app/release/app-release.apk)***

Or alternatively, you can find it in Releases section also.

### Google Play protect may ask to scan the app before installing; let it scan. **The app is safe to use.**

***

# (v11.0.b) 27/01/2026
**Current known issues:**
- **NONE (...of which I noticed).**
- ~~Streak Goal (when enabled) doesn't reset to 0 when the the chain is broken; only displays the no. of days contiguously recorded.~~ __DONE!! 08/01/2026__
- ~~App screens not scrollable on tiny screens.~~ __DONE!!__
- ~~Monthly summary graphs show incorrect week division; likely consistency issues with Week start day (Sunday or Monday)~~
- ~~Weekly summary somewhere still considering Sunday to Monday as one week.~~

__Resolved long standing issues:__
- Progress bar does not consider the end date in the date range.
- Huge space below each goal card.
- Timer would stop after device went to deep-sleep.

***
### **Future updates:**

- Overlay on existing graphs, showing time of mediation in a day.
  - A toggle to show/hide this.
  - Monthly graphs will show week wise avg.
- **Adding a homescreen widget to remind of meditation progress, specifically, most current goal progress and streak.**
- ~~Making menu more intuitive - swipe gestures (using navigation drawer)~~ __DONE!!__
- ~~Adding theme switcher.~~ __DONE!!__
- ~~Changes to method of adding goals (Daily duration, total hours, Start date, end date will be automatically determined)~~ __DONE!!__
- ~~Twice "back" to exit the app~~ **DONE!!**
- ~~Making the navbar/status bar look immersive.~~ <- __DONE!!__ (07/01/26)
- ~~__All summary screens in a single screen, grouped buttons to change between them:__~~
    > - ~~Grouped buttons to change between them.~~ <- __DONE!!__ (07/06/25)
    > - ~~Tapping on month label should open month picker to access any month of choosing.~~ <- __DONE!!__ (08/01/26)
    > - ~~Tapping on year label should open year picker to access any year of choosing.~~ <- __DONE!!__ (08/01/26)
    > - ~~Tapping the month in year graph should lead me to the month graph of that month.~~ <- __DONE!!__ (08/01/26)
    > - ~~Tapping the week in month graph should lead me to the week graph of that week.~~ <- __DONE!!__ (08/01/26)
- ~~Streak feature implementation: (19/06/2025)~~ **DONE!** -> (22/06/2025)
  > - ~~A new card on main screen showing streak days~~
  > - ~~Long pressing for >5secs will open a days picker to set a streak duration, And later on possibly also add a summary from past data, probably a one-liner.~~
  > - ~~A tiny progress bar at the bottom of streak days~~
  > - ~~When no streak goal is currently active or a streak is broken, simply display no. of contiguous days meditated, the progress bar disappears and the stroke color of the card becomes transparent; To indicated no streak is active.~~
  > - ~~The streak data will be kept in a new table inside meditation logs database - id, start date, end date, target streak days, longest streak~~
- ~~Backdated entries~~ __DONE!!__ -> (08/01/26)
  > ~~Long tapping "Add Manually" button will open up a date and time picker along with the same input fields to input duration.~~