# Creator Runtime inventory

## Repository baseline

## [32mcreator-runtime[m...[31morigin/creator-runtime[m
[31m??[m audit/creator_runtime_acceptance_contract_v4.md
[31m??[m audit/creator_runtime_inventory_v4.md
[33m4c2639949[m fix: rebuild creator runtime editor boundary

## Manifest activities and launcher

46:        <activity
48:            android:exported="false"
55:            android:exported="false"
62:        <activity
65:        <activity
69:        <activity android:name="com.besome.sketch.help.LicenseActivity" />
70:        <activity android:name="com.besome.sketch.help.ProgramInfoActivity" />
71:        <activity
74:        <activity android:name="com.besome.sketch.help.SystemInfoActivity" />
75:        <activity
81:        <activity
84:        <activity
87:        <activity
91:        <activity android:name="com.besome.sketch.tools.CompileLogActivity" />
92:        <activity android:name="com.besome.sketch.tools.CollectErrorActivity" />
93:        <activity
96:        <activity
99:        <activity
103:            android:exported="true"
106:                <action android:name="android.intent.action.MAIN" />
107:                <category android:name="android.intent.category.LAUNCHER" />
110:        <activity
113:            android:exported="false"
115:        <activity
118:            android:exported="false"
120:        <activity
123:            android:exported="true"
141:        <activity
144:        <activity
147:        <activity
150:        <activity
153:        <activity
156:        <activity
159:        <activity
162:        <activity
165:        <activity
168:        <activity
172:        <activity
176:        <activity
181:        <activity
185:        <activity
190:        <activity
195:        <activity
200:        <activity
205:        <activity
211:        <activity
217:        <activity
221:        <activity
226:        <activity
231:        <activity
234:        <activity
237:        <activity
240:        <activity
243:        <activity
246:        <activity
249:        <activity
252:        <activity
256:        <activity
259:        <activity
262:        <activity
265:        <activity
268:        <activity
271:        <activity
274:        <activity
277:        <activity
281:        <activity
284:        <activity
287:        <activity
290:        <activity
293:        <activity
296:        <activity
299:        <activity android:name="mod.jbk.editor.manage.library.ExcludeBuiltInLibrariesActivity" />
300:        <activity
305:        <activity
309:        <activity
314:        <activity android:name="pro.sketchware.activities.settings.SettingsActivity" />
316:        <activity android:name="com.sketchware.ai.ui.settings.AISettingsActivity"
317:            android:exported="false"
320:        <activity android:name="com.sketchware.ai.ui.settings.ProviderDetailActivity"
321:            android:exported="false"
324:        <activity android:name="pro.sketchware.activities.resourceseditor.ResourcesEditorActivity" />
326:        <activity
330:        <activity
334:        <activity
338:        <activity
343:        <activity
348:        <activity
352:        <activity
356:        <activity
366:            android:exported="false">

## All Creator-related Java/Kotlin files

app/src/main/java/com/sketchware/ai/tools/creator/ActivityListTool.java
app/src/main/java/com/sketchware/ai/tools/creator/CreatorRuntimeTool.java
app/src/main/java/com/sketchware/ai/tools/resource/IconCreatorTool.java
app/src/main/java/kellinwood/security/zipsigner/optional/CertCreator.java
app/src/main/java/mod/hilal/saif/activities/tools/BlocksManagerCreatorActivity.java
app/src/main/java/pro/sketchware/activities/iconcreator/IconCreatorActivity.java
app/src/main/java/pro/sketchware/creator/CreatorHomeActivity.java
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java
app/src/main/java/pro/sketchware/creator/CreatorShakeDetector.java
app/src/main/java/pro/sketchware/creator/CreatorShakeRecovery.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorAnimatorService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorApplyResult.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorBitmapService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorBluetoothService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorCalendarService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorCameraService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorCompatibilityAnalyzer.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorCompatibilityReport.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorCompatibilityTier.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorDatePickerService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorDeviceMetricsService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorDialogService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorDrawerService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorEntryControl.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorEventBinding.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorFilePickerService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorFileService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseAuthPhoneService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseAuthService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseCloudMessageService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseDatabaseService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseGoogleLoginService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseStorageService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorFragmentAdapterService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorGyroscopeService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorIntentService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorInterstitialAdService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyComponentCapabilityMatrix.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyViewCapabilityMatrix.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyViewImporter.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorLocationService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorMapService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorMediaService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorNetworkService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorNotificationService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationReducer.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationValidator.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocument.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectOperation.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRevisionStore.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRewardedAdService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeBlock.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeCapability.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeCompatibilityInspector.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeComponentServiceMatrix.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeCondition.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeDefaults.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEngine.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEnvironment.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEvent.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEventLog.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExpression.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimePermissionBridge.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeProjectStore.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeResourceResolver.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeResourceValues.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeServiceArguments.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeServiceCatalog.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeServiceDispatcher.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeServices.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeSession.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeWidgetCatalog.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorScreen.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorSpeechToTextService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorStorageService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorTextToSpeechService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorTimePickerService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorTimerService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorUiService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorValidationResult.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorVibratorService.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorWidget.java
app/src/main/java/pro/sketchware/creator/runtime/CreatorWidgetQueryService.java
app/src/main/java/pro/sketchware/fragments/settings/events/creator/EventsManagerCreatorFragment.java
app/src/main/java/pro/sketchware/widgets/WidgetsCreatorManager.java

## Creator-related resources

app/src/main/res/color/design_error.xml
app/src/main/res/drawable-xhdpi/activity_0000.png
app/src/main/res/drawable-xhdpi/activity_0001.png
app/src/main/res/drawable-xhdpi/activity_0010.png
app/src/main/res/drawable-xhdpi/activity_0011.png
app/src/main/res/drawable-xhdpi/activity_0100.png
app/src/main/res/drawable-xhdpi/activity_0101.png
app/src/main/res/drawable-xhdpi/activity_0110.png
app/src/main/res/drawable-xhdpi/activity_0111.png
app/src/main/res/drawable-xhdpi/activity_1000.png
app/src/main/res/drawable-xhdpi/activity_1001.png
app/src/main/res/drawable-xhdpi/activity_1010.png
app/src/main/res/drawable-xhdpi/activity_1011.png
app/src/main/res/drawable-xhdpi/activity_1100.png
app/src/main/res/drawable-xhdpi/activity_1101.png
app/src/main/res/drawable-xhdpi/activity_1110.png
app/src/main/res/drawable-xhdpi/activity_1111.png
app/src/main/res/drawable-xhdpi/activity_preset_1.png
app/src/main/res/drawable-xhdpi/activity_preset_2.png
app/src/main/res/drawable-xhdpi/activity_preset_3.png
app/src/main/res/drawable-xhdpi/activity_preset_4.png
app/src/main/res/drawable-xhdpi/event_on_bind_custom_view_48dp.png
app/src/main/res/drawable-xhdpi/event_pattern_lock_view.png
app/src/main/res/drawable-xhdpi/ic_drawer_color_48dp.png
app/src/main/res/drawable-xhdpi/ic_keyboard_preview_48dp.png
app/src/main/res/drawable-xhdpi/item_badge_view.png
app/src/main/res/drawable-xhdpi/item_bottom_view.png
app/src/main/res/drawable-xhdpi/item_code_view.png
app/src/main/res/drawable-xhdpi/item_otp_view.png
app/src/main/res/drawable-xhdpi/item_pattern_lock_view.png
app/src/main/res/drawable-xhdpi/item_video_view.png
app/src/main/res/drawable-xhdpi/mapview_bg.png
app/src/main/res/drawable-xhdpi/new_view_pane_background_land.png
app/src/main/res/drawable-xhdpi/new_view_pane_background_port.png
app/src/main/res/drawable-xhdpi/project_icon_bg.png
app/src/main/res/drawable-xhdpi/view_bg.9.png
app/src/main/res/drawable-xhdpi/view_pane_background_land.png
app/src/main/res/drawable-xhdpi/view_pane_background_port.png
app/src/main/res/drawable-xhdpi/web_view_bg.png
app/src/main/res/drawable-xhdpi/widget_badge_view.png
app/src/main/res/drawable-xhdpi/widget_bottom_view.png
app/src/main/res/drawable-xhdpi/widget_calendarview.png
app/src/main/res/drawable-xhdpi/widget_cardview.png
app/src/main/res/drawable-xhdpi/widget_code_view.png
app/src/main/res/drawable-xhdpi/widget_horizon_scrollview.png
app/src/main/res/drawable-xhdpi/widget_horizontalscrollview.png
app/src/main/res/drawable-xhdpi/widget_image_view.png
app/src/main/res/drawable-xhdpi/widget_list_view.png
app/src/main/res/drawable-xhdpi/widget_pattern_lock_view.png
app/src/main/res/drawable-xhdpi/widget_scrollview.png
app/src/main/res/drawable-xhdpi/widget_text_view.png
app/src/main/res/drawable-xhdpi/widget_view_pager.png
app/src/main/res/drawable-xhdpi/widget_web_view.png
app/src/main/res/drawable/bg_event_type_activity.xml
app/src/main/res/drawable/bg_event_type_drawer_view.xml
app/src/main/res/drawable/bg_event_type_view.xml
app/src/main/res/drawable/bg_view_pane.xml
app/src/main/res/drawable/design_bottom_navigation_item_background.xml
app/src/main/res/drawable/design_fab_background.xml
app/src/main/res/drawable/ic_drawer.xml
app/src/main/res/drawable/ic_mtrl_design.xml
app/src/main/res/drawable/ic_mtrl_preview.xml
app/src/main/res/drawable/ic_mtrl_preview_off.xml
app/src/main/res/drawable/ic_mtrl_view_horizontal.xml
app/src/main/res/drawable/ic_mtrl_view_relative.xml
app/src/main/res/drawable/ic_mtrl_view_vertical.xml
app/src/main/res/drawable/ic_mtrl_viewpager.xml
app/src/main/res/drawable/img_preview_dark_theme.xml
app/src/main/res/drawable/img_preview_light_theme.xml
app/src/main/res/drawable/project_item_shape_alone.xml
app/src/main/res/drawable/project_item_shape_alone_error.xml
app/src/main/res/drawable/project_item_shape_bottom.xml
app/src/main/res/drawable/project_item_shape_middle.xml
app/src/main/res/drawable/project_item_shape_top.xml
app/src/main/res/drawable/project_store_preview_toolbar_icon_bg.xml
app/src/main/res/drawable/selector_list_view.xml
app/src/main/res/drawable/selector_toggle_list_view.xml
app/src/main/res/drawable/view_switch_background.xml
app/src/main/res/drawable/view_tab_indicator.xml
app/src/main/res/layout/about_teamview.xml
app/src/main/res/layout/activity_about_app.xml
app/src/main/res/layout/activity_ai_settings.xml
app/src/main/res/layout/activity_app_settings.xml
app/src/main/res/layout/activity_blocks_manager.xml
app/src/main/res/layout/activity_blocks_manager_creator.xml
app/src/main/res/layout/activity_blocks_manager_details.xml
app/src/main/res/layout/activity_code_viewer.xml
app/src/main/res/layout/activity_creator_home.xml
app/src/main/res/layout/activity_creator_project.xml
app/src/main/res/layout/activity_icon_creator.xml
app/src/main/res/layout/activity_layout_preview.xml
app/src/main/res/layout/activity_logcatreader.xml
app/src/main/res/layout/activity_manage_custom_attribute.xml
app/src/main/res/layout/activity_oss_libraries.xml
app/src/main/res/layout/activity_settings.xml
app/src/main/res/layout/activity_system_info.xml
app/src/main/res/layout/ai_chat_drawer.xml
app/src/main/res/layout/block_customview.xml
app/src/main/res/layout/block_customview_spec.xml
app/src/main/res/layout/bottom_sheet_project_options.xml
app/src/main/res/layout/custom_view_attribute.xml
app/src/main/res/layout/design.xml
app/src/main/res/layout/design_drawer_item.xml
app/src/main/res/layout/dialog_add_custom_activity.xml
app/src/main/res/layout/dialog_project_settings.xml
app/src/main/res/layout/export_project.xml
app/src/main/res/layout/file_selector_popup_select_xml_activity_item.xml
app/src/main/res/layout/fr_logic_list_item_event_preview.xml
app/src/main/res/layout/fr_logic_list_preview_with_event_item.xml
app/src/main/res/layout/fr_manage_font_list.xml
app/src/main/res/layout/fr_manage_image_list.xml
app/src/main/res/layout/fr_manage_sound_list.xml
app/src/main/res/layout/fr_manage_view_list.xml
app/src/main/res/layout/fragment_block_selector_manager.xml
app/src/main/res/layout/fragment_events_manager.xml
app/src/main/res/layout/fragment_events_manager_creator.xml
app/src/main/res/layout/fragment_events_manager_details.xml
app/src/main/res/layout/fragment_projects_store.xml
app/src/main/res/layout/fragment_store_project_preview.xml
app/src/main/res/layout/fragment_store_project_preview_comments.xml
app/src/main/res/layout/fragment_stringfog_manager.xml
app/src/main/res/layout/logic_editor_drawer.xml
app/src/main/res/layout/main_drawer_header.xml
app/src/main/res/layout/manage_app_compat.xml
app/src/main/res/layout/manage_collection.xml
app/src/main/res/layout/manage_collection_block_list_item.xml
app/src/main/res/layout/manage_collection_more_block_list_item.xml
app/src/main/res/layout/manage_collection_popup_import_more_block_list_item.xml
app/src/main/res/layout/manage_collection_show_block.xml
app/src/main/res/layout/manage_collection_show_widget.xml
app/src/main/res/layout/manage_collection_widget_list_item.xml
app/src/main/res/layout/manage_custom_component.xml
app/src/main/res/layout/manage_custom_component_add.xml
app/src/main/res/layout/manage_custom_component_list_item.xml
app/src/main/res/layout/manage_file.xml
app/src/main/res/layout/manage_file_picker_list_item.xml
app/src/main/res/layout/manage_font.xml
app/src/main/res/layout/manage_font_add.xml
app/src/main/res/layout/manage_font_import.xml
app/src/main/res/layout/manage_font_list_item.xml
app/src/main/res/layout/manage_image.xml
app/src/main/res/layout/manage_image_add.xml
app/src/main/res/layout/manage_image_import.xml
app/src/main/res/layout/manage_image_list_item.xml
app/src/main/res/layout/manage_import_list_item.xml
app/src/main/res/layout/manage_java_item_hs.xml
app/src/main/res/layout/manage_library.xml
app/src/main/res/layout/manage_library_admob.xml
app/src/main/res/layout/manage_library_admob_app_id.xml
app/src/main/res/layout/manage_library_admob_listing.xml
app/src/main/res/layout/manage_library_admob_preview.xml
app/src/main/res/layout/manage_library_admob_set_unit.xml
app/src/main/res/layout/manage_library_admob_test_device.xml
app/src/main/res/layout/manage_library_base_item.xml
app/src/main/res/layout/manage_library_category_item.xml
app/src/main/res/layout/manage_library_exclude_builtin_libraries.xml
app/src/main/res/layout/manage_library_exclude_builtin_libraries_list_item.xml
app/src/main/res/layout/manage_library_firebase.xml
app/src/main/res/layout/manage_library_firebase_preview.xml
app/src/main/res/layout/manage_library_firebase_project_settings.xml
app/src/main/res/layout/manage_library_firebase_storage_url_settings.xml
app/src/main/res/layout/manage_library_manage_admob.xml
app/src/main/res/layout/manage_library_manage_compat.xml
app/src/main/res/layout/manage_library_manage_firebase.xml
app/src/main/res/layout/manage_library_manage_googlemap.xml
app/src/main/res/layout/manage_library_material3.xml
app/src/main/res/layout/manage_library_popup_project_list_item.xml
app/src/main/res/layout/manage_library_popup_project_selector.xml
app/src/main/res/layout/manage_library_setting_admob_adunit_add.xml
app/src/main/res/layout/manage_library_setting_admob_adunit_item.xml
app/src/main/res/layout/manage_library_setting_admob_app_id_add.xml
app/src/main/res/layout/manage_library_setting_admob_test_device_add.xml
app/src/main/res/layout/manage_library_setting_admob_test_device_item.xml
app/src/main/res/layout/manage_locallibraries.xml
app/src/main/res/layout/manage_permission.xml
app/src/main/res/layout/manage_proguard.xml
app/src/main/res/layout/manage_screen_activity_add_feature_item.xml
app/src/main/res/layout/manage_screen_activity_add_temp.xml
app/src/main/res/layout/manage_screen_activity_add_view_preset_setting.xml
app/src/main/res/layout/manage_screen_custom_view_add.xml
app/src/main/res/layout/manage_sound.xml
app/src/main/res/layout/manage_sound_add.xml
app/src/main/res/layout/manage_sound_import.xml
app/src/main/res/layout/manage_sound_list_item.xml
app/src/main/res/layout/manage_view.xml
app/src/main/res/layout/manage_view_custom_list_item.xml
app/src/main/res/layout/manage_view_list_item.xml
app/src/main/res/layout/manage_xml_command.xml
app/src/main/res/layout/manage_xml_command_add.xml
app/src/main/res/layout/menu_activity.xml
app/src/main/res/layout/myproject_button.xml
app/src/main/res/layout/myproject_color.xml
app/src/main/res/layout/myproject_setting.xml
app/src/main/res/layout/myprojects.xml
app/src/main/res/layout/myprojects_item.xml
app/src/main/res/layout/myprojects_item_special.xml
app/src/main/res/layout/pallet_customview.xml
app/src/main/res/layout/preference_activity.xml
app/src/main/res/layout/project_config_layout.xml
app/src/main/res/layout/resources_editors_activity.xml
app/src/main/res/layout/search_with_recycler_view.xml
app/src/main/res/layout/sort_project_dialog.xml
app/src/main/res/layout/src_viewer.xml
app/src/main/res/layout/view_bottom_sheet_dialog.xml
app/src/main/res/layout/view_code_editor.xml
app/src/main/res/layout/view_editor.xml
app/src/main/res/layout/view_events.xml
app/src/main/res/layout/view_item_local_lib.xml
app/src/main/res/layout/view_item_local_lib_search.xml
app/src/main/res/layout/view_item_permission.xml
app/src/main/res/layout/view_logcat_item.xml
app/src/main/res/layout/view_properties.xml
app/src/main/res/layout/view_property.xml
app/src/main/res/layout/view_store_project_item.xml
app/src/main/res/layout/view_store_project_pager_item.xml
app/src/main/res/layout/view_store_project_preview_comment.xml
app/src/main/res/layout/view_store_project_screenshot.xml
app/src/main/res/layout/view_string_editor_add.xml
app/src/main/res/layout/view_used_custom_blocks.xml
app/src/main/res/layout/widgets_creator_dialog.xml
app/src/main/res/menu/design_menu.xml
app/src/main/res/menu/design_view_menu.xml
app/src/main/res/menu/events_manager_menu.xml
app/src/main/res/menu/main_drawer_menu.xml
app/src/main/res/menu/manage_admob_menu.xml
app/src/main/res/menu/manage_collection_items_menu.xml
app/src/main/res/menu/manage_collection_menu.xml
app/src/main/res/menu/manage_custom_attribute_menu.xml
app/src/main/res/menu/manage_firebase_menu.xml
app/src/main/res/menu/manage_font_menu.xml
app/src/main/res/menu/manage_image_menu.xml
app/src/main/res/menu/manage_screen_menu.xml
app/src/main/res/menu/manage_sound_menu.xml
app/src/main/res/menu/permission_manager_menu.xml
app/src/main/res/menu/projects_fragment_menu.xml
app/src/main/res/menu/widget_creator_menu_more.xml
app/src/main/res/values/creator_runtime_strings.xml
app/src/main/res/xml/preferences_config_activity.xml

## Creator-related test files

app/src/androidTest/java/pro/sketchware/creator/CreatorRuntimeNativeWidgetTest.java
app/src/androidTest/java/pro/sketchware/creator/CreatorRuntimeNavigationTest.java
app/src/androidTest/java/pro/sketchware/creator/ProjectSettingsRuntimeRegressionTest.java
app/src/test/java/com/sketchware/ai/tools/creator/CreatorRuntimeToolSchemaTest.java
app/src/test/java/pro/sketchware/creator/CreatorShakeDetectorTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorCalendarServiceTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorCompatibilityAnalyzerTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorFileServiceTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporterTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorLegacyComponentCapabilityMatrixTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorLegacyViewCapabilityMatrixTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorLegacyViewImporterTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorNetworkServiceTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorNotificationServiceTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodecTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorRuntimeCompatibilityInspectorTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorRuntimeComponentServiceMatrixTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorRuntimeControlFlowTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorRuntimeDefaultsTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorRuntimeEngineTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutorTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorRuntimeExpressionConditionTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapperTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorRuntimePermissionBridgeTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorRuntimeResourceResolverTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorRuntimeResourceValuesTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorRuntimeServiceDispatcherTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorRuntimeWidgetCatalogTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorRuntimeWorkflowTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorStorageServiceTest.java
app/src/test/java/pro/sketchware/creator/runtime/CreatorTimerServiceTest.java
app/src/test/java/pro/sketchware/widgets/WidgetsCreatorManagerTest.java

## Lifecycle hooks in critical classes

app/src/main/java/com/besome/sketch/design/DesignActivity.java:132:public class DesignActivity extends BaseAppCompatActivity implements View.OnClickListener {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:444:                startActivity(intent);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:451:    public void finish() {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:453:        // calling finish(). Import that final snapshot before the legacy caches
app/src/main/java/com/besome/sketch/design/DesignActivity.java:467:            startActivity(liveIntent);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:469:        super.finish();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:509:                                    startActivity(launcher);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:536:        startActivity(intent);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:575:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:577:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:581:            finish();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:826:    public void onResume() {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:827:        super.onResume();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:828:        // onCreate() performs the initial runtime -> legacy projection. On later
app/src/main/java/com/besome/sketch/design/DesignActivity.java:834:            finish();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:865:    public void onSaveInstanceState(Bundle outState) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:867:        super.onSaveInstanceState(outState);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:869:            finish();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1072:        startActivity(intent);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1221:            startActivity(intent);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1610:                    activity.finish();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1675:                    activity.finish();
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:72:public final class CreatorProjectActivity extends AppCompatActivity {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:90:    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:92:        super.onCreate(savedInstanceState);
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:124:    @Override protected void onResume() {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:125:        super.onResume();
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:127:        for (MapView map : liveMapViews.values()) map.onResume();
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:133:    @Override protected void onPause() {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:135:        for (MapView map : liveMapViews.values()) map.onPause();
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:138:        super.onPause();
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:157:            finish();
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:172:            startActivity(intent);
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:174:        bindSidebar(R.id.creator_sidebar_info, v -> startActivityForResult(
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:195:        startActivity(new Intent(this, type));
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:201:            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(urlRes))));
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:216:            finish();
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:219:        startActivity(new android.content.Intent(this, CreatorProjectActivity.class)
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:221:        finish();
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:233:            startActivity(intent);
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:236:        startActivity(new Intent(this, CreatorProjectActivity.class));
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:239:    @Override protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:241:        super.onActivityResult(requestCode, resultCode, data);
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:447:            map.onPause();
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:735:            map.onCreate(null);
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:736:            map.onResume();
app/src/main/java/pro/sketchware/creator/CreatorHomeActivity.java:21:public final class CreatorHomeActivity extends AppCompatActivity {
app/src/main/java/pro/sketchware/creator/CreatorHomeActivity.java:22:    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
app/src/main/java/pro/sketchware/creator/CreatorHomeActivity.java:24:        super.onCreate(savedInstanceState);
app/src/main/java/pro/sketchware/creator/CreatorHomeActivity.java:27:        startActivity(new Intent(this, DesignActivity.class)
app/src/main/java/pro/sketchware/creator/CreatorHomeActivity.java:30:        finish();
app/src/main/java/pro/sketchware/creator/runtime/CreatorIntentService.java:73:                environment.getActivity().startActivity(intent(arguments));
app/src/main/java/pro/sketchware/creator/runtime/CreatorIntentService.java:77:                environment.getActivity().finish();
app/src/main/java/pro/sketchware/creator/runtime/CreatorIntentService.java:85:                environment.getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorIntentService.java:93:                environment.getActivity().startActivity(Intent.createChooser(share,
app/src/main/java/pro/sketchware/creator/runtime/CreatorIntentService.java:100:                environment.getActivity().startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEnvironment.java:71:        activity.startActivityForResult(intent, requestCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:467:            BlockBean startActivity = new BlockBean("2", "startActivity %s",
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:468:                    "command", "intent", "startActivity");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:469:            startActivity.parameters.add(CreatorRuntimeDefaults.EDITOR_INTENT_ID);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:471:            blocks.add(startActivity);
app/src/main/java/com/besome/sketch/editor/manage/view/AddCustomViewActivity.java:33:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/view/AddCustomViewActivity.java:34:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/view/AddCustomViewActivity.java:53:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/view/AddCustomViewActivity.java:75:            finish();
app/src/main/java/com/besome/sketch/editor/manage/view/AddCustomViewActivity.java:79:            startActivityForResult(intent, REQ_CD_PRESET_ACTIVITY);
app/src/main/java/com/besome/sketch/editor/manage/view/AddCustomViewActivity.java:81:            finish();
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:33:public class AddViewActivity extends BaseAppCompatActivity {
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:185:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:186:        super.onActivityResult(requestCode, resultCode, data);
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:196:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:197:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:202:        binding.toolbar.setNavigationOnClickListener(v -> finish());
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:241:            finish();
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:276:        finish();
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:289:        finish();
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:45:public class ManageViewActivity extends BaseAppCompatActivity implements OnClickListener, ViewPager.OnPageChangeListener {
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:193:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:194:        super.onActivityResult(requestCode, resultCode, data);
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:266:                startActivityForResult(intent, isActivitiesTab ? REQUEST_CODE_ADD_ACTIVITY : REQUEST_CODE_ADD_CUSTOM_VIEW);
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:272:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:273:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:280:            finish();
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:332:    public void onResume() {
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:333:        super.onResume();
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:335:            finish();
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:346:    public void onSaveInstanceState(Bundle newState) {
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:350:        super.onSaveInstanceState(newState);
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:367:            activity.finish();
app/src/main/java/com/besome/sketch/editor/manage/view/PresetSettingActivity.java:44:        finish();
app/src/main/java/com/besome/sketch/editor/manage/view/PresetSettingActivity.java:79:            finish();
app/src/main/java/com/besome/sketch/editor/manage/view/PresetSettingActivity.java:90:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/view/PresetSettingActivity.java:91:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:97:        startActivityForResult(intent, REQUEST_CODE_ADD_IMAGE_DIALOG);
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:105:        startActivityForResult(intent, REQUEST_CODE_ADD_SOUND_DIALOG);
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:113:        startActivityForResult(intent, REQUEST_CODE_ADD_FONT_DIALOG);
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:161:        startActivityForResult(intent, REQUEST_CODE_SHOW_IMAGE_DETAILS);
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:171:        startActivityForResult(intent, REQUEST_CODE_SHOW_SOUND_DETAILS);
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:180:        startActivityForResult(intent, REQUEST_CODE_SHOW_FONT_DETAILS);
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:188:        startActivityForResult(intent, REQUEST_CODE_SHOW_WIDGET_DETAILS);
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:195:        startActivityForResult(intent, REQUEST_CODE_SHOW_BLOCK_DETAILS);
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:202:        startActivityForResult(intent, REQUEST_CODE_SHOW_MORE_BLOCK_DETAILS);
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:385:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:395:        super.onActivityResult(requestCode, resultCode, data);
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:405:                finish();
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:436:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:437:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:439:            finish();
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:477:    public void onResume() {
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:478:        super.onResume();
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:480:            finish();
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:489:    public void onSaveInstanceState(Bundle outState) {
app/src/main/java/com/besome/sketch/editor/manage/ManageCollectionActivity.java:491:        super.onSaveInstanceState(outState);
app/src/main/java/com/besome/sketch/editor/manage/ShowBlockCollectionActivity.java:122:            finish();
app/src/main/java/com/besome/sketch/editor/manage/ShowBlockCollectionActivity.java:133:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/ShowBlockCollectionActivity.java:134:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/ShowBlockCollectionActivity.java:173:            finish();
app/src/main/java/com/besome/sketch/editor/manage/ShowMoreBlockCollectionActivity.java:128:            finish();
app/src/main/java/com/besome/sketch/editor/manage/ShowMoreBlockCollectionActivity.java:139:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/ShowMoreBlockCollectionActivity.java:140:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/ShowMoreBlockCollectionActivity.java:181:            finish();
app/src/main/java/com/besome/sketch/editor/manage/ShowWidgetCollectionActivity.java:67:            finish();
app/src/main/java/com/besome/sketch/editor/manage/ShowWidgetCollectionActivity.java:78:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/ShowWidgetCollectionActivity.java:79:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:34:public class ViewSelectorActivity extends BaseAppCompatActivity {
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:57:    public void finish() {
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:58:        super.finish();
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:80:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:81:        super.onActivityResult(requestCode, resultCode, data);
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:171:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:173:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:229:                    startActivityForResult(intent, 264);
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:233:                    startActivityForResult(intent, 266);
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:238:        binding.container.setOnClickListener(v -> finish());
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:246:    public void onSaveInstanceState(Bundle outState) {
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:251:        super.onSaveInstanceState(outState);
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:430:                        finish();
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:440:                        startActivityForResult(intent, 265);
app/src/main/java/com/besome/sketch/editor/manage/ViewSelectorActivity.java:450:                        startActivityForResult(intent, requestCode);
app/src/main/java/com/besome/sketch/editor/manage/font/AddFontActivity.java:42:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/font/AddFontActivity.java:43:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/font/AddFontActivity.java:76:                startActivityForResult(Intent.createChooser(intent1, Helper.getResString(R.string.common_word_choose)), REQUEST_CODE_FONT_PICKER);
app/src/main/java/com/besome/sketch/editor/manage/font/AddFontActivity.java:117:            finish();
app/src/main/java/com/besome/sketch/editor/manage/font/AddFontActivity.java:124:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/font/AddFontActivity.java:125:        super.onActivityResult(requestCode, resultCode, data);
app/src/main/java/com/besome/sketch/editor/manage/font/AddFontActivity.java:167:            finish();
app/src/main/java/com/besome/sketch/editor/manage/font/AddFontCollectionActivity.java:33:    public void onCreate(Bundle bundle) {
app/src/main/java/com/besome/sketch/editor/manage/font/AddFontCollectionActivity.java:34:        super.onCreate(bundle);
app/src/main/java/com/besome/sketch/editor/manage/font/AddFontCollectionActivity.java:75:            finish();
app/src/main/java/com/besome/sketch/editor/manage/font/AddFontCollectionActivity.java:85:            finish();
app/src/main/java/com/besome/sketch/editor/manage/font/FontManagerFragment.java:119:            startActivityForResult(intent, 232);
app/src/main/java/com/besome/sketch/editor/manage/font/FontManagerFragment.java:144:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/font/FontManagerFragment.java:145:        super.onActivityResult(requestCode, resultCode, data);
app/src/main/java/com/besome/sketch/editor/manage/font/FontManagerFragment.java:174:    public void onSaveInstanceState(@NonNull Bundle bundle) {
app/src/main/java/com/besome/sketch/editor/manage/font/FontManagerFragment.java:175:        super.onSaveInstanceState(bundle);
app/src/main/java/com/besome/sketch/editor/manage/font/ImportFontFragment.java:183:        startActivityForResult(intent, 271);
app/src/main/java/com/besome/sketch/editor/manage/font/ImportFontFragment.java:210:    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
app/src/main/java/com/besome/sketch/editor/manage/font/ImportFontFragment.java:211:        super.onActivityResult(requestCode, resultCode, intent);
app/src/main/java/com/besome/sketch/editor/manage/font/ImportFontFragment.java:308:    public void onSaveInstanceState(Bundle bundle) {
app/src/main/java/com/besome/sketch/editor/manage/font/ImportFontFragment.java:312:        super.onSaveInstanceState(bundle);
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontActivity.java:50:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontActivity.java:51:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontActivity.java:57:            finish();
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontActivity.java:103:    public void onResume() {
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontActivity.java:104:        super.onResume();
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontActivity.java:106:            finish();
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontActivity.java:111:    public void onSaveInstanceState(Bundle outState) {
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontActivity.java:113:        super.onSaveInstanceState(outState);
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontActivity.java:130:            activity.finish();
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontImportActivity.java:143:            finish();
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontImportActivity.java:148:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontImportActivity.java:149:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontImportActivity.java:151:            finish();
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontImportActivity.java:195:    public void onResume() {
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontImportActivity.java:196:        super.onResume();
app/src/main/java/com/besome/sketch/editor/manage/font/ManageFontImportActivity.java:198:            finish();
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageActivity.java:77:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageActivity.java:78:        super.onActivityResult(requestCode, resultCode, data);
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageActivity.java:119:            finish();
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageActivity.java:121:            finish();
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageActivity.java:137:    public void onCreate(Bundle bundle) {
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageActivity.java:138:        super.onCreate(bundle);
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageActivity.java:244:            startActivityForResult(Intent.createChooser(intent, getString(R.string.common_word_choose)), 215);
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageActivity.java:356:            activity.finish();
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageCollectionActivity.java:85:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageCollectionActivity.java:86:        super.onActivityResult(requestCode, resultCode, data);
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageCollectionActivity.java:108:                finish();
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageCollectionActivity.java:110:                finish();
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageCollectionActivity.java:129:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageCollectionActivity.java:130:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageCollectionActivity.java:200:            startActivityForResult(Intent.createChooser(intent, getString(R.string.common_word_choose)), 215);
app/src/main/java/com/besome/sketch/editor/manage/image/AddImageCollectionActivity.java:297:            activity.finish();
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageActivity.java:73:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageActivity.java:74:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageActivity.java:79:            finish();
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageActivity.java:102:    public void onResume() {
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageActivity.java:103:        super.onResume();
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageActivity.java:105:            finish();
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageActivity.java:110:    public void onSaveInstanceState(Bundle outState) {
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageActivity.java:112:        super.onSaveInstanceState(outState);
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageActivity.java:145:            activity.finish();
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageImportActivity.java:108:                        finish();
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageImportActivity.java:138:    public void onCreate(@Nullable Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageImportActivity.java:139:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageImportActivity.java:141:            finish();
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageImportActivity.java:193:    public void onResume() {
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageImportActivity.java:194:        super.onResume();
app/src/main/java/com/besome/sketch/editor/manage/image/ManageImageImportActivity.java:196:            finish();
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:119:        startActivityForResult(intent, REQUEST_CODE_APPCOMPAT_ACTIVITY);
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:157:        startActivityForResult(intent, REQUEST_CODE_ADMOB_ACTIVITY);
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:165:        startActivityForResult(intent, REQUEST_CODE_FIREBASE_ACTIVITY);
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:173:        startActivityForResult(intent, REQUEST_CODE_GOOGLE_MAPS_ACTIVITY);
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:181:        startActivityForResult(intent, REQUEST_CODE_CUSTOM_ITEM_LIBRARY_ACTIVITY);
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:188:        startActivityForResult(intent, REQUEST_CODE_MATERIAL3_ACTIVITY);
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:195:        startActivity(intent);
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:212:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:213:        super.onActivityResult(requestCode, resultCode, data);
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:303:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:305:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:307:            finish();
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:389:    public void onResume() {
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:390:        super.onResume();
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:392:            finish();
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:397:    public void onSaveInstanceState(Bundle outState) {
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:407:        super.onSaveInstanceState(outState);
app/src/main/java/com/besome/sketch/editor/manage/library/ManageLibraryActivity.java:439:            activity.get().finish();
app/src/main/java/com/besome/sketch/editor/manage/library/admob/AdmobActivity.java:119:                finish();
app/src/main/java/com/besome/sketch/editor/manage/library/admob/AdmobActivity.java:131:            finish();
app/src/main/java/com/besome/sketch/editor/manage/library/admob/AdmobActivity.java:171:                startActivity(intent);
app/src/main/java/com/besome/sketch/editor/manage/library/admob/AdmobActivity.java:182:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/library/admob/AdmobActivity.java:183:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/library/admob/AdmobActivity.java:228:    public void onSaveInstanceState(Bundle outState) {
app/src/main/java/com/besome/sketch/editor/manage/library/admob/AdmobActivity.java:230:        super.onSaveInstanceState(outState);
app/src/main/java/com/besome/sketch/editor/manage/library/admob/AdmobActivity.java:243:                    startActivity(intent);
app/src/main/java/com/besome/sketch/editor/manage/library/admob/AdmobActivity.java:263:                startActivity(intent);
app/src/main/java/com/besome/sketch/editor/manage/library/admob/ManageAdmobActivity.java:106:                startActivity(intent);
app/src/main/java/com/besome/sketch/editor/manage/library/admob/ManageAdmobActivity.java:125:            startActivity(intent);
app/src/main/java/com/besome/sketch/editor/manage/library/admob/ManageAdmobActivity.java:132:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/library/admob/ManageAdmobActivity.java:133:        super.onActivityResult(requestCode, resultCode, data);
app/src/main/java/com/besome/sketch/editor/manage/library/admob/ManageAdmobActivity.java:183:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/library/admob/ManageAdmobActivity.java:184:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/library/admob/ManageAdmobActivity.java:244:    public void onSaveInstanceState(Bundle outState) {
app/src/main/java/com/besome/sketch/editor/manage/library/admob/ManageAdmobActivity.java:246:        super.onSaveInstanceState(outState);
app/src/main/java/com/besome/sketch/editor/manage/library/admob/ManageAdmobActivity.java:275:                startActivity(intent);
app/src/main/java/com/besome/sketch/editor/manage/library/admob/ManageAdmobActivity.java:288:        startActivityForResult(intent, 236);
app/src/main/java/com/besome/sketch/editor/manage/library/compat/ManageCompatActivity.java:84:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/library/compat/ManageCompatActivity.java:85:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/FirebaseActivity.java:96:                finish();
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/FirebaseActivity.java:112:                    startActivity(intent);
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/FirebaseActivity.java:132:                startActivity(intent);
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/FirebaseActivity.java:148:            finish();
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/FirebaseActivity.java:177:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/FirebaseActivity.java:178:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/FirebaseActivity.java:258:    public void onSaveInstanceState(Bundle outState) {
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/FirebaseActivity.java:260:        super.onSaveInstanceState(outState);
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/FirebaseActivity.java:272:                startActivity(intent);
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/ManageFirebaseActivity.java:73:                startActivity(intent);
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/ManageFirebaseActivity.java:92:            startActivity(intent);
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/ManageFirebaseActivity.java:106:            startActivity(intent);
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/ManageFirebaseActivity.java:163:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/ManageFirebaseActivity.java:164:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/ManageFirebaseActivity.java:349:    public void onSaveInstanceState(Bundle outState) {
app/src/main/java/com/besome/sketch/editor/manage/library/firebase/ManageFirebaseActivity.java:351:        super.onSaveInstanceState(outState);
app/src/main/java/com/besome/sketch/editor/manage/library/googlemap/ManageGoogleMapActivity.java:44:                startActivity(openDocIntent);
app/src/main/java/com/besome/sketch/editor/manage/library/googlemap/ManageGoogleMapActivity.java:63:                startActivity(intent);
app/src/main/java/com/besome/sketch/editor/manage/library/googlemap/ManageGoogleMapActivity.java:107:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/library/googlemap/ManageGoogleMapActivity.java:108:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/library/googlemap/ManageGoogleMapActivity.java:142:    public void onSaveInstanceState(Bundle outState) {
app/src/main/java/com/besome/sketch/editor/manage/library/googlemap/ManageGoogleMapActivity.java:145:        super.onSaveInstanceState(outState);
app/src/main/java/com/besome/sketch/editor/manage/library/material3/Material3LibraryActivity.java:25:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/library/material3/Material3LibraryActivity.java:26:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/library/material3/Material3LibraryActivity.java:43:                    .setPositiveButton("OK", (dialog, which) -> finish())
app/src/main/java/com/besome/sketch/editor/manage/library/material3/Material3LibraryActivity.java:87:                finish();
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundActivity.java:69:    public void finish() {
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundActivity.java:80:        super.finish();
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundActivity.java:86:        startActivityForResult(Intent.createChooser(intent, getString(R.string.common_word_choose)), REQUEST_CODE_SOUND_PICKER);
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundActivity.java:100:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundActivity.java:101:        super.onActivityResult(requestCode, resultCode, data);
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundActivity.java:117:            finish();
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundActivity.java:129:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundActivity.java:130:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundActivity.java:203:    public void onPause() {
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundActivity.java:204:        super.onPause();
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundActivity.java:250:                            finish();
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundCollectionActivity.java:51:    public void finish() {
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundCollectionActivity.java:60:        super.finish();
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundCollectionActivity.java:80:    public void onActivityResult(int i, int i2, Intent intent) {
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundCollectionActivity.java:83:        super.onActivityResult(i, i2, intent);
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundCollectionActivity.java:102:            finish();
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundCollectionActivity.java:122:    public void onCreate(Bundle bundle) {
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundCollectionActivity.java:123:        super.onCreate(bundle);
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundCollectionActivity.java:177:    public void onPause() {
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundCollectionActivity.java:178:        super.onPause();
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundCollectionActivity.java:185:        startActivityForResult(Intent.createChooser(intent, getString(R.string.common_word_choose)), 218);
app/src/main/java/com/besome/sketch/editor/manage/sound/AddSoundCollectionActivity.java:233:            finish();
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundActivity.java:60:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundActivity.java:61:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundActivity.java:63:            finish();
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundActivity.java:83:    public void onResume() {
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundActivity.java:84:        super.onResume();
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundActivity.java:86:            finish();
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundActivity.java:91:    public void onSaveInstanceState(Bundle outState) {
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundActivity.java:93:        super.onSaveInstanceState(outState);
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundActivity.java:123:            activity.finish();
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundImportActivity.java:146:                    finish();
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundImportActivity.java:153:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundImportActivity.java:154:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundImportActivity.java:156:            finish();
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundImportActivity.java:210:    public void onPause() {
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundImportActivity.java:211:        super.onPause();
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundImportActivity.java:224:    public void onResume() {
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundImportActivity.java:225:        super.onResume();
app/src/main/java/com/besome/sketch/editor/manage/sound/ManageSoundImportActivity.java:227:            finish();
app/src/main/java/com/besome/sketch/editor/manage/view/AddCustomViewActivity.java:33:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/view/AddCustomViewActivity.java:34:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/view/AddCustomViewActivity.java:53:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/view/AddCustomViewActivity.java:75:            finish();
app/src/main/java/com/besome/sketch/editor/manage/view/AddCustomViewActivity.java:79:            startActivityForResult(intent, REQ_CD_PRESET_ACTIVITY);
app/src/main/java/com/besome/sketch/editor/manage/view/AddCustomViewActivity.java:81:            finish();
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:33:public class AddViewActivity extends BaseAppCompatActivity {
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:185:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:186:        super.onActivityResult(requestCode, resultCode, data);
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:196:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:197:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:202:        binding.toolbar.setNavigationOnClickListener(v -> finish());
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:241:            finish();
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:276:        finish();
app/src/main/java/com/besome/sketch/editor/manage/view/AddViewActivity.java:289:        finish();
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:45:public class ManageViewActivity extends BaseAppCompatActivity implements OnClickListener, ViewPager.OnPageChangeListener {
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:193:    public void onActivityResult(int requestCode, int resultCode, Intent data) {
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:194:        super.onActivityResult(requestCode, resultCode, data);
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:266:                startActivityForResult(intent, isActivitiesTab ? REQUEST_CODE_ADD_ACTIVITY : REQUEST_CODE_ADD_CUSTOM_VIEW);
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:272:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:273:        super.onCreate(savedInstanceState);
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:280:            finish();
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:332:    public void onResume() {
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:333:        super.onResume();
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:335:            finish();
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:346:    public void onSaveInstanceState(Bundle newState) {
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:350:        super.onSaveInstanceState(newState);
app/src/main/java/com/besome/sketch/editor/manage/view/ManageViewActivity.java:367:            activity.finish();
app/src/main/java/com/besome/sketch/editor/manage/view/PresetSettingActivity.java:44:        finish();
app/src/main/java/com/besome/sketch/editor/manage/view/PresetSettingActivity.java:79:            finish();
app/src/main/java/com/besome/sketch/editor/manage/view/PresetSettingActivity.java:90:    public void onCreate(Bundle savedInstanceState) {
app/src/main/java/com/besome/sketch/editor/manage/view/PresetSettingActivity.java:91:        super.onCreate(savedInstanceState);

## Runtime bridge/store symbols

app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:23:import androidx.appcompat.widget.SwitchCompat;
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:55:import pro.sketchware.creator.runtime.CreatorRuntimeCompatibilityInspector;
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:97:        runtimeEnvironment = new CreatorRuntimeEnvironment(this, (serviceId, eventName, payload) ->
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:98:                runOnUiThread(() -> handleRuntimeServiceEvent(serviceId, eventName, payload)));
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:113:        findViewById(R.id.creator_checkpoint).setOnClickListener(v -> createCheckpoint());
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:227:            String legacyScId = CreatorLegacyProjectBridge.ensureLegacyProject(this, document);
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:230:                    .putExtra("creator_runtime_project_id", document.getProjectId());
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:276:    private void createCheckpoint() {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:318:        CreatorCompatibilityReport report = CreatorRuntimeCompatibilityInspector.inspect(session.getDocument());
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:498:            SwitchCompat toggle = new SwitchCompat(this);
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:539:            pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:608:            calendar.setOnDateChangeListener((view, year, month, day) -> dispatchRuntimeEvent(widget.getId(), "date_selected"));
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:712:            picker.setOnTimeChangedListener((view, hour, minute) -> dispatchRuntimeEvent(widget.getId(), "time_selected"));
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:919:        if (hasRuntimeClickBinding(widget.getId())) view.setOnClickListener(v -> dispatchRuntimeEvent(widget.getId(), "click"));
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:923:    private boolean hasRuntimeClickBinding(String widgetId) {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:943:        String backgroundColor = pro.sketchware.creator.runtime.CreatorRuntimeResourceValues.resolveColor(document,
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:946:            try { view.setBackgroundColor(android.graphics.Color.parseColor(backgroundColor)); }
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:957:            String textColor = pro.sketchware.creator.runtime.CreatorRuntimeResourceValues.resolveColor(document,
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:960:                try { text.setTextColor(android.graphics.Color.parseColor(textColor)); }
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:966:            String hintColor = pro.sketchware.creator.runtime.CreatorRuntimeResourceValues.resolveColor(document,
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:969:                try { ((EditText) view).setHintTextColor(android.graphics.Color.parseColor(hintColor)); }
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1062:        if (value instanceof String && ("true".equalsIgnoreCase((String) value) || "false".equalsIgnoreCase((String) value))) {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1169:    private void dispatchRuntimeEvent(String widgetId, String eventName) {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1170:        java.util.List<CreatorRuntimeExecutor.Effect> effects = runtimeExecutor.dispatch(session.getEngine(), widgetId, eventName);
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1175:    private void dispatchLifecycleEvent(String eventName) {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1178:                CreatorLegacyArtifactImporter.ACTIVITY_EVENT_TARGET, eventName));
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1181:    private void handleRuntimeServiceEvent(String serviceId, String eventName, Map<String, Object> payload) {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1182:        if ("creator_runtime".equals(serviceId) && "open_editor".equals(eventName)) {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1186:        if ("intent".equals(serviceId) && "navigate".equals(eventName) && payload.get("screenId") != null) {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1190:            renderEffects(runtimeExecutor.dispatch(session.getEngine(), String.valueOf(payload.get("timerId")), eventName));
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1192:        if ("firebase".equals(serviceId) && "children".equals(eventName)) {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1211:        if ("dialog".equals(serviceId) && "button".equals(eventName)) {
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1217:        Object rawComponents = session.getDocument().getState().get("legacy.components");
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1223:                renderEffects(runtimeExecutor.dispatch(session.getEngine(), String.valueOf(entry.getKey()), eventName));
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1226:        String summary = serviceId + " · " + eventName;
app/src/main/java/pro/sketchware/creator/CreatorProjectActivity.java:1227:        if ("error".equals(eventName) && payload.get("message") != null) {
app/src/main/java/pro/sketchware/creator/CreatorHomeActivity.java:26:        String legacyScId = CreatorLegacyProjectBridge.ensureLegacyProject(this, document);
app/src/main/java/pro/sketchware/creator/CreatorHomeActivity.java:29:                .putExtra("creator_runtime_project_id", document.getProjectId()));
app/src/main/java/pro/sketchware/creator/CreatorShakeRecovery.java:38:    @Override public void onSensorChanged(SensorEvent event) {
app/src/main/java/pro/sketchware/creator/CreatorShakeRecovery.java:39:        if (!started || event == null || event.values == null || event.values.length < 3) return;
app/src/main/java/pro/sketchware/creator/CreatorShakeRecovery.java:40:        if (detector.onSample(event.values[0], event.values[1], event.values[2],
app/src/main/java/pro/sketchware/creator/runtime/CreatorAnimatorService.java:24:        if (action != null) return executeConfigured(action, arguments);
app/src/main/java/pro/sketchware/creator/runtime/CreatorAnimatorService.java:53:    private synchronized Result executeConfigured(String action, Map<String, Object> arguments) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorAnimatorService.java:54:        String componentId = CreatorRuntimeServiceArguments.string(arguments, "componentId");
app/src/main/java/pro/sketchware/creator/runtime/CreatorAnimatorService.java:55:        if (componentId == null || componentId.trim().isEmpty()) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorAnimatorService.java:56:            return CreatorRuntimeServiceArguments.invalid("animator configuration requires componentId.");
app/src/main/java/pro/sketchware/creator/runtime/CreatorAnimatorService.java:58:        Configuration configuration = configurations.get(componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorAnimatorService.java:61:            configurations.put(componentId, configuration);
app/src/main/java/pro/sketchware/creator/runtime/CreatorAnimatorService.java:85:                configuration.repeatMode = "REVERSE".equalsIgnoreCase(mode) ? ValueAnimator.REVERSE : ValueAnimator.RESTART;
app/src/main/java/pro/sketchware/creator/runtime/CreatorAnimatorService.java:95:                return start(componentId, configuration);
app/src/main/java/pro/sketchware/creator/runtime/CreatorAnimatorService.java:99:                return CreatorRuntimeServiceArguments.succeeded("cancelled", true, "componentId", componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorAnimatorService.java:105:            return CreatorRuntimeServiceArguments.succeeded("configured", true, "componentId", componentId, "action", action);
app/src/main/java/pro/sketchware/creator/runtime/CreatorAnimatorService.java:111:    private Result start(String componentId, Configuration configuration) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorAnimatorService.java:128:        return CreatorRuntimeServiceArguments.succeeded("started", true, "componentId", componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorBluetoothService.java:188:    private void publish(String event, String tag, String state, String detail) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorBluetoothService.java:189:        environment.publish(getId(), event, CreatorRuntimeServiceArguments.output("tag", tag, "state", state, "message", detail));
app/src/main/java/pro/sketchware/creator/runtime/CreatorCalendarService.java:10:/** Runtime-native calendar operations corresponding to the legacy Calendar component. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorCalendarService.java:17:        String componentId = CreatorRuntimeServiceArguments.string(arguments, "componentId");
app/src/main/java/pro/sketchware/creator/runtime/CreatorCalendarService.java:18:        if (componentId == null || componentId.trim().isEmpty()) componentId = "runtime";
app/src/main/java/pro/sketchware/creator/runtime/CreatorCalendarService.java:19:        Calendar calendar = calendars.get(componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorCalendarService.java:22:            calendars.put(componentId, calendar);
app/src/main/java/pro/sketchware/creator/runtime/CreatorCalendarService.java:26:                // Query only: preserve the component-scoped Calendar state.
app/src/main/java/pro/sketchware/creator/runtime/CreatorCalendarService.java:40:                        CreatorRuntimeServiceArguments.output("componentId", componentId,
app/src/main/java/pro/sketchware/creator/runtime/CreatorCalendarService.java:47:                        "Unknown calendar component: " + otherId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorCalendarService.java:49:                        CreatorRuntimeServiceArguments.output("componentId", componentId, "otherComponentId", otherId,
app/src/main/java/pro/sketchware/creator/runtime/CreatorCalendarService.java:58:        output.put("componentId", componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorFileService.java:150:            try { return within(resolveCanonical(externalRoot), resolveCanonical(rawPath)); }
app/src/main/java/pro/sketchware/creator/runtime/CreatorFileService.java:249:            File target = resolveCanonical(rawPath);
app/src/main/java/pro/sketchware/creator/runtime/CreatorFileService.java:250:            for (File root : permittedRoots) if (within(resolveCanonical(root), target)) return target;
app/src/main/java/pro/sketchware/creator/runtime/CreatorFileService.java:254:        private static File resolveCanonical(String rawPath) throws IOException { return new File(rawPath).getCanonicalFile(); }
app/src/main/java/pro/sketchware/creator/runtime/CreatorFileService.java:255:        private static File resolveCanonical(File rawPath) throws IOException { return rawPath.getCanonicalFile(); }
app/src/main/java/pro/sketchware/creator/runtime/CreatorDialogService.java:172:        if ("true".equalsIgnoreCase(text)) return true;
app/src/main/java/pro/sketchware/creator/runtime/CreatorDialogService.java:173:        if ("false".equalsIgnoreCase(text)) return false;
app/src/main/java/pro/sketchware/creator/runtime/CreatorDialogService.java:178:        return "STYLE_HORIZONTAL".equalsIgnoreCase(style) || "HORIZONTAL".equalsIgnoreCase(style)
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseAuthService.java:51:    private void publishUser(String event, FirebaseUser user) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseAuthService.java:52:        environment.publish(getId(), event, CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseDatabaseService.java:41:                task = reference.push().updateChildren(CreatorRuntimeServiceArguments.map(arguments, "value"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseDatabaseService.java:42:            } else task = reference.updateChildren(CreatorRuntimeServiceArguments.map(arguments, "value"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseDatabaseService.java:92:                    publishChild("child_added", path, snapshot, previousChildName);
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseDatabaseService.java:95:                    publishChild("child_changed", path, snapshot, previousChildName);
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseDatabaseService.java:97:                @Override public void onChildRemoved(DataSnapshot snapshot) { publishChild("child_removed", path, snapshot, null); }
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseDatabaseService.java:99:                    publishChild("child_moved", path, snapshot, previousChildName);
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseDatabaseService.java:115:    private void publishChild(String event, String path, DataSnapshot snapshot, String previousChildName) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseDatabaseService.java:116:        environment.publish(getId(), event, CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseStorageService.java:47:                                        "bytes", snapshot.getBytesTransferred(), "totalBytes", snapshot.getTotalByteCount())))
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseStorageService.java:73:                    CreatorRuntimeServiceArguments.output("path", path, "bytes", snapshot.getBytesTransferred(), "totalBytes", snapshot.getTotalByteCount())))
app/src/main/java/pro/sketchware/creator/runtime/CreatorIntentService.java:93:                environment.getActivity().startActivity(Intent.createChooser(share,
app/src/main/java/pro/sketchware/creator/runtime/CreatorInterstitialAdService.java:23:        String componentId = CreatorRuntimeServiceArguments.string(arguments, "componentId");
app/src/main/java/pro/sketchware/creator/runtime/CreatorInterstitialAdService.java:24:        if (componentId == null || componentId.trim().isEmpty()) componentId = "runtime";
app/src/main/java/pro/sketchware/creator/runtime/CreatorInterstitialAdService.java:25:        final String id = componentId;
app/src/main/java/pro/sketchware/creator/runtime/CreatorInterstitialAdService.java:26:        if ("create".equals(action)) return CreatorRuntimeServiceArguments.succeeded("created", true, "componentId", id);
app/src/main/java/pro/sketchware/creator/runtime/CreatorInterstitialAdService.java:37:                            "componentId", id, "adUnitId", adUnitId));
app/src/main/java/pro/sketchware/creator/runtime/CreatorInterstitialAdService.java:42:                            "componentId", id, "action", "load", "message", error.getMessage()));
app/src/main/java/pro/sketchware/creator/runtime/CreatorInterstitialAdService.java:45:            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action, "componentId", id);
app/src/main/java/pro/sketchware/creator/runtime/CreatorInterstitialAdService.java:56:                            "componentId", id, "action", "show", "message", error.getMessage()));
app/src/main/java/pro/sketchware/creator/runtime/CreatorInterstitialAdService.java:60:            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action, "componentId", id,
app/src/main/java/pro/sketchware/creator/runtime/CreatorInterstitialAdService.java:66:    private void publish(String componentId, String event) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorInterstitialAdService.java:67:        environment.publish(getId(), event, CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorInterstitialAdService.java:68:                "componentId", componentId, "adUnitId", loadedUnitIds.get(componentId)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:7:import com.besome.sketch.beans.ProjectFileBean;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:25: * Imports legacy components, events, and block chains into the versioned Creator
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:26: * Runtime document. Unsupported executable blocks stay visible in the report
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:42:    public Result importArtifacts(CreatorProjectDocument base, List<ComponentBean> components,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:43:                                  List<EventBean> events, Map<String, List<BlockBean>> blocksByEvent) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:44:        return importArtifacts(base, components, events, blocksByEvent,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:48:    public Result importArtifacts(CreatorProjectDocument base, List<ComponentBean> components,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:49:                                  List<EventBean> events, Map<String, List<BlockBean>> blocksByEvent,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:54:        Map<String, Object> componentState = new LinkedHashMap<>();
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:55:        for (ComponentBean component : components == null ? Collections.<ComponentBean>emptyList() : components) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:56:            if (component == null || blank(component.componentId)) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:61:            String serviceId = CreatorRuntimeComponentServiceMatrix.serviceFor(component.type);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:63:                report.add(component.componentId, "ComponentBean", CreatorCompatibilityTier.R0_UNSUPPORTED,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:64:                        "No Creator Runtime service is registered for component type " + component.type + ".");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:69:            descriptor.put("type", component.type);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:70:            descriptor.put("param1", component.param1);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:71:            descriptor.put("param2", component.param2);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:72:            descriptor.put("param3", component.param3);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:73:            componentState.put(component.componentId, descriptor);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:74:            report.add(component.componentId, "ComponentBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:77:        state.put("legacy.components", componentState);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:81:        for (EventBean event : events == null ? Collections.<EventBean>emptyList() : events) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:82:            if (event == null || blank(event.targetId) || blank(event.eventName)) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:87:            String eventKey = event.getEventKey();
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:88:            List<BlockBean> legacyBlocks = blocksByEvent == null ? null : blocksByEvent.get(eventKey);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:89:            BlockConversion blocks = convertBlocks(legacyBlocks, componentState);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:90:            if (!blocks.unsupported.isEmpty()) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:91:                deferredEvents.put(eventKey, blocks.unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:92:                report.add(eventKey, "BlockBean", CreatorCompatibilityTier.R0_UNSUPPORTED,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:93:                        "Unsupported legacy block opcodes: " + String.join(", ", blocks.unsupported) + ".");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:96:            for (Map.Entry<String, List<CreatorRuntimeBlock>> callback : blocks.timerCallbacks.entrySet()) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:123:            if (!base.getWidgets().containsKey(event.targetId)) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:125:                descriptor.put("eventType", event.eventType);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:126:                descriptor.put("targetId", event.targetId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:127:                descriptor.put("eventName", normalizeEventName(event.eventName));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:128:                descriptor.put("blockCount", blocks.converted.size());
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:129:                deferredEvents.put(eventKey, descriptor);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:130:                if (event.eventType == EventBean.EVENT_TYPE_ACTIVITY) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:131:                    String bindingId = "legacy_activity_" + normalizeEventName(event.eventName);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:133:                            normalizeEventName(event.eventName), blocks.converted));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:134:                } else if (event.eventType == EventBean.EVENT_TYPE_COMPONENT) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:135:                    String bindingId = "legacy_component_" + eventKey;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:136:                    bindings.put(bindingId, new CreatorEventBinding(bindingId, event.targetId,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:137:                            normalizeEventName(event.eventName), blocks.converted));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:139:                report.add(eventKey, "EventBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:140:                        event.eventType == EventBean.EVENT_TYPE_ACTIVITY || event.eventType == EventBean.EVENT_TYPE_COMPONENT
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:141:                                ? "Imported as a typed runtime event binding with a compatibility descriptor."
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:142:                                : "Imported as a runtime event descriptor.");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:145:            String bindingId = "legacy_" + eventKey;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:146:            bindings.put(bindingId, new CreatorEventBinding(bindingId, event.targetId,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:147:                    normalizeEventName(event.eventName), blocks.converted));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:148:            report.add(eventKey, "EventBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:149:                    "Imported as a typed Creator Runtime event binding.");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:160:            BlockConversion body = convertBlocks(definition.blocks, componentState);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:162:                report.add("moreblock:" + functionId, "MoreBlockCollectionBean", CreatorCompatibilityTier.R0_UNSUPPORTED,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:172:            String bindingId = "legacy_moreblock_" + functionId;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:174:            report.add("moreblock:" + functionId, "MoreBlockCollectionBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:182:    public Result importProjectMetadata(CreatorProjectDocument base, List<ProjectFileBean> files,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:189:        for (ProjectFileBean file : files == null ? Collections.<ProjectFileBean>emptyList() : files) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:191:                report.add("unknown", "ProjectFileBean", CreatorCompatibilityTier.R0_UNSUPPORTED,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:207:            if (file.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:210:                descriptor.put("hasToolbar", file.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:211:                descriptor.put("isFullscreen", file.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FULLSCREEN));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:212:                descriptor.put("hasFab", file.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:213:                descriptor.put("hasDrawer", file.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:216:                if (file.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:226:            report.add(file.fileName, "ProjectFileBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:239:                        "Arbitrary local or native libraries are blocked; they cannot execute in Creator Runtime.");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:259:            case ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY: return "activity";
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:260:            case ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW: return "custom_view";
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:261:            case ProjectFileBean.PROJECT_FILE_TYPE_DRAWER: return "drawer";
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:262:            case ProjectFileBean.PROJECT_FILE_TYPE_FRAGMENT: return "fragment";
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:263:            case ProjectFileBean.PROJECT_FILE_TYPE_SHEET: return "sheet";
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:264:            case ProjectFileBean.PROJECT_FILE_TYPE_DIALOG_FRAGMENT: return "dialog_fragment";
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:480:    private BlockConversion convertBlocks(List<BlockBean> blocks, Map<String, Object> componentDescriptors) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:485:        for (BlockBean block : blocks == null ? Collections.<BlockBean>emptyList() : blocks) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:486:            if (block == null) continue;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:487:            try { byId.put(Integer.parseInt(block.id), block); } catch (NumberFormatException ignored) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:488:                result.unsupported.add("invalid block id");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:490:            if (block.nextBlock >= 0) referenced.add(block.nextBlock);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:491:            if (block.subStack1 >= 0) referenced.add(block.subStack1);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:492:            if (block.subStack2 >= 0) referenced.add(block.subStack2);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:493:            for (String parameter : block.parameters == null ? Collections.<String>emptyList() : block.parameters) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:507:                    result.unsupported, result.timerCallbacks, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:511:            if (!visited.contains(entry.getKey())) result.unsupported.add("orphan block " + entry.getKey());
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:519:                              Map<String, Object> componentDescriptors) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:524:                unsupported.add("invalid block id"); return;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:526:            if (!visited.add(id)) { unsupported.add("cyclic block graph at " + id); return; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:527:            CreatorRuntimeBlock converted = convertBlock(current, byId, visited, unsupported, timerCallbacks, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:533:    private CreatorRuntimeBlock convertBlock(BlockBean block, Map<Integer, BlockBean> byId,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:536:                                             Map<String, Object> componentDescriptors) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:537:        if (blank(block.opCode)) { unsupported.add("empty"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:538:        String op = block.opCode.trim().toLowerCase(Locale.ROOT);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:539:        List<String> values = block.parameters == null ? Collections.<String>emptyList() : block.parameters;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:542:            String functionId = moreBlockId(block.spec);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:543:            if (blank(functionId)) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:547:                if (argument == null) { unsupported.add(block.opCode + " (invalid argument expression)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:554:            if (values.size() > 1 || block.subStack1 >= 0 || block.subStack2 >= 0) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:555:                unsupported.add(block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:562:                unsupported.add(block.opCode + " (invalid return expression)");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:569:            if (values.size() < 2 || block.subStack1 < 0) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:572:            BlockBean thenStart = byId.get(block.subStack1);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:573:            if (thenStart == null) { unsupported.add(block.opCode + " (missing then substack)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:574:            convertChain(thenStart, byId, visited, thenBlocks, unsupported, timerCallbacks, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:575:            if (block.subStack2 >= 0) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:576:                BlockBean elseStart = byId.get(block.subStack2);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:577:                if (elseStart == null) { unsupported.add(block.opCode + " (missing else substack)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:578:                convertChain(elseStart, byId, visited, elseBlocks, unsupported, timerCallbacks, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:585:            if (values.isEmpty() || block.subStack1 < 0) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:588:            BlockBean thenStart = byId.get(block.subStack1);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:589:            if (thenStart == null) { unsupported.add(block.opCode + " (missing then substack)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:590:            convertChain(thenStart, byId, visited, thenBlocks, unsupported, timerCallbacks, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:591:            if ("ifelse".equals(op) && block.subStack2 >= 0) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:592:                BlockBean elseStart = byId.get(block.subStack2);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:593:                if (elseStart == null) { unsupported.add(block.opCode + " (missing else substack)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:594:                convertChain(elseStart, byId, visited, elseBlocks, unsupported, timerCallbacks, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:599:                if (expression == null) { unsupported.add(block.opCode + " (invalid reporter expression)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:601:            } else if ("true".equalsIgnoreCase(condition) || "false".equalsIgnoreCase(condition)) payload.put("constant", Boolean.valueOf(condition));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:606:            if (block.subStack1 < 0 || block.subStack2 >= 0) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:607:            BlockBean bodyStart = byId.get(block.subStack1);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:608:            if (bodyStart == null) { unsupported.add(block.opCode + " (missing forever substack)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:610:            convertChain(bodyStart, byId, visited, body, unsupported, timerCallbacks, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:615:            if (!values.isEmpty() || block.subStack1 >= 0 || block.subStack2 >= 0) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:619:            if (values.isEmpty() || block.subStack1 < 0) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:620:            BlockBean bodyStart = byId.get(block.subStack1);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:621:            if (bodyStart == null) { unsupported.add(block.opCode + " (missing repeat substack)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:623:            convertChain(bodyStart, byId, visited, body, unsupported, timerCallbacks, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:627:                if (expression == null) { unsupported.add(block.opCode + " (invalid count expression)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:633:        boolean timerWithCallback = "timerafter".equals(op) || "timerevery".equals(op);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:634:        boolean firebaseChildrenWithCallback = "firebasegetchildren".equals(op);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:635:        boolean dialogButtonWithCallback = "dialogokbutton".equals(op) || "dialogcancelbutton".equals(op)
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:637:        boolean viewOnClickWithCallback = "viewonclick".equals(op);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:638:        if ((block.subStack1 >= 0 || block.subStack2 >= 0) && !timerWithCallback && !firebaseChildrenWithCallback
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:639:                && !dialogButtonWithCallback && !viewOnClickWithCallback) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:640:            unsupported.add(block.opCode + " (control flow)"); return null;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:642:        if (timerWithCallback && block.subStack2 >= 0) { unsupported.add(block.opCode + " (unexpected else substack)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:643:        if (firebaseChildrenWithCallback && block.subStack2 >= 0) { unsupported.add(block.opCode + " (unexpected else substack)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:644:        if (dialogButtonWithCallback && block.subStack2 >= 0) { unsupported.add(block.opCode + " (unexpected else substack)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:645:        if (viewOnClickWithCallback && block.subStack2 >= 0) { unsupported.add(block.opCode + " (unexpected else substack)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:648:            if (values.size() < required) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:649:            if (block.subStack1 >= 0) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:650:                BlockBean callbackStart = byId.get(block.subStack1);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:651:                if (callbackStart == null) { unsupported.add(block.opCode + " (missing timer substack)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:653:                convertChain(callbackStart, byId, visited, callback, unsupported, timerCallbacks, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:661:            return serviceCall("timer", arguments);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:663:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:664:            return serviceCall("timer", CreatorRuntimeServiceArguments.output("timerId", values.get(0), "action", "cancel"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:666:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:667:            return serviceCall("vibrator", CreatorRuntimeServiceArguments.output("durationMs", values.get(1)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:669:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:674:            return listMapPutAt(block, values, unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:676:            return listMapInsert(block, values, unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:678:            return listMapGet(block, values, unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:680:            return listSetAt(block, values, unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:682:            return listMutation(block, values, "add", unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:684:            return listMutation(block, values, "insert", unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:686:            return listMutation(block, values, "remove_at", unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:688:            return listMutation(block, values, "clear", unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:690:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:696:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:702:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:705:            putExpressionOrValue(payload, "json", "jsonExpression", values.get(0), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:708:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:711:            putExpressionOrValue(payload, "json", "jsonExpression", values.get(0), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:714:            return mapMutation(block, values, "create", unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:716:            return mapMutation(block, values, "put", unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:718:            return mapMutation(block, values, "remove", unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:720:            return mapMutation(block, values, "clear", unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:722:            return intentCall(block, values, "configure_action", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:724:            return intentCall(block, values, "configure_data", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:726:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:727:            return serviceCall("intent", CreatorRuntimeServiceArguments.output("intentId", values.get(0),
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:730:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:731:            return serviceCall("intent", CreatorRuntimeServiceArguments.output("intentId", values.get(0),
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:734:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:735:            return serviceCall("intent", CreatorRuntimeServiceArguments.output("intentId", values.get(0),
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:738:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:739:            return serviceCall("intent", CreatorRuntimeServiceArguments.output("intentId", values.get(0), "action", "start"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:741:            return serviceCall("intent", CreatorRuntimeServiceArguments.output("intentId", "runtime", "action", "finish"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:743:            return dialogCall(block, values, "set_title", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:745:            return dialogCall(block, values, "set_message", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:747:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:748:            return serviceCall("dialog", CreatorRuntimeServiceArguments.output("dialogId", values.get(0), "action", "show"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:750:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:751:            return serviceCall("dialog", CreatorRuntimeServiceArguments.output("dialogId", values.get(0), "action", "dismiss"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:754:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:761:            if (block.subStack1 >= 0) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:762:                BlockBean callbackStart = byId.get(block.subStack1);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:763:                if (callbackStart == null) { unsupported.add(block.opCode + " (missing callback substack)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:764:                String callbackId = block.id + "_" + button;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:766:                convertChain(callbackStart, byId, visited, callback, unsupported, timerCallbacks, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:770:            return serviceCall("dialog", arguments);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:772:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:774:            if (block.subStack1 >= 0) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:775:                BlockBean callbackStart = byId.get(block.subStack1);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:776:                if (callbackStart == null) { unsupported.add(block.opCode + " (missing callback substack)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:777:                convertChain(callbackStart, byId, visited, callback, unsupported, timerCallbacks, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:781:                            "targetWidgetId", values.get(0), "eventName", "click"), callback,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:784:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:785:            return serviceCall("media", CreatorRuntimeServiceArguments.output("id", values.get(0),
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:790:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:793:            return serviceCall("media", CreatorRuntimeServiceArguments.output("id", values.get(0), "action", action));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:795:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:796:            return serviceCall("media", CreatorRuntimeServiceArguments.output("id", values.get(0),
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:799:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:800:            return serviceCall("media", CreatorRuntimeServiceArguments.output("id", values.get(0),
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:803:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:804:            return serviceCall("ui", CreatorRuntimeServiceArguments.output("action", "set_title", "title", values.get(0)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:806:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:807:            return serviceCall("ui", CreatorRuntimeServiceArguments.output("action", "copy_text", "text", values.get(0)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:809:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:810:            return serviceCall("gyroscope", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:811:                    "componentId", values.get(0), "action", "gyroscopestartlisten".equals(op) ? "start" : "stop"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:813:            if (values.size() < 4) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:814:            return serviceCall("location", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:815:                    "componentId", values.get(0), "action", "start", "provider", normalizeLocationProvider(values.get(1)),
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:818:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:819:            return serviceCall("location", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:820:                    "componentId", values.get(0), "action", "stop"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:822:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:823:            return serviceCall("camera", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:824:                    "componentId", values.get(0), "action", "capture"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:826:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:827:            return serviceCall("file_picker", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:828:                    "componentId", values.get(0), "action", "pick"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:830:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:831:            return serviceCall("text_to_speech", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:832:                    "componentId", values.get(0), "action", "texttospeechsetpitch".equals(op) ? "set_pitch" : "set_rate",
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:835:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:836:            return serviceCall("text_to_speech", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:837:                    "componentId", values.get(0), "action", "speak", "text", values.get(1)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:839:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:840:            return serviceCall("text_to_speech", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:841:                    "componentId", values.get(0), "action", "texttospeechstop".equals(op) ? "stop" : "shutdown"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:843:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:844:            return serviceCall("speech_to_text", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:845:                    "componentId", values.get(0), "action", "listen"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:847:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:848:            return serviceCall("speech_to_text", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:849:                    "componentId", values.get(0), "action", "speechtotextstoplistening".equals(op) ? "stop" : "shutdown"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:851:            return fileCall(block, values, "write", 2, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:853:            return fileCall(block, values, "copy", 2, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:855:            return fileCall(block, values, "copy_dir", 2, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:857:            return fileCall(block, values, "move", 2, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:859:            return fileCall(block, values, "delete", 1, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:861:            return fileCall(block, values, "make_dir", 1, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:863:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:864:            return serviceCall("file", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:867:            return bitmapCall(block, values, "resize_retain_ratio", 3, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:869:            return bitmapCall(block, values, "resize_square", 3, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:871:            return bitmapCall(block, values, "resize_circle", 2, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:873:            return bitmapCall(block, values, "rounded_border", 3, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:875:            return bitmapCall(block, values, "crop_center", 4, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:877:            return bitmapCall(block, values, "rotate", 3, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:879:            return bitmapCall(block, values, "scale", 4, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:881:            return bitmapCall(block, values, "skew", 4, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:883:            return bitmapCall(block, values, "color_filter", 3, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:885:            return bitmapCall(block, values, "brightness", 3, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:887:            return bitmapCall(block, values, "contrast", 3, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:889:            return widgetResourceProperty(block, values, "thumbResource", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:891:            return widgetResourceProperty(block, values, "trackResource", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:895:            return widgetCustomDataProperty(block, values, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:897:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:898:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:902:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:905:            return interstitialCall(values.get(0), action, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:907:            if (!values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:908:            return serviceCall("drawer", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:933:            return animatorCall(block, values, "set_target", 2, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:935:            return animatorCall(block, values, "set_property", 2, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:937:            return animatorCall(block, values, "set_value", 2, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:939:            return animatorCall(block, values, "set_from_to", 3, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:941:            return animatorCall(block, values, "set_duration", 2, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:943:            return animatorCall(block, values, "set_repeat_mode", 2, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:945:            return animatorCall(block, values, "set_repeat_count", 2, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:947:            return animatorCall(block, values, "set_interpolator", 2, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:949:            return animatorCall(block, values, "objectanimatorstart".equals(op) ? "start" : "cancel", 1, unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:951:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:952:            return serviceCall("firebase_auth", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:953:                    "componentId", values.get(0), "action", "firebaseauthcreateuser".equals(op) ? "register" : "sign_in",
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:956:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:957:            return serviceCall("firebase_auth", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:958:                    "componentId", values.get(0), "action", "anonymous"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:960:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:961:            return serviceCall("firebase_auth", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:962:                    "componentId", values.get(0), "action", "reset_password", "email", values.get(1)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:964:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:965:            return serviceCall("firebase_auth", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:966:                    "componentId", values.get(0), "action", "sign_out"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:968:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:969:            return serviceCall("firebase_storage", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:970:                    "componentId", values.get(0), "action", "upload_file", "filePath", values.get(1), "path", values.get(2)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:972:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:973:            return serviceCall("firebase_storage", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:974:                    "componentId", values.get(0), "action", "download_file", "url", values.get(1), "filePath", values.get(2)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:976:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:977:            return serviceCall("firebase_storage", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:978:                    "componentId", values.get(0), "action", "delete_url", "url", values.get(1)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:980:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:982:            arguments.put("componentId", values.get(0));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:984:            arguments.put("path", firebasePath(componentDescriptors, values.get(0), null));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:986:            if (block.subStack1 >= 0) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:987:                BlockBean callbackStart = byId.get(block.subStack1);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:988:                if (callbackStart == null) { unsupported.add(block.opCode + " (missing callback substack)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:989:                String callbackId = String.valueOf(block.id);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:991:                convertChain(callbackStart, byId, visited, callback, unsupported, timerCallbacks, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:995:            return serviceCall("firebase", arguments);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:997:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:998:            return firebaseCall(values.get(0), "remove", firebasePath(componentDescriptors, values.get(0), values.get(1)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1000:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1001:            return serviceCall("firebase", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1002:                    "componentId", values.get(0), "action", "update",
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1003:                    "path", firebasePath(componentDescriptors, values.get(0), values.get(1)),
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1006:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1007:            return serviceCall("firebase", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1008:                    "componentId", values.get(0), "action", "push_update",
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1009:                    "path", firebasePath(componentDescriptors, values.get(0), null),
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1012:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1013:            return firebaseCall(values.get(0), "firebasestartlisten".equals(op) ? "listen" : "stop_listen",
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1014:                    firebasePath(componentDescriptors, values.get(0), null));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1016:            return serviceCall("date_picker", CreatorRuntimeServiceArguments.output("action", "show"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1018:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1019:            return serviceCall("time_picker", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1020:                    "componentId", values.get(0), "action", "show"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1022:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1025:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1028:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1031:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1034:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1036:            arguments.put("componentId", values.get(0)); arguments.put("action", "format");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1038:            return serviceCall("calendar", arguments);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1040:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1042:            arguments.put("componentId", values.get(0)); arguments.put("action", "diff");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1044:            return serviceCall("calendar", arguments);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1046:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1047:            return storageCall(values.get(0), "configure", null, values.get(1), null, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1049:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1050:            return storageCall(values.get(0), "set", values.get(1), values.get(2), null, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1052:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1053:            return storageCall(values.get(0), "remove", values.get(1), null, null, componentDescriptors);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1055:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1056:            return serviceCall("http", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1057:                    "componentId", values.get(0), "action", "set_params", "paramsStateId", values.get(1), "requestType", values.get(2)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1059:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1060:            return serviceCall("http", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1061:                    "componentId", values.get(0), "action", "set_headers", "headersStateId", values.get(1)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1063:            if (values.size() < 4) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1064:            return serviceCall("http", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1065:                    "componentId", values.get(0), "action", "start", "method", values.get(1), "url", values.get(2), "tag", values.get(3)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1070:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1077:            return serviceCall("dialog", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1080:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1081:            return serviceCall("dialog", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1084:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1085:            return serviceCall("media", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1088:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1089:            return serviceCall("media", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1092:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1093:            return serviceCall("media", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1096:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1097:            return serviceCall("media", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1100:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1101:            return bluetoothCall(values.get(0), "ready_connection", null, null, values.get(1));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1103:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1104:            return bluetoothCall(values.get(0), "ready_connection", values.get(1), null, values.get(2));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1106:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1107:            return bluetoothCall(values.get(0), "start_connection", null, values.get(1), values.get(2));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1109:            if (values.size() < 4) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1110:            return bluetoothCall(values.get(0), "start_connection", values.get(1), values.get(2), values.get(3));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1112:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1113:            return bluetoothCall(values.get(0), "stop_connection", null, null, values.get(1));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1115:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1116:            return serviceCall("bluetooth", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1117:                    "componentId", values.get(0), "action", "send_data", "data", values.get(1), "tag", values.get(2)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1119:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1120:            return serviceCall("bluetooth", CreatorRuntimeServiceArguments.output("componentId", values.get(0), "action", "request_enable"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1122:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1123:            return serviceCall("bluetooth", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1124:                    "componentId", values.get(0), "action", "paired_devices", "resultStateId", values.get(1), "resultKey", "devices"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1126:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1127:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1131:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1133:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1137:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1138:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1141:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1142:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1145:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1146:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1149:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1150:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1154:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1156:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1160:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1161:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1165:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1168:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1171:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1172:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1175:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1176:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1179:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1180:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1183:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1184:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1187:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1188:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1191:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1192:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1195:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1196:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1199:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1200:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1203:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1204:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1208:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1209:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1213:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1214:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1217:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1218:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1221:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1222:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1225:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1226:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1229:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1230:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1233:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1234:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1237:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1238:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1241:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1242:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1245:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1246:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1250:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1255:            return serviceCall("widget", CreatorRuntimeServiceArguments.output("widgetId", values.get(0), "action", action));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1258:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1261:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1265:            return widgetProperty(block, values, "text", unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1267:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1273:            return widgetProperty(block, values, "checked", unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1275:            return widgetProperty(block, values, "enabled", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1277:            return widgetProperty(block, values, "visible", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1279:            return widgetProperty(block, values, "clickable", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1281:            return widgetProperty(block, values, "hint", unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1283:            return widgetProperty(block, values, "textColor", unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1285:            return widgetProperty(block, values, "textSize", unsupported, byId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1287:            return widgetProperty(block, values, "hintTextColor", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1289:            return widgetProperty(block, values, "backgroundColor", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1291:            return widgetProperty(block, values, "backgroundResource", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1293:            return widgetProperty(block, values, "alpha", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1295:            return widgetProperty(block, values, "rotation", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1297:            return widgetProperty(block, values, "translationX", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1299:            return widgetProperty(block, values, "translationY", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1301:            return widgetProperty(block, values, "scaleX", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1303:            return widgetProperty(block, values, "scaleY", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1305:            return widgetProperty(block, values, "resourceName", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1307:            return widgetProperty(block, values, "filePath", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1309:            return widgetProperty(block, values, "url", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1311:            return widgetProperty(block, values, "selectedIndex", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1313:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1315:            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1319:            return widgetProperty(block, values, "url", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1321:            return widgetProperty(block, values, "date", unsupported);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1324:            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1328:                if (expression == null) { unsupported.add(block.opCode + " (invalid value expression)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1334:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1338:            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1342:            if (values.isEmpty() || !CreatorRuntimeServiceCatalog.defaults().supports(values.get(0))) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1343:                unsupported.add(block.opCode); return null;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1349:        unsupported.add(block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1353:    private static CreatorRuntimeBlock serviceCall(String serviceId, Map<String, Object> arguments) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1360:    private static CreatorRuntimeBlock listMutation(BlockBean block, List<String> values, String action,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1363:        if (values.size() < required) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1367:        if ("add".equals(action)) putExpressionOrValue(payload, "value", "valueExpression", values.get(1), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1369:            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1370:            putExpressionOrValue(payload, "index", "indexExpression", values.get(1), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1371:            putExpressionOrValue(payload, "value", "valueExpression", values.get(2), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1372:        } else if ("remove_at".equals(action)) putExpressionOrValue(payload, "index", "indexExpression", values.get(1), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1378:    private static CreatorRuntimeBlock listMapPutAt(BlockBean block, List<String> values,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1380:        if (values.size() < 4) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1384:        putExpressionOrValue(payload, "key", "keyExpression", values.get(0), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1385:        putExpressionOrValue(payload, "value", "valueExpression", values.get(1), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1386:        putExpressionOrValue(payload, "index", "indexExpression", values.get(2), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1392:    private static CreatorRuntimeBlock listMapInsert(BlockBean block, List<String> values,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1394:        if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1398:        putExpressionOrValue(payload, "value", "valueExpression", values.get(0), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1399:        putExpressionOrValue(payload, "index", "indexExpression", values.get(1), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1405:    private static CreatorRuntimeBlock listMapGet(BlockBean block, List<String> values,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1407:        if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1412:        arguments.add(literalExpression(values.get(0), byId, unsupported, block.opCode));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1413:        arguments.add(literalExpression(values.get(1), byId, unsupported, block.opCode));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1434:    private static CreatorRuntimeBlock listSetAt(BlockBean block, List<String> values,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1436:        if (values.size() < 3) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1440:        putExpressionOrValue(payload, "value", "valueExpression", values.get(0), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1441:        putExpressionOrValue(payload, "index", "indexExpression", values.get(1), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1446:    private static CreatorRuntimeBlock mapMutation(BlockBean block, List<String> values, String action,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1449:        if (values.size() < required) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1454:            putExpressionOrValue(payload, "key", "keyExpression", values.get(1), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1455:        if ("put".equals(action)) putExpressionOrValue(payload, "value", "valueExpression", values.get(2), byId, unsupported, block.opCode);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1470:    private static CreatorRuntimeBlock intentCall(BlockBean block, List<String> values, String action,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1472:        if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1473:        return serviceCall("intent", CreatorRuntimeServiceArguments.output("intentId", values.get(0),
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1477:    private static CreatorRuntimeBlock dialogCall(BlockBean block, List<String> values, String action,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1479:        if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1480:        return serviceCall("dialog", CreatorRuntimeServiceArguments.output("dialogId", values.get(0),
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1484:    private static CreatorRuntimeBlock fileCall(BlockBean block, List<String> values, String action, int required,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1486:        if (values.size() < required) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1492:        return serviceCall("file", arguments);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1495:    private static CreatorRuntimeBlock bitmapCall(BlockBean block, List<String> values, String action, int required,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1497:        if (values.size() < required) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1505:            // Legacy blocks retain height at index 2 and width at index 3; Fx reverses them for FileUtil.
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1514:        return serviceCall("bitmap", arguments);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1517:    private static CreatorRuntimeBlock widgetResourceProperty(BlockBean block, List<String> values, String property,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1519:        if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1525:    private static CreatorRuntimeBlock widgetCustomDataProperty(BlockBean block, List<String> values,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1527:        if (values.size() < 2 || blank(values.get(0))) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1533:    private static CreatorRuntimeBlock animatorCall(BlockBean block, List<String> values, String action, int required,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1535:        if (values.size() < required) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1537:        arguments.put("componentId", values.get(0));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1549:        return serviceCall("animator", arguments);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1552:    private static CreatorRuntimeBlock firebaseCall(String componentId, String action, String path) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1553:        return serviceCall("firebase", CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1554:                "componentId", componentId, "action", action, "path", path));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1558:    private static CreatorRuntimeBlock interstitialCall(String componentId, String action,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1559:                                                        Map<String, Object> componentDescriptors) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1561:        arguments.put("componentId", componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1563:        Object raw = componentDescriptors == null ? null : componentDescriptors.get(componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1568:        return serviceCall("ads_interstitial", arguments);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1591:        return serviceCall("map", arguments);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1594:    private static CreatorRuntimeBlock calendarCall(String componentId, String action, String key, String value) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1596:        arguments.put("componentId", componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1603:        return serviceCall("calendar", arguments);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1606:    private static CreatorRuntimeBlock bluetoothCall(String componentId, String action, String uuid, String address, String tag) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1608:        arguments.put("componentId", componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1613:        return serviceCall("bluetooth", arguments);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1625:            BlockBean block = byId.get(id);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1626:            if (block == null || blank(block.opCode)) return null;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1629:            result.put("opCode", block.opCode.trim().toLowerCase(Locale.ROOT));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1630:            result.put("spec", block.spec == null ? "" : block.spec);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1631:            result.put("type", block.type == null ? "" : block.type);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1633:            for (String argument : block.parameters == null ? Collections.<String>emptyList() : block.parameters) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1650:    private static CreatorRuntimeBlock storageCall(String componentId, String action, String key, String value,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1651:                                                   String explicitStoreName, Map<String, Object> componentDescriptors) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1653:        arguments.put("componentId", componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1655:        Object raw = componentDescriptors == null ? null : componentDescriptors.get(componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1664:        return serviceCall("local_storage", arguments);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1668:    private static String firebasePath(Map<String, Object> componentDescriptors, String componentId, String childPath) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1669:        Object raw = componentDescriptors == null ? null : componentDescriptors.get(componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1679:    private static CreatorRuntimeBlock widgetProperty(BlockBean block, List<String> values, String property,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1681:        if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1689:    private static CreatorRuntimeBlock widgetProperty(BlockBean block, List<String> values, String property,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1691:        if (values.size() < 2) { unsupported.add(block.opCode); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1697:            if (expression == null) { unsupported.add(block.opCode + " (invalid value expression)"); return null; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1703:    private static String normalizeEventName(String eventName) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyArtifactImporter.java:1704:        String normalized = eventName.trim().toLowerCase(Locale.ROOT);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyViewImporter.java:64:                        "No Creator Runtime widget mapping exists; import is blocked rather than using fallback execution.");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyViewImporter.java:77:            widgets.put(parentId, widgets.get(parentId).withChild(widget.getId(), view.index));
app/src/main/java/pro/sketchware/creator/runtime/CreatorMapService.java:60:            return () -> entry.map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));
app/src/main/java/pro/sketchware/creator/runtime/CreatorMapService.java:64:            return () -> entry.map.moveCamera(CameraUpdateFactory.zoomTo(zoom));
app/src/main/java/pro/sketchware/creator/runtime/CreatorMapService.java:66:        if ("zoom_in".equals(action)) return () -> entry.map.moveCamera(CameraUpdateFactory.zoomIn());
app/src/main/java/pro/sketchware/creator/runtime/CreatorMapService.java:67:        if ("zoom_out".equals(action)) return () -> entry.map.moveCamera(CameraUpdateFactory.zoomOut());
app/src/main/java/pro/sketchware/creator/runtime/CreatorNetworkService.java:15:/** Runtime-native asynchronous HTTP service for the legacy RequestNetwork component. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorNetworkService.java:17:    interface EventPublisher { void publish(String serviceId, String eventName, Map<String, Object> payload); }
app/src/main/java/pro/sketchware/creator/runtime/CreatorNetworkService.java:24:        this((serviceId, eventName, payload) -> environment.publish(serviceId, eventName, payload), new OkHttpClient());
app/src/main/java/pro/sketchware/creator/runtime/CreatorNetworkService.java:28:        this((serviceId, eventName, payload) -> environment.publish(serviceId, eventName, payload), client);
app/src/main/java/pro/sketchware/creator/runtime/CreatorNetworkService.java:42:            String componentId = CreatorRuntimeServiceArguments.string(arguments, "componentId");
app/src/main/java/pro/sketchware/creator/runtime/CreatorNetworkService.java:43:            if (componentId == null) return CreatorRuntimeServiceArguments.invalid("RequestNetwork action requires componentId.");
app/src/main/java/pro/sketchware/creator/runtime/CreatorNetworkService.java:44:            Configuration configuration = configurations.get(componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorNetworkService.java:47:                configurations.put(componentId, configuration);
app/src/main/java/pro/sketchware/creator/runtime/CreatorNetworkService.java:52:                return CreatorRuntimeServiceArguments.succeeded("configured", true, "componentId", componentId, "action", action);
app/src/main/java/pro/sketchware/creator/runtime/CreatorNetworkService.java:56:                return CreatorRuntimeServiceArguments.succeeded("configured", true, "componentId", componentId, "action", action);
app/src/main/java/pro/sketchware/creator/runtime/CreatorNetworkService.java:77:            boolean requestBody = "REQUEST_BODY".equalsIgnoreCase(requestType) || "1".equals(requestType);
app/src/main/java/pro/sketchware/creator/runtime/CreatorNotificationService.java:13:/** Runtime-native local notification service for the legacy Notification component. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeBlock.java:9:/** One visible, serializable behavior block in a Creator Runtime event binding. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:12:/** Executes an attached event binding using only typed operations and visible effects. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:40:    public List<Effect> dispatch(CreatorRuntimeEngine engine, String targetWidgetId, String eventName) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:42:        CreatorEventBinding binding = findBinding(engine.getCurrent(), targetWidgetId, eventName);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:49:            effects.add(new Effect("return", "ignored_outside_more_block"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:56:    private Flow executeBlocks(CreatorRuntimeEngine engine, List<CreatorRuntimeBlock> blocks, List<Effect> effects) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:57:        for (CreatorRuntimeBlock block : blocks) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:58:            Map<String, Object> payload = block.getPayload();
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:59:            if (block.getType() == CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:63:            } else if (block.getType() == CreatorRuntimeBlock.Type.SET_STATE) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:66:            } else if (block.getType() == CreatorRuntimeBlock.Type.INCREMENT_STATE) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:72:            } else if (block.getType() == CreatorRuntimeBlock.Type.LIST_MUTATE) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:109:            } else if (block.getType() == CreatorRuntimeBlock.Type.MAP_MUTATE) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:127:            } else if (block.getType() == CreatorRuntimeBlock.Type.ATTACH_EVENT
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:128:                    || block.getType() == CreatorRuntimeBlock.Type.REPLACE_EVENT) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:129:                apply(engine, block.getType() == CreatorRuntimeBlock.Type.ATTACH_EVENT
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:134:                                "eventName", payload.get("eventName"),
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:135:                                "blocks", block.getThenBlocks()));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:136:            } else if (block.getType() == CreatorRuntimeBlock.Type.DETACH_EVENT) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:139:            } else if (block.getType() == CreatorRuntimeBlock.Type.SHOW_MESSAGE) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:141:            } else if (block.getType() == CreatorRuntimeBlock.Type.NAVIGATE) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:143:            } else if (block.getType() == CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:159:            } else if (block.getType() == CreatorRuntimeBlock.Type.CUSTOM_FUNCTION_CALL) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:162:            } else if (block.getType() == CreatorRuntimeBlock.Type.RETURN) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:169:            } else if (block.getType() == CreatorRuntimeBlock.Type.IF_STATE_EQUALS) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:174:                Flow childFlow = executeBlocks(engine, matches ? block.getThenBlocks() : block.getElseBlocks(), effects);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:176:            } else if (block.getType() == CreatorRuntimeBlock.Type.IF_BOOLEAN) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:181:                Flow childFlow = executeBlocks(engine, matches ? block.getThenBlocks() : block.getElseBlocks(), effects);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:183:            } else if (block.getType() == CreatorRuntimeBlock.Type.REPEAT) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:191:                    Flow childFlow = executeBlocks(engine, block.getThenBlocks(), effects);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:195:            } else if (block.getType() == CreatorRuntimeBlock.Type.FOREVER) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:198:                    Flow childFlow = executeBlocks(engine, block.getThenBlocks(), effects);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:206:            } else if (block.getType() == CreatorRuntimeBlock.Type.BREAK) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:213:    private CreatorEventBinding findBinding(CreatorProjectDocument document, String targetWidgetId, String eventName) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:215:            if (binding.getTargetWidgetId().equals(targetWidgetId) && binding.getEventName().equals(eventName)) return binding;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:265:            if ("true".equalsIgnoreCase(literal) || "false".equalsIgnoreCase(literal)) return Boolean.valueOf(literal);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:518:            effects.add(new Effect("more_block", "depth_capped:" + functionId));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:525:        CreatorEventBinding binding = engine.getCurrent().getEvents().get("legacy_moreblock_" + functionId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:563:        if ("boolean".equalsIgnoreCase(type) || "b".equalsIgnoreCase(type)) return false;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:564:        if ("double".equalsIgnoreCase(type) || "d".equalsIgnoreCase(type)) return 0d;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:583:    private Object calendarTimestamp(Object componentId) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:584:        if (runtimeServices == null || componentId == null) return null;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:586:                CreatorRuntimeServiceArguments.output("componentId", String.valueOf(componentId), "action", "get_time"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:590:    private Object animatorValue(Object componentId, String action, String outputKey) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:591:        if (runtimeServices == null || componentId == null) return null;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:593:                CreatorRuntimeServiceArguments.output("componentId", String.valueOf(componentId), "action", action));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:597:    private Object textToSpeechValue(Object componentId, String action, String outputKey) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:598:        if (runtimeServices == null || componentId == null) return null;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:600:                CreatorRuntimeServiceArguments.output("componentId", String.valueOf(componentId), "action", action));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:604:    private Object storageValue(Object componentId, Object key, String action, String outputKey) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:605:        if (runtimeServices == null || componentId == null || key == null) return null;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:607:                CreatorRuntimeServiceArguments.output("componentId", String.valueOf(componentId), "action", action,
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:627:    private Object bluetoothValue(Object componentId, String action, String outputKey) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:628:        if (runtimeServices == null || componentId == null) return null;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:630:                CreatorRuntimeServiceArguments.output("componentId", String.valueOf(componentId), "action", action));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:635:    private Object firebaseValue(CreatorRuntimeEngine engine, Object componentId, String action, String outputKey) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:636:        if (runtimeServices == null || componentId == null) return null;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:637:        String id = String.valueOf(componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:639:        Object rawComponents = engine.getCurrent().getState().get("legacy.components");
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:650:                CreatorRuntimeServiceArguments.output("componentId", id, "action", action, "path", path));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExecutor.java:690:        return value instanceof Boolean ? (Boolean) value : "true".equalsIgnoreCase(String.valueOf(value));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:65:            case "attach_event":
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:69:                copy(args, payload, "event_name", "eventName");
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:70:                payload.put("blocks", blocks(args));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:72:            case "replace_event":
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:76:                copy(args, payload, "event_name", "eventName");
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:77:                payload.put("blocks", blocks(args));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:79:            case "detach_event":
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:107:    private static java.util.List<CreatorRuntimeBlock> blocks(JsonObject args) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:108:        return args.has("blocks") && args.get("blocks").isJsonArray()
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:109:                ? blocks(args.getAsJsonArray("blocks")) : new ArrayList<CreatorRuntimeBlock>();
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:112:    private static java.util.List<CreatorRuntimeBlock> blocks(com.google.gson.JsonArray source) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:115:            if (!element.isJsonObject()) throw new IllegalArgumentException("each block must be an object");
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:116:            JsonObject block = element.getAsJsonObject();
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:117:            String type = requiredString(block, "type");
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:118:            Map<String, Object> payload = jsonObjectToMap(block);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:120:            payload.remove("then_blocks");
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:121:            payload.remove("else_blocks");
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:122:            java.util.List<CreatorRuntimeBlock> thenBlocks = block.has("then_blocks") && block.get("then_blocks").isJsonArray()
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:123:                    ? blocks(block.getAsJsonArray("then_blocks")) : new ArrayList<CreatorRuntimeBlock>();
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:124:            java.util.List<CreatorRuntimeBlock> elseBlocks = block.has("else_blocks") && block.get("else_blocks").isJsonArray()
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeOperationMapper.java:125:                    ? blocks(block.getAsJsonArray("else_blocks")) : new ArrayList<CreatorRuntimeBlock>();
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeResourceValues.java:14:    public static String resolveColor(CreatorProjectDocument document, String value) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeResourceValues.java:15:        return resolveColor(document, value, "");
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeResourceValues.java:22:    public static String resolveColor(CreatorProjectDocument document, String value, String variant) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeServiceCatalog.java:8:public final class CreatorRuntimeServiceCatalog {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeServiceCatalog.java:11:    private CreatorRuntimeServiceCatalog(Set<String> serviceIds) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeServiceCatalog.java:15:    public static CreatorRuntimeServiceCatalog defaults() {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeServiceCatalog.java:16:        Set<String> values = new LinkedHashSet<>(CreatorRuntimeComponentServiceMatrix.all().values());
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeServiceCatalog.java:18:        return new CreatorRuntimeServiceCatalog(values);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeServices.java:42:                .register(new CreatorFirebaseCloudMessageService(environment))
app/src/main/java/pro/sketchware/creator/runtime/CreatorSpeechToTextService.java:80:    @Override public void onEvent(int eventType, Bundle params) { }
app/src/main/java/pro/sketchware/creator/runtime/CreatorStorageService.java:10:/** Runtime-native implementation of the legacy SharedPreferences component. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorStorageService.java:36:            String componentId = CreatorRuntimeServiceArguments.string(arguments, "componentId");
app/src/main/java/pro/sketchware/creator/runtime/CreatorStorageService.java:38:            if (componentId == null || componentId.trim().isEmpty() || storeName == null || storeName.trim().isEmpty()) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorStorageService.java:39:                return new Result(Status.UNSUPPORTED_ARGUMENT, Collections.emptyMap(), "Storage configuration requires componentId and storeName.");
app/src/main/java/pro/sketchware/creator/runtime/CreatorStorageService.java:41:            configuredStores.put(componentId, storeName.trim());
app/src/main/java/pro/sketchware/creator/runtime/CreatorStorageService.java:43:                    "componentId", componentId, "storeName", storeName.trim()), null);
app/src/main/java/pro/sketchware/creator/runtime/CreatorStorageService.java:67:        String componentId = CreatorRuntimeServiceArguments.string(arguments, "componentId");
app/src/main/java/pro/sketchware/creator/runtime/CreatorStorageService.java:68:        String configured = componentId == null ? null : configuredStores.get(componentId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorTextToSpeechService.java:8:/** Runtime-native TextToSpeech component with explicit initialization status. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorTimerService.java:11:/** Runtime-native scheduler for the legacy TimerTask component. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorUiService.java:8:/** Runtime-native activity title and clipboard operations used by legacy UI blocks. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorWidgetQueryService.java:430:            if (color.startsWith("#")) return Color.parseColor(color);
app/src/main/java/pro/sketchware/creator/runtime/CreatorCameraService.java:9:/** Runtime-native Camera component using Android's capture intent. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorCompatibilityAnalyzer.java:7:    private final CreatorRuntimeServiceCatalog services;
app/src/main/java/pro/sketchware/creator/runtime/CreatorCompatibilityAnalyzer.java:9:    public CreatorCompatibilityAnalyzer(CreatorRuntimeServiceCatalog services) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorCompatibilityAnalyzer.java:10:        this.services = services == null ? CreatorRuntimeServiceCatalog.defaults() : services;
app/src/main/java/pro/sketchware/creator/runtime/CreatorCompatibilityAnalyzer.java:16:        if (normalized.startsWith("widget:") || normalized.startsWith("block:")
app/src/main/java/pro/sketchware/creator/runtime/CreatorEventBinding.java:7:/** A user-inspectable widget event and its ordered block list. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorEventBinding.java:11:    private final String eventName;
app/src/main/java/pro/sketchware/creator/runtime/CreatorEventBinding.java:12:    private final List<CreatorRuntimeBlock> blocks;
app/src/main/java/pro/sketchware/creator/runtime/CreatorEventBinding.java:14:    public CreatorEventBinding(String id, String targetWidgetId, String eventName, List<CreatorRuntimeBlock> blocks) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorEventBinding.java:16:                || eventName == null || eventName.trim().isEmpty()) throw new IllegalArgumentException("binding fields");
app/src/main/java/pro/sketchware/creator/runtime/CreatorEventBinding.java:19:        this.eventName = eventName;
app/src/main/java/pro/sketchware/creator/runtime/CreatorEventBinding.java:20:        this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks == null
app/src/main/java/pro/sketchware/creator/runtime/CreatorEventBinding.java:21:                ? Collections.<CreatorRuntimeBlock>emptyList() : blocks));
app/src/main/java/pro/sketchware/creator/runtime/CreatorEventBinding.java:25:    public String getEventName() { return eventName; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorEventBinding.java:26:    public List<CreatorRuntimeBlock> getBlocks() { return blocks; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseAuthPhoneService.java:4:import com.google.firebase.auth.PhoneAuthCredential;
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseAuthPhoneService.java:26:                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseAuthPhoneService.java:27:                        @Override public void onVerificationCompleted(PhoneAuthCredential credential) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseAuthPhoneService.java:28:                            auth.signInWithCredential(credential).addOnSuccessListener(result -> environment.publish(getId(), "signed_in",
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseAuthPhoneService.java:46:            auth.signInWithCredential(PhoneAuthProvider.getCredential(id, code))
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseCloudMessageService.java:7:public final class CreatorFirebaseCloudMessageService implements CreatorRuntimeService {
app/src/main/java/pro/sketchware/creator/runtime/CreatorFirebaseCloudMessageService.java:9:    public CreatorFirebaseCloudMessageService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorGyroscopeService.java:44:    @Override public void onSensorChanged(SensorEvent event) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorGyroscopeService.java:45:        if (!listening || event == null || event.values == null || event.values.length < 3) return;
app/src/main/java/pro/sketchware/creator/runtime/CreatorGyroscopeService.java:47:                "x", event.values[0], "y", event.values[1], "z", event.values[2],
app/src/main/java/pro/sketchware/creator/runtime/CreatorGyroscopeService.java:48:                "timestamp", event.timestamp));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLocationService.java:11:/** Runtime-native LocationManager component with start/stop updates and explicit permission state. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationReducer.java:17:        Map<String, CreatorEventBinding> events = new LinkedHashMap<>(document.getEvents());
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationReducer.java:43:                widgets.put(parentId, widgets.get(parentId).withChild(widgetId, index));
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationReducer.java:54:                removeWidgetTree(widgets, events, widgetId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationReducer.java:69:                events.put((String) payload.get("bindingId"), new CreatorEventBinding(
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationReducer.java:71:                        (String) payload.get("eventName"),
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationReducer.java:72:                        (java.util.List<CreatorRuntimeBlock>) payload.get("blocks")));
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationReducer.java:75:                events.remove((String) payload.get("bindingId"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationReducer.java:83:                document.getName(), entryScreenId, screens, widgets, entryControl, state, events);
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationReducer.java:87:                                          Map<String, CreatorEventBinding> events,
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationReducer.java:92:            removeWidgetTree(widgets, events, childId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationReducer.java:99:        java.util.Iterator<Map.Entry<String, CreatorEventBinding>> iterator = events.entrySet().iterator();
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationValidator.java:152:        String eventName = string(payload, "eventName");
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationValidator.java:153:        if (bindingId == null || targetWidgetId == null || eventName == null
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationValidator.java:154:                || !(payload.get("blocks") instanceof java.util.List)) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorOperationValidator.java:155:            return invalid("bindingId, targetWidgetId, eventName and blocks are required");
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocument.java:23:    private final Map<String, CreatorEventBinding> events;
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocument.java:40:                                  Map<String, CreatorEventBinding> events) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocument.java:57:        this.events = Collections.unmodifiableMap(new LinkedHashMap<>(events == null
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocument.java:58:                ? Collections.<String, CreatorEventBinding>emptyMap() : events));
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocument.java:76:    public Map<String, CreatorEventBinding> getEvents() { return events; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocument.java:83:                nextEntryScreenId, nextScreens, nextWidgets, nextEntryControl, state, events);
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:61:        JsonArray events = new JsonArray();
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:63:            JsonObject event = new JsonObject();
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:64:            event.addProperty("id", binding.getId());
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:65:            event.addProperty("targetWidgetId", binding.getTargetWidgetId());
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:66:            event.addProperty("eventName", binding.getEventName());
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:67:            event.add("blocks", encodeBlocks(binding.getBlocks()));
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:68:            events.add(event);
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:70:        root.add("events", events);
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:114:        Map<String, CreatorEventBinding> events = new LinkedHashMap<>();
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:115:        for (JsonElement item : arrayOrEmpty(root, "events")) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:116:            JsonObject event = item.getAsJsonObject();
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:117:            List<CreatorRuntimeBlock> blocks = decodeBlocks(arrayOrEmpty(event, "blocks"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:118:            CreatorEventBinding binding = new CreatorEventBinding(required(event, "id").getAsString(),
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:119:                    required(event, "targetWidgetId").getAsString(), required(event, "eventName").getAsString(), blocks);
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:120:            events.put(binding.getId(), binding);
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:123:                screens, widgets, entryControl, state, events);
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:137:    private static JsonArray encodeBlocks(List<CreatorRuntimeBlock> blocks) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:139:        for (CreatorRuntimeBlock block : blocks) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:141:            value.addProperty("type", block.getType().name());
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:142:            value.add("payload", toJsonObject(block.getPayload()));
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:143:            value.add("thenBlocks", encodeBlocks(block.getThenBlocks()));
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:144:            value.add("elseBlocks", encodeBlocks(block.getElseBlocks()));
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:151:        List<CreatorRuntimeBlock> blocks = new ArrayList<>();
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:153:            JsonObject block = item.getAsJsonObject();
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:154:            CreatorRuntimeBlock.Type type = CreatorRuntimeBlock.Type.valueOf(required(block, "type").getAsString());
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:155:            Map<String, Object> payload = block.has("payload") && block.get("payload").isJsonObject()
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:156:                    ? fromJsonObject(block.getAsJsonObject("payload")) : new LinkedHashMap<String, Object>();
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:157:            blocks.add(new CreatorRuntimeBlock(type, payload,
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:158:                    decodeBlocks(arrayOrEmpty(block, "thenBlocks")), decodeBlocks(arrayOrEmpty(block, "elseBlocks"))));
app/src/main/java/pro/sketchware/creator/runtime/CreatorProjectDocumentCodec.java:160:        return blocks;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRewardedAdService.java:13:/** Runtime-native rewarded-ad service with explicit reward event publishing. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorRewardedAdService.java:62:    private void publish(String event) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRewardedAdService.java:63:        environment.publish(getId(), event, CreatorRuntimeServiceArguments.output("adUnitId", loadedUnitId));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeCapability.java:4:public enum CreatorRuntimeCapability {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeCapability.java:12:    CreatorRuntimeCapability(String permission) { this.permission = permission; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeCompatibilityInspector.java:4:public final class CreatorRuntimeCompatibilityInspector {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeCompatibilityInspector.java:5:    private CreatorRuntimeCompatibilityInspector() { }
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeCompatibilityInspector.java:23:                    : "No Creator Runtime renderer is registered; execution is blocked rather than falling back.";
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeComponentServiceMatrix.java:9:public final class CreatorRuntimeComponentServiceMatrix {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeComponentServiceMatrix.java:45:    private CreatorRuntimeComponentServiceMatrix() { }
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeComponentServiceMatrix.java:46:    public static String serviceFor(int componentType) { return SERVICES.get(componentType); }
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeCondition.java:7:public final class CreatorRuntimeCondition {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeCondition.java:8:    private CreatorRuntimeCondition() { }
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeCondition.java:51:            if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeCondition.java:52:            if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEngine.java:12:    private final CreatorRuntimeEventLog eventLog;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEngine.java:15:                                CreatorRuntimeEventLog eventLog) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEngine.java:17:        this.eventLog = eventLog == null ? new CreatorRuntimeEventLog(200) : eventLog;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEngine.java:66:            eventLog.append(new CreatorRuntimeEvent(System.currentTimeMillis(), current.getProjectId(), current.getRevision(),
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEngine.java:75:    public CreatorRuntimeEventLog getEventLog() { return eventLog; }
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEngine.java:80:        eventLog.append(new CreatorRuntimeEvent(System.currentTimeMillis(), document.getProjectId(), document.getRevision(),
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEnvironment.java:19:        void onServiceEvent(String serviceId, String eventName, Map<String, Object> payload);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEnvironment.java:30:        final String eventName;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEnvironment.java:31:        PendingAction(String serviceId, String eventName) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEnvironment.java:33:            this.eventName = eventName;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEnvironment.java:52:    public void publish(String serviceId, String eventName, Map<String, Object> payload) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEnvironment.java:54:        listener.onServiceEvent(serviceId, eventName, Collections.unmodifiableMap(
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEnvironment.java:68:    public void launchForResult(String serviceId, String eventName, Intent intent) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEnvironment.java:70:        pendingActions.put(requestCode, new PendingAction(serviceId, eventName));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEnvironment.java:81:        publish(pending.serviceId, pending.eventName, result);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEnvironment.java:90:        publish(pending.serviceId, pending.eventName, CreatorRuntimeServiceArguments.output(
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEvent.java:7:/** Privacy-safe diagnostic event generated by Creator Runtime. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEventLog.java:14:    private final List<CreatorRuntimeEvent> events = new ArrayList<>();
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEventLog.java:21:    public synchronized void append(CreatorRuntimeEvent event) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEventLog.java:22:        if (event == null) return;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEventLog.java:23:        if (events.size() == capacity) events.remove(0);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEventLog.java:24:        events.add(new CreatorRuntimeEvent(event.getTimestampEpochMs(), event.getProjectId(), event.getRevision(),
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEventLog.java:25:                event.getCategory(), event.getName(), event.getSeverity(), event.getCorrelationId(),
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEventLog.java:26:                redact(event.getAttributes())));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeEventLog.java:30:        return Collections.unmodifiableList(new ArrayList<>(events));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExpression.java:8:/** Evaluates a deliberately small, typed expression language used by imported blocks. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExpression.java:60:        if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExpression.java:61:        if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeExpression.java:161:        return value != null && !String.valueOf(value).isEmpty() && !"false".equalsIgnoreCase(String.valueOf(value));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimePermissionBridge.java:13:    private final Set<CreatorRuntimeCapability> supported;
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimePermissionBridge.java:14:    private final Set<CreatorRuntimeCapability> granted = EnumSet.noneOf(CreatorRuntimeCapability.class);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimePermissionBridge.java:16:    public CreatorRuntimePermissionBridge(Set<CreatorRuntimeCapability> supported) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimePermissionBridge.java:17:        this.supported = supported == null ? Collections.<CreatorRuntimeCapability>emptySet()
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimePermissionBridge.java:21:    public Outcome check(CreatorRuntimeCapability capability, boolean hasHost) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimePermissionBridge.java:27:    public Outcome resolve(CreatorRuntimeCapability capability, boolean userGranted) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeProjectStore.java:29:                // A corrupt local draft must not block Creator Home. The next
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeResourceResolver.java:17:                    Object store = SketchwareApi.invokeStatic("a.a.a.jC", "d", id);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeSession.java:43:    public synchronized CreatorProjectDocument importLegacySnapshot(CreatorProjectDocument imported) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorVibratorService.java:10:/** Runtime-native implementation of the legacy Vibrator component. */
app/src/main/java/pro/sketchware/creator/runtime/CreatorWidget.java:36:    public CreatorWidget withChild(String childId, int index) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:16:import a.a.a.eC;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:17:import a.a.a.hC;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:19:import a.a.a.jC;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:29:import com.besome.sketch.beans.ProjectFileBean;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:42: * ProjectBean/ProjectFileBean/ViewBean/BlockBean code. This bridge only
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:54:    public static synchronized String ensureLegacyProject(Context context,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:63:            ensureLegacyStores(context, existingScId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:64:            ensureLegacyStarterIntent(context, document, existingScId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:65:            projectRuntimeViews(context, document, existingScId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:71:        ensureLegacyStores(context, scId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:72:        ensureLegacyStarterIntent(context, document, scId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:73:        projectRuntimeViews(context, document, scId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:108:     * and then invokes eC's normal view serialization.
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:110:    public static synchronized void projectRuntimeViews(Context context,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:114:        eC viewStore = jC.a(scId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:116:            projectScreen(viewStore, document, null, "main.xml");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:120:                projectScreen(viewStore, document, screen, screen.getId() + ".xml");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:123:        viewStore.n(wq.b(scId) + File.separator + "view");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:126:    private static void projectScreen(eC viewStore, CreatorProjectDocument document,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:139:            // The binary eC view store assumes parent is non-null and calls
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:154:        viewStore.c.put(xmlName, projected);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:169:    public static synchronized CreatorProjectDocument importLegacyProject(Context context,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:173:        eC viewStore = jC.a(scId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:174:        hC fileStore = jC.b(scId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:175:        ArrayList<ProjectFileBean> files = fileStore == null ? new ArrayList<>() : fileStore.b();
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:177:        if (files.isEmpty()) files.add(new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:181:        ArrayList<ComponentBean> components = new ArrayList<>();
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:182:        ArrayList<EventBean> events = new ArrayList<>();
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:183:        Map<String, List<BlockBean>> blocksByEvent = new java.util.LinkedHashMap<>();
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:184:        for (ProjectFileBean file : files) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:190:                    viewStore.d(file.getXmlName()), file.isActivityLocked());
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:194:            ArrayList<ComponentBean> fileComponents = viewStore.e(javaName);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:195:            if (fileComponents != null) components.addAll(fileComponents);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:196:            ArrayList<EventBean> fileEvents = viewStore.g(javaName);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:198:                events.addAll(fileEvents);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:199:                for (EventBean event : fileEvents) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:200:                    if (event != null) blocksByEvent.put(event.getEventKey(), viewStore.a(javaName, event.getEventKey()));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:210:                importedDocument, components, events, blocksByEvent);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:223:        kC resourceStore = jC.d(scId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:244:                    // A missing optional value resource must not block the editor.
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:253:        iC libraries = jC.c(scId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:382:     * Metadata alone is insufficient: DesignActivity immediately asks hC for
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:386:    private static void ensureLegacyStores(Context context, String scId) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:391:        hC fileStore = jC.b(scId, false);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:392:        if (fileStore.b(ProjectFileBean.DEFAULT_XML_NAME) == null) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:393:            fileStore.a(new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main"));
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:395:        ProjectFileBean editorFile = fileStore.b("editor.xml");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:397:            editorFile = new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "editor");
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:399:                    | ProjectFileBean.OPTION_ACTIVITY_LOCKED);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:402:        // hC.j() rebuilds derived XML/Java name lists; hC.l() persists the
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:403:        // updated ProjectFileBean list to the legacy `file` store.
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:408:        // requests main.xml. Its normal eC initializer creates the empty view
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:410:        jC.a(scId, false);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:414:     * Seeds the original editor with the same Intent component and click blocks
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:417:    private static void ensureLegacyStarterIntent(Context context, CreatorProjectDocument document, String scId) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:425:        eC viewStore = jC.a(scId);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:426:        String javaName = ProjectFileBean.DEFAULT_JAVA_NAME;
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:427:        ProjectFileBean mainFile = jC.b(scId).b(ProjectFileBean.DEFAULT_XML_NAME);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:432:        ArrayList<ComponentBean> components = viewStore.e(javaName);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:433:        if (components != null) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:434:            for (ComponentBean component : components) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:435:                if (component != null && CreatorRuntimeDefaults.EDITOR_INTENT_ID.equals(component.componentId)) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:442:            viewStore.b(javaName, new ComponentBean(ComponentBean.COMPONENT_TYPE_INTENT,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:447:        ArrayList<EventBean> events = viewStore.g(javaName);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:448:        if (events != null) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:449:            for (EventBean event : events) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:450:                if (event != null && CreatorRuntimeDefaults.ENTRY_WIDGET_ID.equals(event.targetId)
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:451:                        && "onClick".equals(event.eventName)) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:458:            EventBean event = new EventBean(EventBean.EVENT_TYPE_VIEW,
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:460:            viewStore.a(javaName, event);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:461:            ArrayList<BlockBean> blocks = new ArrayList<>();
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:470:            blocks.add(setScreen);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:471:            blocks.add(startActivity);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:472:            viewStore.a(javaName, event.getEventKey(), blocks);
app/src/main/java/pro/sketchware/creator/runtime/CreatorLegacyProjectBridge.java:474:        viewStore.n(wq.b(scId) + File.separator + "view");
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeDefaults.java:43:        Map<String, CreatorEventBinding> events = new LinkedHashMap<>(document.getEvents());
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeDefaults.java:115:                widgets.put(rootId, root.withChild(ENTRY_WIDGET_ID, -1));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeDefaults.java:118:        if (!hasClickBinding(events, ENTRY_WIDGET_ID)) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeDefaults.java:125:            List<CreatorRuntimeBlock> blocks = new ArrayList<>();
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeDefaults.java:126:            blocks.add(new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL, payload));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeDefaults.java:127:            events.put(ENTRY_CLICK_BINDING_ID, new CreatorEventBinding(
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeDefaults.java:128:                    ENTRY_CLICK_BINDING_ID, ENTRY_WIDGET_ID, "click", blocks));
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeDefaults.java:135:                document.getEntryControl(), state, events);
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeDefaults.java:155:    private static boolean hasClickBinding(Map<String, CreatorEventBinding> events, String widgetId) {
app/src/main/java/pro/sketchware/creator/runtime/CreatorRuntimeDefaults.java:156:        for (CreatorEventBinding binding : events.values()) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:49:import com.besome.sketch.beans.ProjectFileBean;
app/src/main/java/com/besome/sketch/design/DesignActivity.java:51:import com.besome.sketch.editor.manage.ManageCollectionActivity;
app/src/main/java/com/besome/sketch/design/DesignActivity.java:66:import com.google.firebase.crashlytics.FirebaseCrashlytics;
app/src/main/java/com/besome/sketch/design/DesignActivity.java:86:import a.a.a.eC;
app/src/main/java/com/besome/sketch/design/DesignActivity.java:87:import a.a.a.jC;
app/src/main/java/com/besome/sketch/design/DesignActivity.java:102:import mod.hey.studios.project.custom_blocks.CustomBlocksDialog;
app/src/main/java/com/besome/sketch/design/DesignActivity.java:135:    private final FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:149:    private ProjectFileBean projectFile;
app/src/main/java/com/besome/sketch/design/DesignActivity.java:175:    private rs eventTabAdapter;
app/src/main/java/com/besome/sketch/design/DesignActivity.java:176:    private br componentTabAdapter;
app/src/main/java/com/besome/sketch/design/DesignActivity.java:179:        String runtimeProjectId = getIntent().getStringExtra("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/design/DesignActivity.java:183:        CreatorLegacyProjectBridge.projectRuntimeViews(this, document, sc_id);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:186:        refreshComponentsForAi();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:223:                ? null : getIntent().getStringExtra("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/design/DesignActivity.java:257:        jC.a(sc_id, haveSavedState);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:258:        jC.b(sc_id, haveSavedState);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:259:        kC var2 = jC.d(sc_id, haveSavedState);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:260:        jC.c(sc_id, haveSavedState);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:270:    private ProjectFileBean getDefaultProjectFile() {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:271:        return jC.b(sc_id).b(ProjectFileBean.DEFAULT_XML_NAME);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:287:            if (!ProjectFileBean.DEFAULT_XML_NAME.equals(xmlFileName) && jC.b(sc_id).b(xmlFileName) == null) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:289:                xmlFileName = ProjectFileBean.DEFAULT_XML_NAME;
app/src/main/java/com/besome/sketch/design/DesignActivity.java:293:            if (!ProjectFileBean.DEFAULT_JAVA_NAME.equals(currentJavaFileName) && jC.b(sc_id).a(currentJavaFileName) == null) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:295:                currentJavaFileName = ProjectFileBean.DEFAULT_JAVA_NAME;
app/src/main/java/com/besome/sketch/design/DesignActivity.java:304:            if (orientation == ProjectFileBean.ORIENTATION_PORTRAIT) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:306:            } else if (orientation == ProjectFileBean.ORIENTATION_LANDSCAPE) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:316:     * Returns the currently active ProjectFileBean in the editor, or null
app/src/main/java/com/besome/sketch/design/DesignActivity.java:321:    public ProjectFileBean getCurrentProjectFile() {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:337:     * ViewBeans collection via {@code jC.a(sc_id).d(xmlName)} and re-inits
app/src/main/java/com/besome/sketch/design/DesignActivity.java:360:                    ProjectFileBean bean = jC.b(sc_id).b(xmlName);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:387:    public void refreshComponentsForAi() {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:388:        runOnUiThread(this::refreshComponentTabAdapter);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:398:        if (eventTabAdapter != null && projectFile != null) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:399:            eventTabAdapter.setCurrentActivity(projectFile);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:400:            eventTabAdapter.refreshEvents();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:404:    private void refreshComponentTabAdapter() {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:405:        if (componentTabAdapter != null && projectFile != null) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:406:            componentTabAdapter.setProjectFile(projectFile);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:407:            componentTabAdapter.refreshData();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:417:            refreshComponentTabAdapter();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:434:    private void indicateCompileErrorOccurred(String error) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:452:        // SaveChangesProjectCloser/ProjectSaver persist the legacy stores before
app/src/main/java/com/besome/sketch/design/DesignActivity.java:457:            importLegacyRuntimeBoundary();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:459:        jC.a();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:472:    private void prepareCreatorRuntimeLaunch() {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:476:                || launchIntent.hasExtra("creator_runtime_project_id")) return;
app/src/main/java/com/besome/sketch/design/DesignActivity.java:478:        String legacyScId = CreatorLegacyProjectBridge.ensureLegacyProject(this, document);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:480:        launchIntent.putExtra("creator_runtime_project_id", document.getProjectId());
app/src/main/java/com/besome/sketch/design/DesignActivity.java:484:        if (jC.c(sc_id).g() || jC.b(sc_id).g() || jC.d(sc_id).q() || jC.a(sc_id).d() || jC.a(sc_id).c()) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:551:                saveChangesAndCloseProject();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:562:    private void saveChangesAndCloseProject() {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:564:        SaveChangesProjectCloser saveChangesProjectCloser = new SaveChangesProjectCloser(this);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:565:        saveChangesProjectCloser.execute();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:578:        prepareCreatorRuntimeLaunch();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:590:        syncCreatorRuntimeBoundary();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:680:        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:683:            public void onPageScrollStateChanged(int state) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:693:                    if (eventTabAdapter != null) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:694:                        eventTabAdapter.c();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:696:                } else if (currentTabNumber == 2 && componentTabAdapter != null) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:697:                    componentTabAdapter.unselectAll();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:710:                        if (eventTabAdapter != null) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:711:                            eventTabAdapter.refreshEvents();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:719:                        if (componentTabAdapter != null) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:720:                            componentTabAdapter.refreshData();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:796:            if (eventTabAdapter != null) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:797:                eventTabAdapter.toggleSearchBar();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:832:        importLegacyRuntimeBoundary();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:844:    private void syncCreatorRuntimeBoundary() {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:845:        String runtimeProjectId = getIntent().getStringExtra("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/design/DesignActivity.java:850:            CreatorLegacyProjectBridge.projectRuntimeViews(this, document, sc_id);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:854:    private void importLegacyRuntimeBoundary() {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:855:        String runtimeProjectId = getIntent().getStringExtra("creator_runtime_project_id");
app/src/main/java/com/besome/sketch/design/DesignActivity.java:860:        CreatorProjectDocument imported = CreatorLegacyProjectBridge.importLegacyProject(this, current, sc_id);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:861:        session.importLegacySnapshot(imported);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:902:                    saveChangesAndCloseProject();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:946:                boolean g = jC.c(sc_id).g();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:947:                boolean g2 = jC.b(sc_id).g();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:948:                boolean q = jC.d(sc_id).q();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:949:                boolean d = jC.a(sc_id).d();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:950:                boolean c = jC.a(sc_id).c();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:952:                    jC.c(sc_id).h();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:955:                    jC.b(sc_id).h();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:958:                    jC.d(sc_id).r();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:961:                    jC.a(sc_id).h();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:964:                    jC.a(sc_id).f();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:967:                    jC.b(sc_id).a(jC.c(sc_id));
app/src/main/java/com/besome/sketch/design/DesignActivity.java:968:                    jC.a(sc_id).a(jC.c(sc_id).d());
app/src/main/java/com/besome/sketch/design/DesignActivity.java:971:                    jC.a(sc_id).a(jC.b(sc_id));
app/src/main/java/com/besome/sketch/design/DesignActivity.java:974:                    jC.a(sc_id).c(jC.d(sc_id));
app/src/main/java/com/besome/sketch/design/DesignActivity.java:975:                    jC.a(sc_id).a(jC.d(sc_id));
app/src/main/java/com/besome/sketch/design/DesignActivity.java:995:            var code = new yq(getApplicationContext(), sc_id).getFileSrc(filename, jC.b(sc_id), jC.a(sc_id), jC.c(sc_id));
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1021:            refreshComponentTabAdapter();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1035:            intent.putExtra("creator_runtime_project_id",
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1036:                    getIntent().getStringExtra("creator_runtime_project_id"));
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1051:            var projectDataManager = jC.a(sc_id);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1055:            xmlGenerator.a(eC.a(viewBeans), viewFab);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1076:     * Opens {@link ManageCollectionActivity}.
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1079:        launchActivity(ManageCollectionActivity.class, openCollectionManager);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1196:    void toSourceCodeViewer() {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1213:            intent.putExtra("creator_runtime_project_id",
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1214:                    getIntent().getStringExtra("creator_runtime_project_id"));
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1311:                kC kC = jC.d(sc_id);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1313:                kC = jC.d(sc_id);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1315:                kC = jC.d(sc_id);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1320:                var fileManager = jC.b(sc_id);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1321:                var dataManager = jC.a(sc_id);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1322:                var libraryManager = jC.c(sc_id);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1429:                activity.indicateCompileErrorOccurred(zy.getMessage());
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1433:                activity.indicateCompileErrorOccurred(Log.getStackTraceString(tr));
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1550:                btnRun.setIconTint(ColorStateList.valueOf(ThemeUtils.getColor(context, isRunning ? R.attr.colorOnErrorContainer : R.attr.colorSurfaceContainerLowest)));
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1551:                btnRun.setTextColor(ColorStateList.valueOf(ThemeUtils.getColor(context, isRunning ? R.attr.colorOnErrorContainer : R.attr.colorSurfaceContainerLowest)));
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1605:                jC.d(sc_id).v();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1606:                jC.d(sc_id).w();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1607:                jC.d(sc_id).u();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1632:                jC.d(sc_id).a();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1633:                jC.b(sc_id).m();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1634:                jC.a(sc_id).j();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1635:                jC.d(sc_id).x();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1636:                jC.c(sc_id).l();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1641:                    jC.d(sc_id).f();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1642:                    jC.d(sc_id).g();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1643:                    jC.d(sc_id).e();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1649:    private static class SaveChangesProjectCloser extends BaseTask {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1652:        public SaveChangesProjectCloser(DesignActivity activity) {
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1665:                jC.d(sc_id).a();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1666:                jC.b(sc_id).m();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1667:                jC.a(sc_id).j();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1668:                jC.d(sc_id).x();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1669:                jC.c(sc_id).l();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1670:                jC.d(sc_id).h();
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1695:                eC ecInstance = jC.a(sc_id);
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1710:                    Helper.getResString(R.string.design_tab_title_event),
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1711:                    Helper.getResString(R.string.design_tab_title_component),
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1732:                eventTabAdapter = (rs) fragment;
app/src/main/java/com/besome/sketch/design/DesignActivity.java:1734:                componentTabAdapter = (br) fragment;
