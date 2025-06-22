### **Version History and changes:**

| __Version__ | __Date__ | __Changes__                                                                              |
|-------------|----------|------------------------------------------------------------------------------------------|
|             |          |                                                                                          |
| 2.13.1      |          | The app entirely supports dark mode.                                                     |
| 2.12.1      |          | Theming overhaul. Partial app supports dark mode.                                        |
| 2.12        |          | Now exports and imports in *.db + *.json files. Changed goals date format to yyyy-mm-dd. |
| 1.0         |          | Debug app                                                                                |

    54. 4.0.a (19/06/2025)
     > Implemented streak functionality - BROKEN.
     > The table creation inside meditationlogs.db mdessed up other fucntions.

    53. 3.2.a (19/06/2025)
     > Streak feature implemented. UI only.
     > Details about it in the readme on Github.
     > Enabled minification and shrink resources.

    52. 3.1.d (07/06/2025)
     > Removed leftovers from old separate activity files and classes.

    51. 3.1.c (07/06/2025)
     > Graph screens scrolling looks more polished.

    51. 3.1.b (07/06/2025)
     > Minor font size and button height changes.

    50. 3.1.a (07/06/2025)
     > Summary screen overhauled:
       > Now all graphs in one screen.
       > Graph screens majorly overhauled.
       > Swipe-able between WMY cards, also via buttons

    49. 3.0.h (05/06/2025)
     > Weekly screens now show date in dd-E format - making it easier to decipher which date, on which day.

    48. 3.0.g (05/06/2025)
     > Alignment changes to graphs screens.
     > 0 displays no week days now. 
     > Prev/next button spacing adjusted. 
     > Changed graphs color.

    46. 3.0.f (04/06/2025)
     > Alignemnt fixed
     > Bar colors changed.
    
    45. 3.0.e (01/06/2025)
     > TimerService fixed - secondsElapsed is reconstructed on every tick
     > Tap notification to open the app - added.
    
    44. 3.0.d
     > Upgraded gradle version to latest.
     > 0 hrs data days/weeks/months do not show up.


    43. 3.0.c  
     > Bar charts do no display excess 0s.
     > All screens are now scrollable.
     > Although, at the cost of lovely goals screen scroll.

    42. 3.0.b

    41. 3.0.a
     > Timerservice overhaul.
     > Back pressing overhaul.
     > Bar charts changes.

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
     > The app is now 100% optimized for dark mode.
     > Timer now works in background.
     > Progress bar works as intended.
     > Goal Cards now have proper spacing.
     > Theme switcher works flawlessly.

    31. 2.13.2

    30. 2.13.1
     > The app entirely supports dark mode.

    29. 2.12.1
     > Theming overhaul. Partial app supports dark mode.

    28. 2.12
     > Now exports and imports in *.db + *.json files
     > Changed goals date format to yyyy-mm-dd

    27. 2.11.9.7
    26. 2.11.9.6
    25. 2.11.9.5
    24. 2.11.9.3
    23. 2.11.9.1

    22. 2.11.9 (24/01)
     > Sorted out Progress bar issue; displays and refreshes correctly in all places.

    21. 2.11.6.3 (23/01)
     > Changes to about screen.

    20. 2.11.6.2 (23/01)
     > Parsing error while adding goals rectified. Adding goals now adds with timestamps

    19. 2.11.6.1 (23/01)
     > Changed how date looked in duration of goalcard.
     > Changed the about app image.
     > Export now exports with timestamps.

    18. 2.11.5 (22/01)
     > Export data now works.

    17. 2.11.4 (22/01)
     > Menu and delete button works now.
     > Export issues due to permissions.
     > Introduced Goal card visibility on home screen and a button to go to goals screen directly.

    16. 2.11.3 (22/01)
     > Scroll works with some issues; implemented recyclerview. Discarded inflategoalCards and nestedscrollview
     > Issues:
       > Menu doesn't work in goal screen
       > Delete button doesn't work

    15. 2.11
     > Monthly display of weekly hours works perfectly, Implemented calendar and timestamps in the meditation database.

    14. 2.10.8
    13. 2.10.7

    12. 2.10.6
     > Goal cards issue solved. Took help from blackbox.ai.
       > Issue lied with how goal container was being refreshed in loadGoals() and the layout of, not just scroll view, but also goal item.

    11. 2.10.2
     > Rectified the error of meditation time in week 4 of January not being displayed correctly in the graph view.

    10. 2.10
     > Added a functionality to switch buttons in summary screens.

    9. 2.09.6
     > Rectified inability to refresh UI after deleting a goal.

    8. 2.09.5
     > Added a "session ID" table in meditation logs, earlier date itself was ID, meaning multiple sessions from a single day would look like one single session.
     > This can be used for future functionality within the app for something like frequency of the meditation sessions per day, etc.

    7. 2.09.1
     > Data now exports to the Documents directory in storage.

    6. 2.09
     > Added a button to import old meditation data
     > Added the functionality

    5. 2.08
     > Added functional graphs to respective pages

    4. 2.07
     > Added graphview to respective pages

    3. 2.06
     > Minor aesthetic improvements

    2. 2.05
     > Added database functionality to store goals and logs

    1. 1.0
     > Debug app
     > Timer works