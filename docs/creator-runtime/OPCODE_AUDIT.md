# Creator Runtime Opcode-by-Opcode Audit

This report compares the legacy opcode truth set from `Fx.java` with the existing `creator-runtime` importer, executor/reporter evaluator, and operation mapper. A source opcode is considered covered when at least one target runtime path recognizes it; `importer+executor` is the strongest direct evidence.

## Summary

| Status | Count |
| --- | ---: |
| importer+executor | 9 |
| importer-only | 193 |
| executor/reporter-only | 102 |
| operation-mapper-only | 0 |
| source-only | 1 |
| **Total legacy opcodes** | **305** |

## Family coverage

| Family prefix | Total | Covered | Source-only |
| --- | ---: | ---: | ---: |
| `addSourceDirectly` | 1 | 0 | 1 |
| `addListInt` | 1 | 1 | 0 |
| `addListMap` | 1 | 1 | 0 |
| `adViewLoadAd` | 1 | 1 | 0 |
| `bluetoothConnectActivateBluetooth` | 1 | 1 | 0 |
| `bluetoothConnectGetPairedDevices` | 1 | 1 | 0 |
| `bluetoothConnectGetRandomUuid` | 1 | 1 | 0 |
| `bluetoothConnectIsBluetoothActivated` | 1 | 1 | 0 |
| `bluetoothConnectIsBluetoothEnabled` | 1 | 1 | 0 |
| `bluetoothConnectReadyConnection` | 1 | 1 | 0 |
| `bluetoothConnectReadyConnectionToUuid` | 1 | 1 | 0 |
| `bluetoothConnectSendData` | 1 | 1 | 0 |
| `bluetoothConnectStartConnection` | 1 | 1 | 0 |
| `bluetoothConnectStartConnectionToUuid` | 1 | 1 | 0 |
| `bluetoothConnectStopConnection` | 1 | 1 | 0 |
| `break` | 1 | 1 | 0 |
| `calendarAdd` | 1 | 1 | 0 |
| `calendarDiff` | 1 | 1 | 0 |
| `calendarFormat` | 1 | 1 | 0 |
| `calendarGetNow` | 1 | 1 | 0 |
| `calendarGetTime` | 1 | 1 | 0 |
| `calendarSet` | 1 | 1 | 0 |
| `calendarSetTime` | 1 | 1 | 0 |
| `calendarViewGetDate` | 1 | 1 | 0 |
| `calendarViewSetDate` | 1 | 1 | 0 |
| `calendarViewSetMinDate` | 1 | 1 | 0 |
| `calnedarViewSetMaxDate` | 1 | 1 | 0 |
| `camerastarttakepicture` | 1 | 1 | 0 |
| `closeDrawer` | 1 | 1 | 0 |
| `containListMap` | 1 | 1 | 0 |
| `copyToClipboard` | 1 | 1 | 0 |
| `cropBitmapFileFromCenter` | 1 | 1 | 0 |
| `currentTime` | 1 | 1 | 0 |
| `decreaseInt` | 1 | 1 | 0 |
| `definedFunc` | 1 | 1 | 0 |
| `deleteList` | 1 | 1 | 0 |
| `dialogCancelButton` | 1 | 1 | 0 |
| `dialogNeutralButton` | 1 | 1 | 0 |
| `dialogOkButton` | 1 | 1 | 0 |
| `dialogSetMessage` | 1 | 1 | 0 |
| `dialogSetTitle` | 1 | 1 | 0 |
| `dialogShow` | 1 | 1 | 0 |
| `doToast` | 1 | 1 | 0 |
| `false` | 1 | 1 | 0 |
| `fileGetData` | 1 | 1 | 0 |
| `filepickerstartpickfiles` | 1 | 1 | 0 |
| `fileRemoveData` | 1 | 1 | 0 |
| `fileSetData` | 1 | 1 | 0 |
| `fileSetFileName` | 1 | 1 | 0 |
| `fileutilcopy` | 1 | 1 | 0 |
| `fileutildelete` | 1 | 1 | 0 |
| `fileutilEndsWith` | 1 | 1 | 0 |
| `fileutilGetLastSegmentPath` | 1 | 1 | 0 |
| `fileutilisdir` | 1 | 1 | 0 |
| `fileutilisexist` | 1 | 1 | 0 |
| `fileutilisfile` | 1 | 1 | 0 |
| `fileutillength` | 1 | 1 | 0 |
| `fileutillistdir` | 1 | 1 | 0 |
| `fileutilmakedir` | 1 | 1 | 0 |
| `fileutilmove` | 1 | 1 | 0 |
| `fileutilread` | 1 | 1 | 0 |
| `fileutilStartsWith` | 1 | 1 | 0 |
| `fileutilwrite` | 1 | 1 | 0 |
| `finishActivity` | 1 | 1 | 0 |
| `firebaseAdd` | 1 | 1 | 0 |
| `firebaseauthCreateUser` | 1 | 1 | 0 |
| `firebaseauthGetCurrentUser` | 1 | 1 | 0 |
| `firebaseauthGetUid` | 1 | 1 | 0 |
| `firebaseauthIsLoggedIn` | 1 | 1 | 0 |
| `firebaseauthResetPassword` | 1 | 1 | 0 |
| `firebaseauthSignInAnonymously` | 1 | 1 | 0 |
| `firebaseauthSignInUser` | 1 | 1 | 0 |
| `firebaseauthSignOutUser` | 1 | 1 | 0 |
| `firebaseDelete` | 1 | 1 | 0 |
| `firebaseGetChildren` | 1 | 1 | 0 |
| `firebaseGetPushKey` | 1 | 1 | 0 |
| `firebasePush` | 1 | 1 | 0 |
| `firebaseStartListen` | 1 | 1 | 0 |
| `firebaseStopListen` | 1 | 1 | 0 |
| `firebasestorageDelete` | 1 | 1 | 0 |
| `firebasestorageDownloadFile` | 1 | 1 | 0 |
| `firebasestorageUploadFile` | 1 | 1 | 0 |
| `forever` | 1 | 1 | 0 |
| `getAlpha` | 1 | 1 | 0 |
| `getArg` | 1 | 1 | 0 |
| `getAtListInt` | 1 | 1 | 0 |
| `getAtListMap` | 1 | 1 | 0 |
| `getAtListStr` | 1 | 1 | 0 |
| `getChecked` | 1 | 1 | 0 |
| `getEnable` | 1 | 1 | 0 |
| `getExternalStorageDir` | 1 | 1 | 0 |
| `getJpegRotate` | 1 | 1 | 0 |
| `getLocationX` | 1 | 1 | 0 |
| `getLocationY` | 1 | 1 | 0 |
| `getMapInList` | 1 | 1 | 0 |
| `getPackageDataDir` | 1 | 1 | 0 |
| `getPublicDir` | 1 | 1 | 0 |
| `getResStr` | 1 | 1 | 0 |
| `getRotate` | 1 | 1 | 0 |
| `getScaleX` | 1 | 1 | 0 |
| `getScaleY` | 1 | 1 | 0 |
| `getText` | 1 | 1 | 0 |
| `getTranslationX` | 1 | 1 | 0 |
| `getTranslationY` | 1 | 1 | 0 |
| `getVar` | 1 | 1 | 0 |
| `gridSetCustomViewData` | 1 | 1 | 0 |
| `gyroscopeStartListen` | 1 | 1 | 0 |
| `gyroscopeStopListen` | 1 | 1 | 0 |
| `if` | 1 | 1 | 0 |
| `ifElse` | 1 | 1 | 0 |
| `increaseInt` | 1 | 1 | 0 |
| `insertListInt` | 1 | 1 | 0 |
| `insertListMap` | 1 | 1 | 0 |
| `insertListStr` | 1 | 1 | 0 |
| `insertMapToList` | 1 | 1 | 0 |
| `intentGetString` | 1 | 1 | 0 |
| `intentPutExtra` | 1 | 1 | 0 |
| `intentSetAction` | 1 | 1 | 0 |
| `intentSetData` | 1 | 1 | 0 |
| `intentSetFlags` | 1 | 1 | 0 |
| `intentSetScreen` | 1 | 1 | 0 |
| `interstitialadCreate` | 1 | 1 | 0 |
| `interstitialadLoadAd` | 1 | 1 | 0 |
| `interstitialadShow` | 1 | 1 | 0 |
| `isDrawerOpen` | 1 | 1 | 0 |
| `listGetCheckedCount` | 1 | 1 | 0 |
| `listGetCheckedPosition` | 1 | 1 | 0 |
| `listGetCheckedPositions` | 1 | 1 | 0 |
| `listRefresh` | 1 | 1 | 0 |
| `listSetCustomViewData` | 1 | 1 | 0 |
| `listSetData` | 1 | 1 | 0 |
| `listSetItemChecked` | 1 | 1 | 0 |
| `listSmoothScrollTo` | 1 | 1 | 0 |
| `locationManagerRemoveUpdates` | 1 | 1 | 0 |
| `locationManagerRequestLocationUpdates` | 1 | 1 | 0 |
| `mapContainKey` | 1 | 1 | 0 |
| `mapCreateNew` | 1 | 1 | 0 |
| `mapGet` | 1 | 1 | 0 |
| `mapGetAllKeys` | 1 | 1 | 0 |
| `mapIsEmpty` | 1 | 1 | 0 |
| `mapPut` | 1 | 1 | 0 |
| `mapRemoveKey` | 1 | 1 | 0 |
| `mapViewAddMarker` | 1 | 1 | 0 |
| `mapViewMoveCamera` | 1 | 1 | 0 |
| `mapViewSetMapType` | 1 | 1 | 0 |
| `mapViewSetMarkerColor` | 1 | 1 | 0 |
| `mapViewSetMarkerIcon` | 1 | 1 | 0 |
| `mapViewSetMarkerInfo` | 1 | 1 | 0 |
| `mapViewSetMarkerPosition` | 1 | 1 | 0 |
| `mapViewSetMarkerVisible` | 1 | 1 | 0 |
| `mapViewZoomIn` | 1 | 1 | 0 |
| `mapViewZoomOut` | 1 | 1 | 0 |
| `mapViewZoomTo` | 1 | 1 | 0 |
| `mathAbs` | 1 | 1 | 0 |
| `mathAcos` | 1 | 1 | 0 |
| `mathAsin` | 1 | 1 | 0 |
| `mathAtan` | 1 | 1 | 0 |
| `mathCeil` | 1 | 1 | 0 |
| `mathCos` | 1 | 1 | 0 |
| `mathE` | 1 | 1 | 0 |
| `mathExp` | 1 | 1 | 0 |
| `mathFloor` | 1 | 1 | 0 |
| `mathGetDip` | 1 | 1 | 0 |
| `mathGetDisplayHeight` | 1 | 1 | 0 |
| `mathGetDisplayWidth` | 1 | 1 | 0 |
| `mathLog` | 2 | 2 | 0 |
| `mathMax` | 1 | 1 | 0 |
| `mathMin` | 1 | 1 | 0 |
| `mathPi` | 1 | 1 | 0 |
| `mathPow` | 1 | 1 | 0 |
| `mathRound` | 1 | 1 | 0 |
| `mathSin` | 1 | 1 | 0 |
| `mathSqrt` | 1 | 1 | 0 |
| `mathTan` | 1 | 1 | 0 |
| `mathToDegree` | 1 | 1 | 0 |
| `mathToRadian` | 1 | 1 | 0 |
| `mediaplayerCreate` | 1 | 1 | 0 |
| `mediaplayerGetCurrent` | 1 | 1 | 0 |
| `mediaplayerGetDuration` | 1 | 1 | 0 |
| `mediaplayerIsLooping` | 1 | 1 | 0 |
| `mediaplayerIsPlaying` | 1 | 1 | 0 |
| `mediaplayerPause` | 1 | 1 | 0 |
| `mediaplayerRelease` | 1 | 1 | 0 |
| `mediaplayerReset` | 1 | 1 | 0 |
| `mediaplayerSeek` | 1 | 1 | 0 |
| `mediaplayerSetLooping` | 1 | 1 | 0 |
| `mediaplayerStart` | 1 | 1 | 0 |
| `not` | 1 | 1 | 0 |
| `objectanimatorCancel` | 1 | 1 | 0 |
| `objectanimatorIsRunning` | 1 | 1 | 0 |
| `objectanimatorSetDuration` | 1 | 1 | 0 |
| `objectanimatorSetFromTo` | 1 | 1 | 0 |
| `objectanimatorSetInterpolator` | 1 | 1 | 0 |
| `objectanimatorSetProperty` | 1 | 1 | 0 |
| `objectanimatorSetRepeatCount` | 1 | 1 | 0 |
| `objectanimatorSetRepeatMode` | 1 | 1 | 0 |
| `objectanimatorSetTarget` | 1 | 1 | 0 |
| `objectanimatorSetValue` | 1 | 1 | 0 |
| `objectanimatorStart` | 1 | 1 | 0 |
| `openDrawer` | 1 | 1 | 0 |
| `pagerSetCustomViewData` | 1 | 1 | 0 |
| `progressBarSetIndeterminate` | 1 | 1 | 0 |
| `random` | 1 | 1 | 0 |
| `recyclerSetCustomViewData` | 1 | 1 | 0 |
| `repeat` | 1 | 1 | 0 |
| `requestFocus` | 1 | 1 | 0 |
| `requestnetworkSetHeaders` | 1 | 1 | 0 |
| `requestnetworkSetParams` | 1 | 1 | 0 |
| `requestnetworkStartRequestNetwork` | 1 | 1 | 0 |
| `resizeBitmapFileRetainRatio` | 1 | 1 | 0 |
| `resizeBitmapFileToCircle` | 1 | 1 | 0 |
| `resizeBitmapFileToSquare` | 1 | 1 | 0 |
| `resizeBitmapFileWithRoundedBorder` | 1 | 1 | 0 |
| `rotateBitmapFile` | 1 | 1 | 0 |
| `scaleBitmapFile` | 1 | 1 | 0 |
| `seekBarGetMax` | 1 | 1 | 0 |
| `seekBarGetProgress` | 1 | 1 | 0 |
| `seekBarSetMax` | 1 | 1 | 0 |
| `seekBarSetProgress` | 1 | 1 | 0 |
| `setAlpha` | 1 | 1 | 0 |
| `setBgColor` | 1 | 1 | 0 |
| `setBgResource` | 1 | 1 | 0 |
| `setBitmapFileBrightness` | 1 | 1 | 0 |
| `setBitmapFileColorFilter` | 1 | 1 | 0 |
| `setBitmapFileContrast` | 1 | 1 | 0 |
| `setChecked` | 1 | 1 | 0 |
| `setClickable` | 1 | 1 | 0 |
| `setColorFilter` | 1 | 1 | 0 |
| `setEnable` | 1 | 1 | 0 |
| `setHint` | 1 | 1 | 0 |
| `setHintTextColor` | 1 | 1 | 0 |
| `setImage` | 1 | 1 | 0 |
| `setImageFilePath` | 1 | 1 | 0 |
| `setImageUrl` | 1 | 1 | 0 |
| `setListMap` | 1 | 1 | 0 |
| `setRotate` | 1 | 1 | 0 |
| `setScaleX` | 1 | 1 | 0 |
| `setScaleY` | 1 | 1 | 0 |
| `setText` | 1 | 1 | 0 |
| `setTextColor` | 1 | 1 | 0 |
| `setThumbResource` | 1 | 1 | 0 |
| `setTitle` | 1 | 1 | 0 |
| `setTrackResource` | 1 | 1 | 0 |
| `setTranslationX` | 1 | 1 | 0 |
| `setTranslationY` | 1 | 1 | 0 |
| `setTypeface` | 1 | 1 | 0 |
| `setVisible` | 1 | 1 | 0 |
| `skewBitmapFile` | 1 | 1 | 0 |
| `soundpoolCreate` | 1 | 1 | 0 |
| `soundpoolLoad` | 1 | 1 | 0 |
| `soundpoolStreamPlay` | 1 | 1 | 0 |
| `soundpoolStreamStop` | 1 | 1 | 0 |
| `speechToTextShutdown` | 1 | 1 | 0 |
| `speechToTextStartListening` | 1 | 1 | 0 |
| `speechToTextStopListening` | 1 | 1 | 0 |
| `spnGetSelection` | 1 | 1 | 0 |
| `spnRefresh` | 1 | 1 | 0 |
| `spnSetCustomViewData` | 1 | 1 | 0 |
| `spnSetData` | 1 | 1 | 0 |
| `spnSetSelection` | 1 | 1 | 0 |
| `startActivity` | 1 | 1 | 0 |
| `stringContains` | 1 | 1 | 0 |
| `stringEquals` | 1 | 1 | 0 |
| `stringIndex` | 1 | 1 | 0 |
| `stringJoin` | 1 | 1 | 0 |
| `stringLastIndex` | 1 | 1 | 0 |
| `stringLength` | 1 | 1 | 0 |
| `stringReplace` | 1 | 1 | 0 |
| `stringReplaceAll` | 1 | 1 | 0 |
| `stringReplaceFirst` | 1 | 1 | 0 |
| `stringSub` | 1 | 1 | 0 |
| `strToListMap` | 1 | 1 | 0 |
| `strToMap` | 1 | 1 | 0 |
| `textToSpeechIsSpeaking` | 1 | 1 | 0 |
| `textToSpeechSetPitch` | 1 | 1 | 0 |
| `textToSpeechSetSpeechRate` | 1 | 1 | 0 |
| `textToSpeechShutdown` | 1 | 1 | 0 |
| `textToSpeechSpeak` | 1 | 1 | 0 |
| `textToSpeechStop` | 1 | 1 | 0 |
| `timerAfter` | 1 | 1 | 0 |
| `timerCancel` | 1 | 1 | 0 |
| `timerEvery` | 1 | 1 | 0 |
| `toLowerCase` | 1 | 1 | 0 |
| `toNumber` | 1 | 1 | 0 |
| `toString` | 1 | 1 | 0 |
| `toStringFormat` | 1 | 1 | 0 |
| `toStringWithDecimal` | 1 | 1 | 0 |
| `toUpperCase` | 1 | 1 | 0 |
| `trim` | 1 | 1 | 0 |
| `true` | 1 | 1 | 0 |
| `vibratorAction` | 1 | 1 | 0 |
| `viewOnClick` | 1 | 1 | 0 |
| `webViewCanGoBack` | 1 | 1 | 0 |
| `webViewCanGoForward` | 1 | 1 | 0 |
| `webViewClearCache` | 1 | 1 | 0 |
| `webViewClearHistory` | 1 | 1 | 0 |
| `webViewGetUrl` | 1 | 1 | 0 |
| `webViewGoBack` | 1 | 1 | 0 |
| `webViewGoForward` | 1 | 1 | 0 |
| `webViewLoadUrl` | 1 | 1 | 0 |
| `webViewSetCacheMode` | 1 | 1 | 0 |
| `webViewStopLoading` | 1 | 1 | 0 |
| `webViewZoomIn` | 1 | 1 | 0 |
| `webViewZoomOut` | 1 | 1 | 0 |

