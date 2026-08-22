# Creator Runtime device failure matrix v5

## User-observed result

The user reports that the delivered APK does not behave as specified on the Redmi M2101K7BG, Android 13 / SDK 33. The previous green emulator workflow is therefore insufficient evidence and must not be treated as proof of the physical-device flow.

| Step | Required observable state | Reported state | Evidence status | Next diagnostic assertion |
|---|---|---|---|---|
| 1. App launch | Original Sketchware DesignActivity opens on the main activity, with the starter Continue widget visible and editable | User reports the product does not reach the intended usable flow | Unreproduced in current session | Capture launcher component, activity stack, and first rendered view IDs/text |
| 2. Main editor | Main screen contains the same editable Continue widget as runtime | Continue may be absent, misplaced, or the editor may show a different state | Unreproduced in current session | Inspect `main.xml`, ViewBeans, root parent/index, and View editor hierarchy after ProjectLoader |
| 3. Components | Intent component exists for Continue from first run | User previously reported it missing | Prior source/native assertions only | Inspect Java-name component store after editor load, not only after bridge provisioning |
| 4. Logic/events | Continue has editable onClick blocks that navigate to editor | User previously reported missing blocks or blank visual block editor | Prior importer/unit assertions only | Open actual Logic tab and inspect event/block rows and block editor state |
| 5. User edit | Adding a button in original View editor and configuring its event persists without save dialog | User reports added button disappears after Back | Physical-device failure | Mutate through actual ViewEditor UI, then inspect legacy disk/cache before and after Back |
| 6. Back | Physical Back returns to native runtime showing the exact edited main screen | User reports blank/different screen or only default Continue | Physical-device failure | Compare runtime revision, imported widgets, ViewBeans, and live canvas tags after transition |
| 7. Reopen | Reopening editor shows the same edited widget and behavior | User reports state mismatch | Physical-device failure | Reopen with same sc_id/project id and compare all four stores |
| 8. Activity manager | Built-in `main` and locked `editor` activities are visible, lock state is shown, and lock toggle is editable | User reports editor activity/lock UI absent | Prior source/native assertions only | Exercise ViewSelector and ManageViewActivity on API 33-equivalent path |
| 9. Runtime controls | Lower arrow/options and Run are hidden; right sidebar contains original actions plus Versions | User reports lower arrow remains and runtime UI differs | Prior source assertions incomplete | Assert actual visibility after ProjectLoader/onResume and drawer population |

## Important limitation

This file deliberately records the user's report as an unresolved physical-device failure. No claim of fix is valid until a reproducible test or a user-confirmed APK run demonstrates the same flow.
