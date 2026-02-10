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

### **Version history and changes:**

Latest release **_[here](https://github.com/spewedprojects/MeditationTracker/releases/latest)_**.

85. **_[12.0.1 (02/02/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v12.0.1)_**
    > - Simplified `goal_item.xml` by removing the redundant outer `ConstraintLayout`.
    > - Renamed `cardView_goals_list` to `cardView_goal` in `MainActivity` and `activity_main.xml`.
    > - Removed the cap on goal progress percentage, allowing it to exceed 100% in `GoalsActivity` and `MainActivity`.

     84. [12.0.0 (02/02/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v12.0.0)
    >  - Implemented `ClearFocusUtils` to automatically clear focus from input fields.
    >  - Refactored `MeditationChartManager` Y-axis logic to allow hiding labels/lines while maintaining a fixed scale (0 to Max) to prevent "floating" bars.
    >  - Updated `SummaryFragment` to hide the Y-axis in charts.
    >  - Migrated `GoalsDatabaseHelper` to version 6, updating `COLUMN_TARGET_HOURS.

     83. [11.2.0 (01/02/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v11.2.0)
    >  - A new `setYAxisEnabled` method to toggle Y-axis visibility - `false` in report card.
    >  - Replaced Left/Right with Start/End across layouts.
    >  - Standardized menu item texts, button descriptions using new string resources.
    >  - Auto button for theme switcher is now an image instead of text.

     82. [11.1.2 (31/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v11.1.2)
    >  - Added `StreakDatabaseHelper` support for JSON export and import, ensuring streak history is included in data backups.
    >  - Updated `BaseActivity` to include streak data in `exportDataAsJson` and `importDataFromJson` methods.
    >  - Updated `SummaryFragment` to force a data refresh in `onResume`.
    >  - Adjusted `dialog_streak.xml` constraint bias for better vertical alignment.
    >  - Lighter scrim color for navigation drawer - 0.2 now, vs 0.5 earlier.

     81. [11.1.1 (29/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v11.1.1)
    >  - Applied rounded corner backgrounds to `TimePickerDialog` and `DatePickerDialog` across `GoalsActivity`.
    >  - Improved `ViewPager` navigation logic in `SummaryActivity` to transition to the nearest relative page (Week/Month).
    >  - Optimized data loading in `SummaryFragment` by triggering `refreshData()` during `onViewCreated`.

     80. [11.1.0 (27/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v11.1.0)
    >  - Added Streak Stats view in `StreakDialogFragment`, showing longest streak and current progress.
    >  - Redesigned streak dialog with `HorizontalScrollView` + snap behavior for toggling goal input/stats.
    >  - Major UI refinements: new `ImageButton` close, updated icons, RTL-friendly constraints, improved button styles.

     79. [11.0.1 (27/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v11.0.1)
    >  - Migrated multiple UI elements from `?attr/colorAccent` to a static `@color/onPrimary2` for better color consistency.
    >  - Added `android:textColorHighlight` to both light and night themes using a new `text_highlight_color` resource.

     78. **[11.0.0 (27/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v11.0.0)**
    >  - Redesigned Summary Screen – Infinite scrolling between Week/Month/Year views with smoother navigation, these are now view fragments inflated via `viewpager2`.
    >  - Faster performance via optimized database queries, smarter date handling, and a state‑aware Add Entry button (default - disabled).
    >  - UI/UX Enhancements – Modernized layouts, rounded dialogs, refined icons, and consistent button styling for a cleaner look.

     77. [10.0.b (16/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v10.0.b)
    >  - Migrated report persistence from a single JSON file to individual files per report in a dedicated `reports/` directory.
    >  - Wrapped menu items in a `NestedScrollView` to support smaller screens.
    >  - Removed `textIsSelectable` and `focusableInTouchMode` for a cleaner interaction model and to make links clickable.

     76. [10.0.a (15/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v10.0.a)
    >  - Redesigned `minireport_card.xml` to use horizontal rows with weighted columns, thereby refining alignment.
    >  - Removed padding for the Week/Month/Year toggle buttons in `activity_summary.xml`.
    >  - Font and size changes to release notes view.

     75. **[10.0.0 (14/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v10.0.0)**
    >  - Goal cards now show target per day, instead of just the entire target. Made some Changes to how date range was being displayed and refactored teh corresponding files. 
    >  - Refined mini report card layout and wrapped full report card contents inside a nestedscrollview.
    >  - Font and size changes to release notes view. 
    >  - Main screen polishing with gradient while scrolling below static cards.

     74. [9.1.a (13/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v9.1.a)
    >  - Added `avgSessionGap` and yearly activity extremes.
    >  - Refined `streakStability` calculation to represent average streak length.
    >  - Implemented state persistence for the goal card using SharedPreferences.

     73. **[9.0.a (13/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v9.0.a)**
    >  - ### HUGE ONE!!! Reports are ready!
    >    - ReportJsonHelper, MeditationReportData, ReportGenerator, ReportDetailDialogFragment - files that made it happen.
    >    - Button Ready to use in Summary activity
    >  - Next will be adding a persistent goal collapse state.

     72. **[8.1.a (12/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v8.1.a)**
    >  - Extracted chart configuration logic into a new `MeditationChartManager` class to reduce boilerplate in `SummaryActivity`
    >  - Dialogs completely ready, visually. Using new classes to inject year and handle the carousel.
    >  - The prev./next buttons now without any BG, looks better this way. Text size increased and font thickened.
    >  - Added a background to about screen
    >  - WIP - Reports screen; dialogs visible, no function yet.

     71. [7.3.b (10/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v7.3.b)
    >  - Organized classes into new packages.
    >  - Updated dialog fragment windows to `MATCH_PARENT` width, touching the edges of the screen.
    >    - Since hte sides will touch the edges, the dialog boxes will not prematurely shrink and will also retain the elevation shadow.

     70. [7.3.a (10/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v7.3.a)
    >  - Correctly handling streak database upgrade.
    >  - WIP - Reports screen.

     69. [7.2.b (09/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v7.2.b)
    >  - _The lovely goals card scroll is back - much more usable on small screens._
    >  - __HUGE!__ Implemented a collapsible "Set a New Goal" card using a button which animates as changes occur.
    >  - Using LinearLayout wherever possible.

     68. **[7.1.a (08/01/2026)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v7.1.a)**
    >  - Added back-dated entry feature, via long click on (Add manually) opens a dialog box with blur bg.
    >  - Refined streakmanager to resurrect streak in case of contiguous day due to back entry.
    >  - Added a `status` column to the streaks table to distinguish between active and inactive/failed goals (db ver. 2).

     67. 6.1.e (08/01/2026)
    >  - Fixed drill-down by making `monthLoad` and `weekLoad` to false. Now drills to the correct month and week.
    >  - Reports screen initiated (button hidden).
    >  - Using `LinearLayout` in menu for vertical items - simplifying.
    
     66. 6.1.d (08/01/2026)
    >  - Implemented drill-down functionality in `SummaryActivity`; long-pressing on Yearly or Monthly chart bars now navigates to the specific Month or Week view respectively.
    >  - Monthly view to display dynamic week numbers (e.g., "Week #42").
    >  - Long pressing (This week) on main screen will open weekview specifically, not just Summary screen.

     65. 6.1.c (07/01/2026)
    >  - Applied window insets listeners to handle system bar padding programmatically.
    >  - Applied EdgetoEdge to go along with transparent actionbar.

     64. 6.1.b (07/01/2026)
    >  - Cosmetic changes to Streak dialog.
    >  - No solid actionbar.
    >  - Release notes now have cardview for current version.
    >  - Colors are now stored in night and not-night instead of Dark/Light prefixes.

     63. **[6.1.a (18/09/2025)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v6.1.a)**
    >  - Upgraded Gradle wrapper to 8.13 and updated various AndroidX and Material dependencies.
    >  - Made About screen scrollable for better usability on smaller displays.
    >  - Replaced deprecated `LocalBroadcastManager` with `ContextCompat.registerReceiver` for timer updates.

     62. 5.1.f (25/06/2025)    
    >  - Refined streak logic: Streak now continues from yesterday if no meditation today, instead of resetting to 0.
    >  - Adjusted "d" unit size next to streak days to 18% (was 25%).
    >  - Minor UI tweaks in About screen layout.
    >  - Updated version name and code.
    >  - Added clickable attribute to menu drawer; prevents clicks to pass on to the screen beneath.
    >  - Week total display is now long-clickable and navigates to the Summary activity (version code - 52)

     61. 5.1.e (24/06/2025)
    >  Programmatically added the "d" unit next to streak days, scaled to 25% size and 100 alpha.

     60. 5.1.d (23/06/2025)
    >  - Refactored streak calculation in `StreakManager` to count backwards from today.
    >  - Updated `MainActivity` to refresh UI elements and clear focus.

     59. 5.1.c (23/06/2025)
    >  Minor text layout adjustments and applied the new font to SummaryActivity charts.

     58. 5.1.b (23/06/2025)
    >  Database and streak functionality in place and works.

     57. 5.1.a (22/06/2025)
    >  - Added `Streak.java` and `StreakDatabaseHelper.java` for storing and managing streak data.
    >  - Minor UI adjustments in `activity_main.xml`.

     56. 5.0.b (22/06/2025)
    >  - Fixed crash due to toolbar/constraint issue.

     55. **[5.0.a (22/06/2025)](https://github.com/spewedprojects/MeditationTracker/releases/tag/v5.0.a)**
    >  - Font style is now "Atkinson Hyperlegible Next" because I like the way ZERO looks in it.
    >  - Made a separate actionbar layout resource file and included it in all the screens - reducing redundancies.
    >  - Removed the leftover layout files of separate summary screens.
    >  - Rolled back streak logs been fed into medtitation logs database. A separate database will be maintained to store that data.

     54. 4.0.a (19/06/2025)
    >  - Implemented streak functionality - BROKEN.
    >  - The table creation inside meditationlogs.db mdessed up other fucntions.

     53. 3.2.a (19/06/2025)
    >  - Streak feature implemented. UI only.
    >  - Details about it in the readme on Github.
    >  - Enabled minification and shrink resources.

     52. 3.1.d (07/06/2025)
    >  Removed leftovers from old separate activity files and classes.

     51. 3.1.c (07/06/2025)
    >  Graph screens scrolling looks more polished.

     51. 3.1.b (07/06/2025)
    >  Minor font size and button height changes.

     50. 3.1.a (07/06/2025)
    >  - Summary screen overhauled:
    >  - Now all graphs in one screen.
    >  - Graph screens majorly overhauled.
    >  - Swipe-able between WMY cards, also via buttons

     49. 3.0.h (05/06/2025)
    >  Weekly screens now show date in dd-E format - making it easier to decipher which date, on which day.

     48. 3.0.g (05/06/2025)
    >  - Alignment changes to graphs screens.
    >  - 0 displays no week days now. 
    >  - Prev/next button spacing adjusted. 
    >  - Changed graphs color.

     46. 3.0.f (04/06/2025)
    >  - Alignemnt fixed
    >  - Bar colors changed.
    
     45. 3.0.e (01/06/2025)
    >  - TimerService fixed - secondsElapsed is reconstructed on every tick
    >  - Tap notification to open the app - added.
    
     44. 3.0.d
    >  - Upgraded gradle version to latest.
    >  - 0 hrs data days/weeks/months do not show up.

     43. 3.0.c  
    >  - Bar charts do no display excess 0s.
    >  - All screens are now scrollable.
    >  - Although, at the cost of lovely goals screen scroll.

     42. 3.0.b

     41. 3.0.a
    >  - Timerservice overhaul.
    >  - Back pressing overhaul.
    >  - Bar charts changes.

     40. 2.17.j
    >  - Weekly page reflects current week on opening; showed next week.
    >  - Timerservice will run no matter what - and won't ever stop (bug). Using sharedprefs to store time.

     39. 2.17.i
    >  - Bar charts now have 7dp rounded corners.
    >  - Revived the RoundedBarChartRenderer Utility.

     38. 2.17.h
    >  Bar charts now devoid of any color; goes will with the overall theme.

     37. 2.17.g
    >  Fixed: Now Monday starts the week in monthly summaries. It was sunday before.

     36. 2.17.f
    >  - Fixed database leaks using "db.close();"
    >  - Added a link to the GitHub profile via ImageView in the about screen.
    >  - NEXT: Home screen widget.

     35. 2.17.e
    >  - Added "This Week: " in main screen.
    >  - New, switchable, method to add goals.

     34. 2.17.d
    >  - Rectified alignment issues with the two methods in the goals screen.
    >  - Resetting input fields works as intended.

     33. 2.17.a
    >  New, switchable, method to add goals.

     32. 2.16.e
    >  - The app is now 100% optimized for dark mode.
    >  - Timer now works in background.
    >  - Progress bar works as intended.
    >  - Goal Cards now have proper spacing.
    >  - Theme switcher works flawlessly.

     31. 2.13.2

     30. 2.13.1
    >  - The app entirely supports dark mode.

     29. 2.12.1
    >  - Theming overhaul. Partial app supports dark mode.

     28. 2.12
    >  - Now exports and imports in *.db + *.json files
    >  - Changed goals date format to yyyy-mm-dd

     27. 2.11.9.7
    >

     26. 2.11.9.6
    >

     25. 2.11.9.5
    >

     24. 2.11.9.3
    >

     23. 2.11.9.1
    >

     22. 2.11.9 (24/01)
    >  Sorted out Progress bar issue; displays and refreshes correctly in all places.

     21. 2.11.6.3 (23/01)
    >  Changes to about screen.

     20. 2.11.6.2 (23/01)
    >  Parsing error while adding goals rectified. Adding goals now adds with timestamps

     19. 2.11.6.1 (23/01)
    >  - Changed how date looked in duration of goalcard.
    >  - Changed the about app image.
    >  - Export now exports with timestamps.

     18. 2.11.5 (22/01)
    >  Export data now works.

     17. 2.11.4 (22/01)
    >  - Menu and delete button works now.
    >  - Export issues due to permissions.
    >  - Introduced Goal card visibility on home screen and a button to go to goals screen directly.

     16. 2.11.3 (22/01)
    >  Scroll works with some issues; implemented recyclerview. Discarded inflategoalCards and nestedscrollview
    >  - Issues:
    >    - Menu doesn't work in goal screen
    >    - Delete button doesn't work

     15. 2.11
    >  Monthly display of weekly hours works perfectly, Implemented calendar and timestamps in the meditation database.

     14. 2.10.8
    >

     13. 2.10.7
    >

     12. 2.10.6
    >  - Goal cards issue solved. Took help from blackbox.ai.
    >    - Issue lied with how goal container was being refreshed in loadGoals() and the layout of, not just scroll view, but also goal item.

     11. 2.10.2
    >  Rectified the error of meditation time in week 4 of January not being displayed correctly in the graph view.

     10. 2.10
    >  Added a functionality to switch buttons in summary screens.

     9. 2.09.6
    >  Rectified inability to refresh UI after deleting a goal.

     8. 2.09.5
    >  - Added a "session ID" table in meditation logs, earlier date itself was ID, meaning multiple sessions from a single day would look like one single session.
    >  - This can be used for future functionality within the app for something like frequency of the meditation sessions per day, etc.

     7. 2.09.1
    >  Data now exports to the Documents directory in storage.

     6. 2.09
    >  Added a button to import old meditation data; Added the functionality.

     5. 2.08
    >  Added functional graphs to respective pages

     4. 2.07
    >  Added graphview to respective pages

     3. 2.06
    >  Minor aesthetic improvements

     2. 2.05
    >  Added database functionality to store goals and logs

     1. 1.0
    >  - Debug app
    >  - Timer works