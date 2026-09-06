**06/09/2026**
- Enhanced `StreakManager` with logic to calculate and format the specific date range (start to end) of the longest meditation streak.
- Refactored `progress_bar_style.xml` to use theme-based attributes (`?attr/colorPrimary`) for gradients instead of hardcoded hex values.
- Standardized margins and updated placeholder progress states in `item_goal.xml`.
- Updated `ReleaseNotesActivity` to load from a new `latest_release.md` raw resource instead of string resource - utilizing Markdown here as well.

NEXT:
- [x] Main screen should dynamically adjust constraints based on goals visibility.
  - Its possibly done.
- [x] Smaller devices still need meticulous planning.
- [ ] Add an option in settings to disable landscape specific layouts.