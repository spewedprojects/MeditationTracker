***Version History and changes:***

| __Version__ | __Changes__                                                                              |
|-------------|------------------------------------------------------------------------------------------|
| 2.13.1      | The app entirely supports dark mode.                                                     |
| 2.12.1      | Theming overhaul. Partial app supports dark mode.                                        |
| 2.12        | Now exports and imports in *.db + *.json files. Changed goals date format to yyyy-mm-dd. |
| 1.0         | Debug app                                                                                |

    40. 2.17.j
     > Weekly page reflects current week on opening; showed next week.
     > Timerservice will run no matter what - and won't ever stop (bug). Using sharedprefs to store time.

    39. 2.17.i
     > Bar charts now have 7dp rounded corners.
     > Revived the RoundedBarChartRenderer Utility.

    38. 2.17.h
     > Bar charts now devoid of any color; goes will with the overall theme.

    37. 2.17.g
     > Fixed: Now Monday starts the week in monthly summaries. It was sunday before.

    36. 2.17.f
     > Fixed database leaks using "db.close();"
     > Added a link to the GitHub profile via ImageView in the about screen.
     > NEXT: Home screen widget.

    35. 2.17.e
     > Added "This Week: " in main screen.
     > New, switchable, method to add goals.

    34. 2.17.d
     > Rectified alignment issues with the two methods in the goals screen.
     > Resetting input fields works as intended.

    33. 2.17.a
     > New, switchable, method to add goals.

    32. 2.16.e
     a. The app is now 100% optimized for dark mode.
     b. Timer now works in background.
     c. Progress bar works as intended.
     d. Goal Cards now have proper spacing.
     e. Theme switcher works flawlessly.

    31. 2.13.2

    30. 2.13.1
     a. The app entirely supports dark mode.

    29. 2.12.1
     a. Theming overhaul. Partial app supports dark mode.

    28. 2.12
     a. Now exports and imports in *.db + *.json files
     b. Changed goals date format to yyyy-mm-dd

    27. 2.11.9.7
    26. 2.11.9.6
    25. 2.11.9.5
    24. 2.11.9.3
    23. 2.11.9.1

    22. 2.11.9 (24/01)
     a. Sorted out Progress bar issue; displays and refreshes correctly in all places.

    21. 2.11.6.3 (23/01)
     a. Changes to about screen.

    20. 2.11.6.2 (23/01)
     a. Parsing error while adding goals rectified. Adding goals now adds with timestamps

    19. 2.11.6.1 (23/01)
     a. Changed how date looked in duration of goalcard.
     b. Changed the about app image.
     c. Export now exports with timestamps.

    18. 2.11.5 (22/01)
     a. Export data now works.

    17. 2.11.4 (22/01)
     a. Menu and delete button works now.
     b. Export issues due to permissions.
     c. Introduced Goal card visibility on home screen and a button to go to goals screen directly.

    16. 2.11.3 (22/01)
     a. Scroll works with some issues; implemented recyclerview. Discarded inflategoalCards and nestedscrollview
    b. Issues:
     i. Menu doesn't work in goal screen
     ii. Delete button doesn't work

    15. 2.11
     a. Monthly display of weekly hours works perfectly, Implemented calendar and timestamps in the meditation database.

    14. 2.10.8
    13. 2.10.7

    12. 2.10.6
     a. Goal cards issue solved. Took help from blackbox.ai.
      i. Issue lied with how goal container was being refreshed in loadGoals() and the layout of, not just scroll view, but also goal item.

    11. 2.10.2
     a. Rectified the error of meditation time in week 4 of January not being displayed correctly in the graph view.

    10. 2.10
     a. Added a functionality to switch buttons in summary screens.

    9. 2.09.6
     a. Rectified inability to refresh UI after deleting a goal.

    8. 2.09.5
     a. Added a "session ID" table in meditation logs, earlier date itself was ID, meaning multiple sessions from a single day would look like one single session.
     b. This can be used for future functionality within the app for something like frequency of the meditation sessions per day, etc.

    7. 2.09.1
     a. Data now exports to the Documents directory in storage.

    6. 2.09
     ○ Added a button to import old meditation data
     ○ Added the functionality

    5. 2.08
     ○ Added functional graphs to respective pages

    4. 2.07
     ○ Added graphview to respective pages

    3. 2.06
     ○ Minor aesthetic improvements

    2. 2.05
     ○ Added database functionality to store goals and logs

    1. 1.0
     ○ Debug app
     ○ Timer works