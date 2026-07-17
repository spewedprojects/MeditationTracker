<table width="100%">
  <thead>
    <tr>
      <th width="10%" align="right">Ver.</th>
      <th width="20%" align="left">Date</th>
      <th width="70%" align="left">Breaking change / reason for major bump</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td align="right">11</td>
      <td>27 Jan 2026</td>
      <td>Redesigned summary architecture: <code>SummaryActivity</code> → fragment + <code>ViewPager2</code> infinite carousel (breaking rewrite of summary UI/fragment contract and data-loading flow).</td>
    </tr>
    <tr>
      <td align="right">10</td>
      <td>14 Jan 2026</td>
      <td>Goal-tracking data model refactor: <code>Goal</code> now includes <code>dailyTarget</code> and <code>dateRange</code> (adapter/DB changes affecting goal persistence and UI).</td>
    </tr>
    <tr>
      <td align="right">9</td>
      <td>13 Jan 2026</td>
      <td>Full reporting system added: <code>ReportGenerator</code> / <code>ReportJsonHelper</code>, <code>ReportDetailDialogFragment</code> and <code>ReportsActivity</code> (new report data formats and storage).</td>
    </tr>
    <tr>
      <td align="right">8</td>
      <td>12 Jan 2026</td>
      <td>Chart/picker overhaul: extracted chart logic, carousel-style year/month picker and MotionLayout-based reports UI (significant UI/UX component changes).</td>
    </tr>
    <tr>
      <td align="right">7</td>
      <td>08 Jan 2026</td>
      <td>Backdated-entry & streak resurrection: added backdated manual entries and DB support to resurrect streaks (schema/DB logic change for streaks).</td>
    </tr>
    <tr>
      <td align="right">6</td>
      <td>08 Jan 2026</td>
      <td>Summary navigation and menu refactor: fixed <code>SummaryActivity</code> navigation/refresh logic and reworked menu drawer layout (breaking UI/navigation behaviour fixes).</td>
    </tr>
    <tr>
      <td align="right">5</td>
      <td>22 Jun 2025</td>
      <td>Streak system implemented in SQLite: new <code>Streak</code> model + <code>StreakDatabaseHelper</code> and <code>StreakManager</code> (database schema + import/export changes).</td>
    </tr>
    <tr>
      <td align="right">3</td>
      <td>07 Jun 2025</td>
      <td>Summary/graphs overhaul: consolidated all graphs into a single swipeable screen (major UI flow change).</td>
    </tr>
    <tr>
      <td align="right">2</td>
      <td>31 May 2025</td>
      <td>Chart rendering update: introduced rounded bar rendering and revived <code>RoundedBarChartRenderer</code> (visual/renderer change impacting chart modules).</td>
    </tr>
    <tr>
      <td align="right">1</td>
      <td>07 Feb 2025</td>
      <td>Core UI/menu polish: menu/title/close layout refactor and swipeable menu (foundation UI changes that stabilized app look/feel).</td>
    </tr>
  </tbody>
</table>
***

# (v14.3.1) 17/07/2026
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
- ~~**Adding a homescreen widget to remind of meditation progress, specifically, most current goal progress and streak.**~~ __DONE!!__ -> (v13 - 10/02/2026)
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