## Source-only backlog

- `addSourceDirectly`

## Full matrix

| # | Opcode | Importer | Executor/reporter | Operation mapper | Status |
| ---: | --- | :---: | :---: | :---: | --- |
| 1 | `definedFunc` | yes | yes | no | importer+executor |
| 2 | `getArg` | no | yes | no | executor/reporter-only |
| 3 | `getVar` | no | yes | no | executor/reporter-only |
| 4 | `getResStr` | no | yes | no | executor/reporter-only |
| 5 | `increaseInt` | yes | no | no | importer-only |
| 6 | `decreaseInt` | yes | no | no | importer-only |
| 7 | `mapCreateNew` | yes | no | no | importer-only |
| 8 | `mapPut` | yes | no | no | importer-only |
| 9 | `mapGet` | no | yes | no | executor/reporter-only |
| 10 | `mapContainKey` | no | yes | no | executor/reporter-only |
| 11 | `mapRemoveKey` | yes | no | no | importer-only |
| 12 | `mapIsEmpty` | no | yes | no | executor/reporter-only |
| 13 | `mapGetAllKeys` | yes | no | no | importer-only |
| 14 | `addListInt` | yes | no | no | importer-only |
| 15 | `insertListInt` | yes | no | no | importer-only |
| 16 | `getAtListInt` | no | yes | no | executor/reporter-only |
| 17 | `insertListStr` | yes | no | no | importer-only |
| 18 | `getAtListStr` | no | yes | no | executor/reporter-only |
| 19 | `addListMap` | yes | no | no | importer-only |
| 20 | `insertListMap` | yes | no | no | importer-only |
| 21 | `getAtListMap` | no | yes | no | executor/reporter-only |
| 22 | `setListMap` | yes | no | no | importer-only |
| 23 | `containListMap` | no | yes | no | executor/reporter-only |
| 24 | `insertMapToList` | yes | no | no | importer-only |
| 25 | `getMapInList` | yes | no | no | importer-only |
| 26 | `deleteList` | yes | no | no | importer-only |
| 27 | `forever` | yes | yes | no | importer+executor |
| 28 | `repeat` | yes | yes | no | importer+executor |
| 29 | `if` | yes | no | no | importer-only |
| 30 | `ifElse` | yes | no | no | importer-only |
| 31 | `break` | yes | yes | no | importer+executor |
| 32 | `true` | yes | yes | no | importer+executor |
| 33 | `false` | yes | yes | no | importer+executor |
| 34 | `not` | no | yes | no | executor/reporter-only |
| 35 | `random` | no | yes | no | executor/reporter-only |
| 36 | `stringLength` | no | yes | no | executor/reporter-only |
| 37 | `stringJoin` | no | yes | no | executor/reporter-only |
| 38 | `stringIndex` | no | yes | no | executor/reporter-only |
| 39 | `stringLastIndex` | no | yes | no | executor/reporter-only |
| 40 | `stringSub` | no | yes | no | executor/reporter-only |
| 41 | `stringEquals` | no | yes | no | executor/reporter-only |
| 42 | `stringContains` | no | yes | no | executor/reporter-only |
| 43 | `stringReplace` | no | yes | no | executor/reporter-only |
| 44 | `stringReplaceFirst` | no | yes | no | executor/reporter-only |
| 45 | `stringReplaceAll` | no | yes | no | executor/reporter-only |
| 46 | `toNumber` | no | yes | no | executor/reporter-only |
| 47 | `currentTime` | no | yes | no | executor/reporter-only |
| 48 | `trim` | no | yes | no | executor/reporter-only |
| 49 | `toUpperCase` | no | yes | no | executor/reporter-only |
| 50 | `toLowerCase` | no | yes | no | executor/reporter-only |
| 51 | `toString` | no | yes | no | executor/reporter-only |
| 52 | `toStringWithDecimal` | no | yes | no | executor/reporter-only |
| 53 | `toStringFormat` | no | yes | no | executor/reporter-only |
| 54 | `addSourceDirectly` | no | no | no | source-only |
| 55 | `strToMap` | yes | no | no | importer-only |
| 56 | `strToListMap` | yes | no | no | importer-only |
| 57 | `mathGetDip` | no | yes | no | executor/reporter-only |
| 58 | `mathGetDisplayWidth` | no | yes | no | executor/reporter-only |
| 59 | `mathGetDisplayHeight` | no | yes | no | executor/reporter-only |
| 60 | `mathPi` | no | yes | no | executor/reporter-only |
| 61 | `mathE` | no | yes | no | executor/reporter-only |
| 62 | `mathPow` | no | yes | no | executor/reporter-only |
| 63 | `mathMin` | no | yes | no | executor/reporter-only |
| 64 | `mathMax` | no | yes | no | executor/reporter-only |
| 65 | `mathSqrt` | no | yes | no | executor/reporter-only |
| 66 | `mathAbs` | no | yes | no | executor/reporter-only |
| 67 | `mathRound` | no | yes | no | executor/reporter-only |
| 68 | `mathCeil` | no | yes | no | executor/reporter-only |
| 69 | `mathFloor` | no | yes | no | executor/reporter-only |
| 70 | `mathSin` | no | yes | no | executor/reporter-only |
| 71 | `mathCos` | no | yes | no | executor/reporter-only |
| 72 | `mathTan` | no | yes | no | executor/reporter-only |
| 73 | `mathAsin` | no | yes | no | executor/reporter-only |
| 74 | `mathAcos` | no | yes | no | executor/reporter-only |
| 75 | `mathAtan` | no | yes | no | executor/reporter-only |
| 76 | `mathExp` | no | yes | no | executor/reporter-only |
| 77 | `mathLog` | no | yes | no | executor/reporter-only |
| 78 | `mathLog10` | no | yes | no | executor/reporter-only |
| 79 | `mathToRadian` | no | yes | no | executor/reporter-only |
| 80 | `mathToDegree` | no | yes | no | executor/reporter-only |
| 81 | `viewOnClick` | yes | no | no | importer-only |
| 82 | `isDrawerOpen` | no | yes | no | executor/reporter-only |
| 83 | `openDrawer` | yes | no | no | importer-only |
| 84 | `closeDrawer` | yes | no | no | importer-only |
| 85 | `setEnable` | yes | no | no | importer-only |
| 86 | `getEnable` | no | yes | no | executor/reporter-only |
| 87 | `setText` | yes | no | no | importer-only |
| 88 | `setTypeface` | yes | no | no | importer-only |
| 89 | `getText` | no | yes | no | executor/reporter-only |
| 90 | `setBgColor` | yes | no | no | importer-only |
| 91 | `setBgResource` | yes | no | no | importer-only |
| 92 | `setTextColor` | yes | no | no | importer-only |
| 93 | `setImage` | yes | no | no | importer-only |
| 94 | `setColorFilter` | yes | no | no | importer-only |
| 95 | `requestFocus` | yes | no | no | importer-only |
| 96 | `doToast` | yes | no | no | importer-only |
| 97 | `copyToClipboard` | yes | no | no | importer-only |
| 98 | `setTitle` | yes | no | no | importer-only |
| 99 | `intentSetAction` | yes | no | no | importer-only |
| 100 | `intentSetData` | yes | no | no | importer-only |
| 101 | `intentSetScreen` | yes | no | no | importer-only |
| 102 | `intentPutExtra` | yes | no | no | importer-only |
| 103 | `intentSetFlags` | yes | no | no | importer-only |
| 104 | `intentGetString` | no | yes | no | executor/reporter-only |
| 105 | `startActivity` | yes | no | no | importer-only |
| 106 | `finishActivity` | yes | no | no | importer-only |
| 107 | `fileSetFileName` | yes | no | no | importer-only |
| 108 | `fileGetData` | no | yes | no | executor/reporter-only |
| 109 | `fileSetData` | yes | no | no | importer-only |
| 110 | `fileRemoveData` | yes | no | no | importer-only |
| 111 | `calendarGetNow` | yes | no | no | importer-only |
| 112 | `calendarAdd` | yes | no | no | importer-only |
| 113 | `calendarSet` | yes | no | no | importer-only |
| 114 | `calendarFormat` | yes | yes | no | importer+executor |
| 115 | `calendarDiff` | yes | yes | no | importer+executor |
| 116 | `calendarGetTime` | yes | yes | no | importer+executor |
| 117 | `calendarSetTime` | yes | no | no | importer-only |
| 118 | `setVisible` | yes | no | no | importer-only |
| 119 | `setClickable` | yes | no | no | importer-only |
| 120 | `setRotate` | yes | no | no | importer-only |
| 121 | `getRotate` | no | yes | no | executor/reporter-only |
| 122 | `setAlpha` | yes | no | no | importer-only |
| 123 | `getAlpha` | no | yes | no | executor/reporter-only |
| 124 | `setTranslationX` | yes | no | no | importer-only |
| 125 | `getTranslationX` | no | yes | no | executor/reporter-only |
| 126 | `setTranslationY` | yes | no | no | importer-only |
| 127 | `getTranslationY` | no | yes | no | executor/reporter-only |
| 128 | `setScaleX` | yes | no | no | importer-only |
| 129 | `getScaleX` | no | yes | no | executor/reporter-only |
| 130 | `setScaleY` | yes | no | no | importer-only |
| 131 | `getScaleY` | no | yes | no | executor/reporter-only |
| 132 | `getLocationX` | no | yes | no | executor/reporter-only |
| 133 | `getLocationY` | no | yes | no | executor/reporter-only |
| 134 | `setChecked` | yes | no | no | importer-only |
| 135 | `getChecked` | no | yes | no | executor/reporter-only |
| 136 | `listSetData` | yes | no | no | importer-only |
| 137 | `listSetCustomViewData` | yes | no | no | importer-only |
| 138 | `recyclerSetCustomViewData` | yes | no | no | importer-only |
| 139 | `spnSetCustomViewData` | yes | no | no | importer-only |
| 140 | `pagerSetCustomViewData` | yes | no | no | importer-only |
| 141 | `gridSetCustomViewData` | yes | no | no | importer-only |
| 142 | `listRefresh` | yes | no | no | importer-only |
| 143 | `listSetItemChecked` | yes | no | no | importer-only |
| 144 | `listGetCheckedPosition` | no | yes | no | executor/reporter-only |
| 145 | `listGetCheckedPositions` | yes | no | no | importer-only |
| 146 | `listGetCheckedCount` | no | yes | no | executor/reporter-only |
| 147 | `listSmoothScrollTo` | yes | no | no | importer-only |
| 148 | `spnSetData` | yes | no | no | importer-only |
| 149 | `spnRefresh` | yes | no | no | importer-only |
| 150 | `spnSetSelection` | yes | no | no | importer-only |
| 151 | `spnGetSelection` | no | yes | no | executor/reporter-only |
| 152 | `webViewLoadUrl` | yes | no | no | importer-only |
| 153 | `webViewGetUrl` | no | yes | no | executor/reporter-only |
| 154 | `webViewSetCacheMode` | yes | no | no | importer-only |
| 155 | `webViewCanGoBack` | no | yes | no | executor/reporter-only |
| 156 | `webViewCanGoForward` | no | yes | no | executor/reporter-only |
| 157 | `webViewGoBack` | yes | no | no | importer-only |
| 158 | `webViewGoForward` | yes | no | no | importer-only |
| 159 | `webViewClearCache` | yes | no | no | importer-only |
| 160 | `webViewClearHistory` | yes | no | no | importer-only |
| 161 | `webViewStopLoading` | yes | no | no | importer-only |
| 162 | `webViewZoomIn` | yes | no | no | importer-only |
| 163 | `webViewZoomOut` | yes | no | no | importer-only |
| 164 | `calendarViewGetDate` | no | yes | no | executor/reporter-only |
| 165 | `calendarViewSetDate` | yes | no | no | importer-only |
| 166 | `calendarViewSetMinDate` | yes | no | no | importer-only |
| 167 | `calnedarViewSetMaxDate` | yes | no | no | importer-only |
| 168 | `adViewLoadAd` | yes | no | no | importer-only |
| 169 | `mapViewSetMapType` | yes | no | no | importer-only |
| 170 | `mapViewMoveCamera` | yes | no | no | importer-only |
| 171 | `mapViewZoomTo` | yes | no | no | importer-only |
| 172 | `mapViewZoomIn` | yes | no | no | importer-only |
| 173 | `mapViewZoomOut` | yes | no | no | importer-only |
| 174 | `mapViewAddMarker` | yes | no | no | importer-only |
| 175 | `mapViewSetMarkerInfo` | yes | no | no | importer-only |
| 176 | `mapViewSetMarkerPosition` | yes | no | no | importer-only |
| 177 | `mapViewSetMarkerColor` | yes | no | no | importer-only |
| 178 | `mapViewSetMarkerIcon` | yes | no | no | importer-only |
| 179 | `mapViewSetMarkerVisible` | yes | no | no | importer-only |
| 180 | `vibratorAction` | yes | no | no | importer-only |
| 181 | `timerAfter` | yes | no | no | importer-only |
| 182 | `timerEvery` | yes | no | no | importer-only |
| 183 | `timerCancel` | yes | no | no | importer-only |
| 184 | `firebaseAdd` | yes | no | no | importer-only |
| 185 | `firebasePush` | yes | no | no | importer-only |
| 186 | `firebaseGetPushKey` | no | yes | no | executor/reporter-only |
| 187 | `firebaseDelete` | yes | no | no | importer-only |
| 188 | `firebaseGetChildren` | yes | no | no | importer-only |
| 189 | `firebaseauthCreateUser` | yes | no | no | importer-only |
| 190 | `firebaseauthSignInUser` | yes | no | no | importer-only |
| 191 | `firebaseauthSignInAnonymously` | yes | no | no | importer-only |
| 192 | `firebaseauthIsLoggedIn` | no | yes | no | executor/reporter-only |
| 193 | `firebaseauthGetCurrentUser` | no | yes | no | executor/reporter-only |
| 194 | `firebaseauthGetUid` | no | yes | no | executor/reporter-only |
| 195 | `firebaseauthResetPassword` | yes | no | no | importer-only |
| 196 | `firebaseauthSignOutUser` | yes | no | no | importer-only |
| 197 | `firebaseStartListen` | yes | no | no | importer-only |
| 198 | `firebaseStopListen` | yes | no | no | importer-only |
| 199 | `gyroscopeStartListen` | yes | no | no | importer-only |
| 200 | `gyroscopeStopListen` | yes | no | no | importer-only |
| 201 | `dialogSetTitle` | yes | no | no | importer-only |
| 202 | `dialogSetMessage` | yes | no | no | importer-only |
| 203 | `dialogShow` | yes | no | no | importer-only |
| 204 | `dialogOkButton` | yes | no | no | importer-only |
| 205 | `dialogCancelButton` | yes | no | no | importer-only |
| 206 | `dialogNeutralButton` | yes | no | no | importer-only |
| 207 | `mediaplayerCreate` | yes | no | no | importer-only |
| 208 | `mediaplayerStart` | yes | no | no | importer-only |
| 209 | `mediaplayerPause` | yes | no | no | importer-only |
| 210 | `mediaplayerSeek` | yes | no | no | importer-only |
| 211 | `mediaplayerGetCurrent` | no | yes | no | executor/reporter-only |
| 212 | `mediaplayerGetDuration` | no | yes | no | executor/reporter-only |
| 213 | `mediaplayerReset` | yes | no | no | importer-only |
| 214 | `mediaplayerRelease` | yes | no | no | importer-only |
| 215 | `mediaplayerIsPlaying` | no | yes | no | executor/reporter-only |
| 216 | `mediaplayerSetLooping` | yes | no | no | importer-only |
| 217 | `mediaplayerIsLooping` | no | yes | no | executor/reporter-only |
| 218 | `soundpoolCreate` | yes | no | no | importer-only |
| 219 | `soundpoolLoad` | yes | no | no | importer-only |
| 220 | `soundpoolStreamPlay` | yes | no | no | importer-only |
| 221 | `soundpoolStreamStop` | yes | no | no | importer-only |
| 222 | `setThumbResource` | yes | no | no | importer-only |
| 223 | `setTrackResource` | yes | no | no | importer-only |
| 224 | `seekBarSetProgress` | yes | no | no | importer-only |
| 225 | `seekBarGetProgress` | no | yes | no | executor/reporter-only |
| 226 | `seekBarSetMax` | yes | no | no | importer-only |
| 227 | `seekBarGetMax` | no | yes | no | executor/reporter-only |
| 228 | `objectanimatorSetTarget` | yes | no | no | importer-only |
| 229 | `objectanimatorSetProperty` | yes | no | no | importer-only |
| 230 | `objectanimatorSetValue` | yes | no | no | importer-only |
| 231 | `objectanimatorSetFromTo` | yes | no | no | importer-only |
| 232 | `objectanimatorSetDuration` | yes | no | no | importer-only |
| 233 | `objectanimatorSetRepeatMode` | yes | no | no | importer-only |
| 234 | `objectanimatorSetRepeatCount` | yes | no | no | importer-only |
| 235 | `objectanimatorSetInterpolator` | yes | no | no | importer-only |
| 236 | `objectanimatorStart` | yes | no | no | importer-only |
| 237 | `objectanimatorCancel` | yes | no | no | importer-only |
| 238 | `objectanimatorIsRunning` | no | yes | no | executor/reporter-only |
| 239 | `interstitialadCreate` | yes | no | no | importer-only |
| 240 | `interstitialadLoadAd` | yes | no | no | importer-only |
| 241 | `interstitialadShow` | yes | no | no | importer-only |
| 242 | `firebasestorageUploadFile` | yes | no | no | importer-only |
| 243 | `firebasestorageDownloadFile` | yes | no | no | importer-only |
| 244 | `firebasestorageDelete` | yes | no | no | importer-only |
| 245 | `fileutilread` | no | yes | no | executor/reporter-only |
| 246 | `fileutilwrite` | yes | no | no | importer-only |
| 247 | `fileutilcopy` | yes | no | no | importer-only |
| 248 | `fileutilmove` | yes | no | no | importer-only |
| 249 | `fileutildelete` | yes | no | no | importer-only |
| 250 | `fileutilisexist` | no | yes | no | executor/reporter-only |
| 251 | `fileutilmakedir` | yes | no | no | importer-only |
| 252 | `fileutillistdir` | yes | no | no | importer-only |
| 253 | `fileutilisdir` | no | yes | no | executor/reporter-only |
| 254 | `fileutilisfile` | no | yes | no | executor/reporter-only |
| 255 | `fileutillength` | no | yes | no | executor/reporter-only |
| 256 | `fileutilStartsWith` | no | yes | no | executor/reporter-only |
| 257 | `fileutilEndsWith` | no | yes | no | executor/reporter-only |
| 258 | `fileutilGetLastSegmentPath` | no | yes | no | executor/reporter-only |
| 259 | `getExternalStorageDir` | no | yes | no | executor/reporter-only |
| 260 | `getPackageDataDir` | no | yes | no | executor/reporter-only |
| 261 | `getPublicDir` | no | yes | no | executor/reporter-only |
| 262 | `resizeBitmapFileRetainRatio` | yes | no | no | importer-only |
| 263 | `resizeBitmapFileToSquare` | yes | no | no | importer-only |
| 264 | `resizeBitmapFileToCircle` | yes | no | no | importer-only |
| 265 | `resizeBitmapFileWithRoundedBorder` | yes | no | no | importer-only |
| 266 | `cropBitmapFileFromCenter` | yes | no | no | importer-only |
| 267 | `rotateBitmapFile` | yes | no | no | importer-only |
| 268 | `scaleBitmapFile` | yes | no | no | importer-only |
| 269 | `skewBitmapFile` | yes | no | no | importer-only |
| 270 | `setBitmapFileColorFilter` | yes | no | no | importer-only |
| 271 | `setBitmapFileBrightness` | yes | no | no | importer-only |
| 272 | `setBitmapFileContrast` | yes | no | no | importer-only |
| 273 | `getJpegRotate` | no | yes | no | executor/reporter-only |
| 274 | `filepickerstartpickfiles` | yes | no | no | importer-only |
| 275 | `camerastarttakepicture` | yes | no | no | importer-only |
| 276 | `setImageFilePath` | yes | no | no | importer-only |
| 277 | `setImageUrl` | yes | no | no | importer-only |
| 278 | `setHint` | yes | no | no | importer-only |
| 279 | `setHintTextColor` | yes | no | no | importer-only |
| 280 | `requestnetworkSetParams` | yes | no | no | importer-only |
| 281 | `requestnetworkSetHeaders` | yes | no | no | importer-only |
| 282 | `requestnetworkStartRequestNetwork` | yes | no | no | importer-only |
| 283 | `progressBarSetIndeterminate` | yes | no | no | importer-only |
| 284 | `textToSpeechSetPitch` | yes | no | no | importer-only |
| 285 | `textToSpeechSetSpeechRate` | yes | no | no | importer-only |
| 286 | `textToSpeechSpeak` | yes | no | no | importer-only |
| 287 | `textToSpeechIsSpeaking` | no | yes | no | executor/reporter-only |
| 288 | `textToSpeechStop` | yes | no | no | importer-only |
| 289 | `textToSpeechShutdown` | yes | no | no | importer-only |
| 290 | `speechToTextStartListening` | yes | no | no | importer-only |
| 291 | `speechToTextStopListening` | yes | no | no | importer-only |
| 292 | `speechToTextShutdown` | yes | no | no | importer-only |
| 293 | `bluetoothConnectReadyConnection` | yes | no | no | importer-only |
| 294 | `bluetoothConnectReadyConnectionToUuid` | yes | no | no | importer-only |
| 295 | `bluetoothConnectStartConnection` | yes | no | no | importer-only |
| 296 | `bluetoothConnectStartConnectionToUuid` | yes | no | no | importer-only |
| 297 | `bluetoothConnectStopConnection` | yes | no | no | importer-only |
| 298 | `bluetoothConnectSendData` | yes | no | no | importer-only |
| 299 | `bluetoothConnectIsBluetoothEnabled` | no | yes | no | executor/reporter-only |
| 300 | `bluetoothConnectIsBluetoothActivated` | no | yes | no | executor/reporter-only |
| 301 | `bluetoothConnectActivateBluetooth` | yes | no | no | importer-only |
| 302 | `bluetoothConnectGetPairedDevices` | yes | no | no | importer-only |
| 303 | `bluetoothConnectGetRandomUuid` | no | yes | no | executor/reporter-only |
| 304 | `locationManagerRequestLocationUpdates` | yes | no | no | importer-only |
| 305 | `locationManagerRemoveUpdates` | yes | no | no | importer-only |
## Disposition of the remaining source-only opcode

`addSourceDirectly` is the only legacy opcode without a target importer or executor path. It injects arbitrary Java source into generated projects and is therefore intentionally blocked by the Creator Runtime security boundary. It must remain `R0_UNSUPPORTED`; implementing it would reintroduce source execution and violate the target branch's no-fallback/no-arbitrary-code architecture. No runtime transfer is appropriate for this opcode.

## Audit conclusion

The audit found no safe, executable opcode gap that can be transferred from local `main` without duplicating or weakening the target architecture. The target branch already covers the remaining opcode families through typed importer paths, runtime service calls, list/map mutations, reporter evaluation, and component-scoped services. The only uncovered legacy opcode is the deliberately blocked arbitrary-source escape hatch described above.
