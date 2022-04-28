package com.dexterous.flutterlocalnotifications;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.service.notification.StatusBarNotification;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.core.app.AlarmManagerCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.dexterous.flutterlocalnotifications.models.DateTimeComponents;
import com.dexterous.flutterlocalnotifications.models.NotificationChannelAction;
import com.dexterous.flutterlocalnotifications.models.NotificationChannelDetails;
import com.dexterous.flutterlocalnotifications.models.NotificationChannelGroupDetails;
import com.dexterous.flutterlocalnotifications.models.NotificationDetails;
import com.dexterous.flutterlocalnotifications.models.ScheduledNotificationRepeatFrequency;
import com.dexterous.flutterlocalnotifications.models.styles.BigPictureStyleInformation;
import com.dexterous.flutterlocalnotifications.models.styles.BigTextStyleInformation;
import com.dexterous.flutterlocalnotifications.models.styles.DefaultStyleInformation;
import com.dexterous.flutterlocalnotifications.models.styles.InboxStyleInformation;
import com.dexterous.flutterlocalnotifications.models.styles.MessagingStyleInformation;
import com.dexterous.flutterlocalnotifications.models.styles.StyleInformation;
import com.dexterous.flutterlocalnotifications.utils.BooleanUtils;
import com.dexterous.flutterlocalnotifications.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.threetenabp.AndroidThreeTen;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterLocalNotificationsPlugin
 */
@Keep public class FlutterLocalNotificationsPlugin implements MethodCallHandler, PluginRegistry.NewIntentListener, FlutterPlugin, ActivityAware {
    private static final String SHARED_PREFERENCES_KEY = "notification_plugin_cache";
    private static final String DRAWABLE = "drawable";
    private static final String DEFAULT_ICON = "defaultIcon";
    private static final String SELECT_NOTIFICATION = "SELECT_NOTIFICATION";
    private static final String SCHEDULED_NOTIFICATIONS = "scheduled_notifications";
    private static final String INITIALIZE_METHOD = "initialize";
    private static final String CREATE_NOTIFICATION_CHANNEL_GROUP_METHOD = "createNotificationChannelGroup";
    private static final String DELETE_NOTIFICATION_CHANNEL_GROUP_METHOD = "deleteNotificationChannelGroup";
    private static final String CREATE_NOTIFICATION_CHANNEL_METHOD = "createNotificationChannel";
    private static final String DELETE_NOTIFICATION_CHANNEL_METHOD = "deleteNotificationChannel";
    private static final String GET_ACTIVE_NOTIFICATIONS_METHOD = "getActiveNotifications";
    private static final String PENDING_NOTIFICATION_REQUESTS_METHOD = "pendingNotificationRequests";
    private static final String SHOW_METHOD = "show";
    private static final String CANCEL_METHOD = "cancel";
    private static final String CANCEL_ALL_METHOD = "cancelAll";
    private static final String SCHEDULE_METHOD = "schedule";
    private static final String ZONED_SCHEDULE_METHOD = "zonedSchedule";
    private static final String PERIODICALLY_SHOW_METHOD = "periodicallyShow";
    private static final String SHOW_DAILY_AT_TIME_METHOD = "showDailyAtTime";
    private static final String SHOW_WEEKLY_AT_DAY_AND_TIME_METHOD = "showWeeklyAtDayAndTime";
    private static final String GET_NOTIFICATION_APP_LAUNCH_DETAILS_METHOD = "getNotificationAppLaunchDetails";
    private static final String METHOD_CHANNEL = "dexterous.com/flutter/local_notifications";
    private static final String PAYLOAD = "payload";
    private static final String INVALID_ICON_ERROR_CODE = "INVALID_ICON";
    private static final String INVALID_LARGE_ICON_ERROR_CODE = "INVALID_LARGE_ICON";
    private static final String INVALID_BIG_PICTURE_ERROR_CODE = "INVALID_BIG_PICTURE";
    private static final String INVALID_SOUND_ERROR_CODE = "INVALID_SOUND";
    private static final String INVALID_LED_DETAILS_ERROR_CODE = "INVALID_LED_DETAILS";
    private static final String GET_ACTIVE_NOTIFICATIONS_ERROR_CODE = "GET_ACTIVE_NOTIFICATIONS_ERROR_CODE";
    private static final String GET_ACTIVE_NOTIFICATIONS_ERROR_MESSAGE = "Android version must be 6.0 or newer to use getActiveNotifications";
    private static final String INVALID_LED_DETAILS_ERROR_MESSAGE = "Must specify both ledOnMs and ledOffMs to configure the blink cycle on older versions of Android before Oreo";
    private static final String NOTIFICATION_LAUNCHED_APP = "notificationLaunchedApp";
    private static final String INVALID_DRAWABLE_RESOURCE_ERROR_MESSAGE = "The resource %s could not be found. Please make sure it has been added as a drawable resource to your Android head project.";
    private static final String INVALID_RAW_RESOURCE_ERROR_MESSAGE = "The resource %s could not be found. Please make sure it has been added as a raw resource to your Android head project.";
    private static final String CANCEL_ID = "id";
    private static final String CANCEL_TAG = "tag";
    static String NOTIFICATION_DETAILS = "notificationDetails";
    static Gson gson;
    private MethodChannel channel;
    private Context applicationContext;
    private Activity mainActivity;
    private Intent launchIntent;

    public static void registerWith(Registrar registrar) {
        FlutterLocalNotificationsPlugin plugin = new FlutterLocalNotificationsPlugin();
        plugin.setActivity(registrar.activity());
        registrar.addNewIntentListener(plugin);
        plugin.onAttachedToEngine(registrar.context(), registrar.messenger());
    }

    static void rescheduleNotifications(Context context) {
        initAndroidThreeTen(context);
        ArrayList<NotificationDetails> scheduledNotifications = loadScheduledNotifications(context);
        for (NotificationDetails scheduledNotification : scheduledNotifications) {
            if (scheduledNotification.repeatInterval == null) {
                if (scheduledNotification.timeZoneName == null) {
                    scheduleNotification(context, scheduledNotification, false);
                } else {
                    zonedScheduleNotification(context, scheduledNotification, false);
                }
            } else {
                repeatNotification(context, scheduledNotification, false);
            }
        }
    }

    private static void initAndroidThreeTen(Context context) {
        if (VERSION.SDK_INT < VERSION_CODES.O) {
            AndroidThreeTen.init(context);
        }
    }

    private static Notification createNotification(Context context, NotificationDetails notificationDetails) {
        setupNotificationChannel(context, NotificationChannelDetails.fromNotificationDetails(notificationDetails));
        Intent intent = getLaunchIntent(context);
        intent.setAction(SELECT_NOTIFICATION);
        intent.putExtra(PAYLOAD, notificationDetails.payload);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationDetails.id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        DefaultStyleInformation defaultStyleInformation = (DefaultStyleInformation) notificationDetails.styleInformation;

        Notification.Builder builder = new Notification.Builder(context, notificationDetails.channelId).setContentTitle(defaultStyleInformation.htmlFormatTitle ? fromHtml(notificationDetails.title) : notificationDetails.title).setContentText(defaultStyleInformation.htmlFormatBody ? fromHtml(notificationDetails.body) : notificationDetails.body).setTicker(notificationDetails.ticker).setAutoCancel(BooleanUtils.getValue(notificationDetails.autoCancel)).setContentIntent(pendingIntent).setPriority(notificationDetails.priority).setOngoing(BooleanUtils.getValue(notificationDetails.ongoing)).setOnlyAlertOnce(BooleanUtils.getValue(notificationDetails.onlyAlertOnce));

        setSmallIcon(context, notificationDetails, builder);

        if (!StringUtils.isNullOrEmpty(notificationDetails.largeIcon)) {
            builder.setLargeIcon(getBitmapFromSource(context, notificationDetails.largeIcon, notificationDetails.largeIconBitmapSource));
        }

        if (notificationDetails.color != null) {
            builder.setColor(notificationDetails.color);
        }

        if (notificationDetails.showWhen != null) {
            builder.setShowWhen(BooleanUtils.getValue(notificationDetails.showWhen));
        }

        if (notificationDetails.when != null) {
            builder.setWhen(notificationDetails.when);
        }

        if (notificationDetails.usesChronometer != null) {
            builder.setUsesChronometer(notificationDetails.usesChronometer);
        }

        if (BooleanUtils.getValue(notificationDetails.fullScreenIntent)) {
            builder.setFullScreenIntent(pendingIntent, true);
        }

        if (!StringUtils.isNullOrEmpty(notificationDetails.shortcutId)) {
            builder.setShortcutId(notificationDetails.shortcutId);
        }

        if (!StringUtils.isNullOrEmpty(notificationDetails.subText)) {
            builder.setSubText(notificationDetails.subText);
        }

        setVisibility(notificationDetails, builder);
        applyGrouping(notificationDetails, builder);
        setStyle(context, notificationDetails, builder);
        setProgress(notificationDetails, builder);
        setCategory(notificationDetails, builder);
        setTimeoutAfter(notificationDetails, builder);
        Notification notification = builder.build();
        if (notificationDetails.additionalFlags != null && notificationDetails.additionalFlags.length > 0) {
            for (int additionalFlag : notificationDetails.additionalFlags) {
                notification.flags |= additionalFlag;
            }
        }
        return notification;
    }

    private static void setSmallIcon(Context context, NotificationDetails notificationDetails, Notification.Builder builder) {
        if (!StringUtils.isNullOrEmpty(notificationDetails.icon)) {
            int icon = getDrawableResourceId(context, notificationDetails.icon);

            if (icon == 0 && notificationDetails.icon.length() <= 4) {
                builder.setSmallIcon(textIcon(context, notificationDetails.icon));
            } else {
                builder.setSmallIcon(icon);
            }
        } else {
            SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
            String defaultIcon = sharedPreferences.getString(DEFAULT_ICON, null);
            if (StringUtils.isNullOrEmpty(defaultIcon)) {
                // for backwards compatibility: this is for handling the old way references to the icon used to be kept but should be removed in future
                builder.setSmallIcon(notificationDetails.iconResourceId);
            } else {
                builder.setSmallIcon(getDrawableResourceId(context, defaultIcon));
            }
        }
    }

    @NonNull
    static Gson buildGson() {
        if (gson == null) {
            RuntimeTypeAdapterFactory<StyleInformation> styleInformationAdapter = RuntimeTypeAdapterFactory.of(StyleInformation.class).registerSubtype(DefaultStyleInformation.class).registerSubtype(BigTextStyleInformation.class).registerSubtype(BigPictureStyleInformation.class).registerSubtype(InboxStyleInformation.class).registerSubtype(MessagingStyleInformation.class);
            GsonBuilder builder = new GsonBuilder().registerTypeAdapterFactory(styleInformationAdapter);
            gson = builder.create();
        }
        return gson;
    }

    private static ArrayList<NotificationDetails> loadScheduledNotifications(Context context) {
        ArrayList<NotificationDetails> scheduledNotifications = new ArrayList<>();
        SharedPreferences sharedPreferences = context.getSharedPreferences(SCHEDULED_NOTIFICATIONS, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(SCHEDULED_NOTIFICATIONS, null);
        if (json != null) {
            Gson gson = buildGson();
            Type type = new TypeToken<ArrayList<NotificationDetails>>() {
            }.getType();
            scheduledNotifications = gson.fromJson(json, type);
        }
        return scheduledNotifications;
    }

    private static void saveScheduledNotifications(Context context, ArrayList<NotificationDetails> scheduledNotifications) {
        Gson gson = buildGson();
        String json = gson.toJson(scheduledNotifications);
        SharedPreferences sharedPreferences = context.getSharedPreferences(SCHEDULED_NOTIFICATIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SCHEDULED_NOTIFICATIONS, json);
        tryCommittingInBackground(editor, 3);
    }

    private static void tryCommittingInBackground(final SharedPreferences.Editor editor, final int tries) {
        if (tries == 0) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean isCommitted = editor.commit();
                if (!isCommitted) {
                    tryCommittingInBackground(editor, tries - 1);
                }
            }
        }).start();
    }

    static void removeNotificationFromCache(Context context, Integer notificationId) {
        ArrayList<NotificationDetails> scheduledNotifications = loadScheduledNotifications(context);
        for (Iterator<NotificationDetails> it = scheduledNotifications.iterator(); it.hasNext(); ) {
            NotificationDetails notificationDetails = it.next();
            if (notificationDetails.id.equals(notificationId)) {
                it.remove();
                break;
            }
        }
        saveScheduledNotifications(context, scheduledNotifications);
    }

    @SuppressWarnings("deprecation")
    private static Spanned fromHtml(String html) {
        if (html == null) {
            return null;
        }
        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }

    private static void scheduleNotification(Context context, final NotificationDetails notificationDetails, Boolean updateScheduledNotificationsCache) {
        Gson gson = buildGson();
        String notificationDetailsJson = gson.toJson(notificationDetails);
        Intent notificationIntent = new Intent(context, ScheduledNotificationReceiver.class);
        notificationIntent.putExtra(NOTIFICATION_DETAILS, notificationDetailsJson);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationDetails.id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = getAlarmManager(context);
        if (BooleanUtils.getValue(notificationDetails.allowWhileIdle)) {
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, notificationDetails.millisecondsSinceEpoch, pendingIntent);
        } else {
            AlarmManagerCompat.setExact(alarmManager, AlarmManager.RTC_WAKEUP, notificationDetails.millisecondsSinceEpoch, pendingIntent);
        }

        if (updateScheduledNotificationsCache) {
            saveScheduledNotification(context, notificationDetails);
        }
    }

    private static void zonedScheduleNotification(Context context, final NotificationDetails notificationDetails, Boolean updateScheduledNotificationsCache) {
        Gson gson = buildGson();
        String notificationDetailsJson = gson.toJson(notificationDetails);
        Intent notificationIntent = new Intent(context, ScheduledNotificationReceiver.class);
        notificationIntent.putExtra(NOTIFICATION_DETAILS, notificationDetailsJson);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationDetails.id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = getAlarmManager(context);
        long epochMilli = VERSION.SDK_INT >= VERSION_CODES.O ? ZonedDateTime.of(LocalDateTime.parse(notificationDetails.scheduledDateTime), ZoneId.of(notificationDetails.timeZoneName)).toInstant().toEpochMilli() : org.threeten.bp.ZonedDateTime.of(org.threeten.bp.LocalDateTime.parse(notificationDetails.scheduledDateTime), org.threeten.bp.ZoneId.of(notificationDetails.timeZoneName)).toInstant().toEpochMilli();
        if (BooleanUtils.getValue(notificationDetails.allowWhileIdle)) {
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, epochMilli, pendingIntent);
        } else {
            AlarmManagerCompat.setExact(alarmManager, AlarmManager.RTC_WAKEUP, epochMilli, pendingIntent);
        }

        if (updateScheduledNotificationsCache) {
            saveScheduledNotification(context, notificationDetails);
        }
    }

    static void scheduleNextRepeatingNotification(Context context, NotificationDetails notificationDetails) {
        long repeatInterval = calculateRepeatIntervalMilliseconds(notificationDetails);
        long notificationTriggerTime = calculateNextNotificationTrigger(notificationDetails.calledAt, repeatInterval);
        Gson gson = buildGson();
        String notificationDetailsJson = gson.toJson(notificationDetails);
        Intent notificationIntent = new Intent(context, ScheduledNotificationReceiver.class);
        notificationIntent.putExtra(NOTIFICATION_DETAILS, notificationDetailsJson);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationDetails.id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = getAlarmManager(context);
        AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, notificationTriggerTime, pendingIntent);
        saveScheduledNotification(context, notificationDetails);
    }

    private static void repeatNotification(Context context, NotificationDetails notificationDetails, Boolean updateScheduledNotificationsCache) {
        long repeatInterval = calculateRepeatIntervalMilliseconds(notificationDetails);

        long notificationTriggerTime = notificationDetails.calledAt;
        if (notificationDetails.repeatTime != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, notificationDetails.repeatTime.hour);
            calendar.set(Calendar.MINUTE, notificationDetails.repeatTime.minute);
            calendar.set(Calendar.SECOND, notificationDetails.repeatTime.second);
            if (notificationDetails.day != null) {
                calendar.set(Calendar.DAY_OF_WEEK, notificationDetails.day);
            }

            notificationTriggerTime = calendar.getTimeInMillis();
        }

        notificationTriggerTime = calculateNextNotificationTrigger(notificationTriggerTime, repeatInterval);

        Gson gson = buildGson();
        String notificationDetailsJson = gson.toJson(notificationDetails);
        Intent notificationIntent = new Intent(context, ScheduledNotificationReceiver.class);
        notificationIntent.putExtra(NOTIFICATION_DETAILS, notificationDetailsJson);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationDetails.id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = getAlarmManager(context);

        if (BooleanUtils.getValue(notificationDetails.allowWhileIdle)) {
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, notificationTriggerTime, pendingIntent);
        } else {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, notificationTriggerTime, repeatInterval, pendingIntent);
        }
        if (updateScheduledNotificationsCache) {
            saveScheduledNotification(context, notificationDetails);
        }
    }

    private static long calculateNextNotificationTrigger(long notificationTriggerTime, long repeatInterval) {
        // ensures that time is in the future
        long currentTime = System.currentTimeMillis();
        while (notificationTriggerTime < currentTime) {
            notificationTriggerTime += repeatInterval;
        }
        return notificationTriggerTime;
    }

    private static long calculateRepeatIntervalMilliseconds(NotificationDetails notificationDetails) {
        long repeatInterval = 0;
        switch (notificationDetails.repeatInterval) {
            case EveryMinute:
                repeatInterval = 60000;
                break;
            case Hourly:
                repeatInterval = 60000 * 60;
                break;
            case Daily:
                repeatInterval = 60000 * 60 * 24;
                break;
            case Weekly:
                repeatInterval = 60000 * 60 * 24 * 7;
                break;
            default:
                break;
        }
        return repeatInterval;
    }

    private static void saveScheduledNotification(Context context, NotificationDetails notificationDetails) {
        ArrayList<NotificationDetails> scheduledNotifications = loadScheduledNotifications(context);
        ArrayList<NotificationDetails> scheduledNotificationsToSave = new ArrayList<>();
        for (NotificationDetails scheduledNotification : scheduledNotifications) {
            if (scheduledNotification.id.equals(notificationDetails.id)) {
                continue;
            }
            scheduledNotificationsToSave.add(scheduledNotification);
        }
        scheduledNotificationsToSave.add(notificationDetails);
        saveScheduledNotifications(context, scheduledNotificationsToSave);
    }

    private static int getDrawableResourceId(Context context, String name) {
        return context.getResources().getIdentifier(name, DRAWABLE, context.getPackageName());
    }

    private static Bitmap getBitmapFromSource(Context context, String bitmapPath, BitmapSource bitmapSource) {
        Bitmap bitmap = null;
        if (bitmapSource == BitmapSource.DrawableResource) {
            bitmap = BitmapFactory.decodeResource(context.getResources(), getDrawableResourceId(context, bitmapPath));
        } else if (bitmapSource == BitmapSource.FilePath) {
            bitmap = BitmapFactory.decodeFile(bitmapPath);
        } else if (bitmapSource == BitmapSource.VectorDrawable) {
            bitmap = getBitmapFromVectorDrawable(context, getDrawableResourceId(context, bitmapPath));
        }

        return bitmap;
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static Icon textIcon(Context context, String text) {
        float size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, context.getResources().getDisplayMetrics());
        float textSize = size;

        if (text.length() >= 4) {
            textSize = size / 2.0f;
        } else if (text.length() == 3){
            textSize = size / 1.5f;
        }

        Bitmap bitmap = Bitmap.createBitmap((int) size, (int) size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.create("Montserrat", Typeface.BOLD));

        canvas.drawText(text, (size / 2f), (size / 2f) + (textSize / 3f), paint);

        return Icon.createWithBitmap(bitmap);
    }

    /**
     * Sets the visibility property to the input Notification Builder
     *
     * @throws IllegalArgumentException If `notificationDetails.visibility` is not null but also
     *                                  not matches any known index.
     */
    private static void setVisibility(NotificationDetails notificationDetails, Notification.Builder builder) {
        if (notificationDetails.visibility == null) {
            return;
        }

        int visibility;
        switch (notificationDetails.visibility) {
            case 0: // Private
                visibility = Notification.VISIBILITY_PRIVATE;
                break;
            case 1: // Public
                visibility = Notification.VISIBILITY_PUBLIC;
                break;
            case 2: // Secret
                visibility = Notification.VISIBILITY_SECRET;
                break;

            default:
                throw new IllegalArgumentException("Unknown index: " + notificationDetails.visibility);
        }

        builder.setVisibility(visibility);
    }

    private static void applyGrouping(NotificationDetails notificationDetails, Notification.Builder builder) {
        boolean isGrouped = false;
        if (!StringUtils.isNullOrEmpty(notificationDetails.groupKey)) {
            builder.setGroup(notificationDetails.groupKey);
            isGrouped = true;
        }

        if (isGrouped) {
            if (BooleanUtils.getValue(notificationDetails.setAsGroupSummary)) {
                builder.setGroupSummary(true);
            }

            builder.setGroupAlertBehavior(notificationDetails.groupAlertBehavior);
        }
    }

    private static void setCategory(NotificationDetails notificationDetails, Notification.Builder builder) {
        if (notificationDetails.category == null) {
            return;
        }
        builder.setCategory(notificationDetails.category);
    }

    private static void setTimeoutAfter(NotificationDetails notificationDetails, Notification.Builder builder) {
        if (notificationDetails.timeoutAfter == null) {
            return;
        }
        builder.setTimeoutAfter(notificationDetails.timeoutAfter);
    }

    private static Intent getLaunchIntent(Context context) {
        String packageName = context.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        return packageManager.getLaunchIntentForPackage(packageName);
    }

    private static void setStyle(Context context, NotificationDetails notificationDetails, Notification.Builder builder) {
        switch (notificationDetails.style) {
            case BigText:
                setBigTextStyle(notificationDetails, builder);
                break;
            default:
                break;
        }
    }

    private static void setProgress(NotificationDetails notificationDetails, Notification.Builder builder) {
        if (BooleanUtils.getValue(notificationDetails.showProgress)) {
            builder.setProgress(notificationDetails.maxProgress, notificationDetails.progress, notificationDetails.indeterminate);
        }
    }

    private static void setBigTextStyle(NotificationDetails notificationDetails, Notification.Builder builder) {
        BigTextStyleInformation bigTextStyleInformation = (BigTextStyleInformation) notificationDetails.styleInformation;
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        if (bigTextStyleInformation.bigText != null) {
            CharSequence bigText = bigTextStyleInformation.htmlFormatBigText ? fromHtml(bigTextStyleInformation.bigText) : bigTextStyleInformation.bigText;
            bigTextStyle.bigText(bigText);
        }
        if (bigTextStyleInformation.contentTitle != null) {
            CharSequence contentTitle = bigTextStyleInformation.htmlFormatContentTitle ? fromHtml(bigTextStyleInformation.contentTitle) : bigTextStyleInformation.contentTitle;
            bigTextStyle.setBigContentTitle(contentTitle);
        }
        if (bigTextStyleInformation.summaryText != null) {
            CharSequence summaryText = bigTextStyleInformation.htmlFormatSummaryText ? fromHtml(bigTextStyleInformation.summaryText) : bigTextStyleInformation.summaryText;
            bigTextStyle.setSummaryText(summaryText);
        }
        builder.setStyle(bigTextStyle);
    }

    private static void setupNotificationChannel(Context context, NotificationChannelDetails notificationChannelDetails) {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(notificationChannelDetails.id);
            // only create/update the channel when needed/specified. Allow this happen to when channelAction may be null to support cases where notifications had been
            // created on older versions of the plugin where channel management options weren't available back then
            if ((notificationChannel == null && (notificationChannelDetails.channelAction == null || notificationChannelDetails.channelAction == NotificationChannelAction.CreateIfNotExists)) || (notificationChannel != null && notificationChannelDetails.channelAction == NotificationChannelAction.Update)) {
                notificationChannel = new NotificationChannel(notificationChannelDetails.id, notificationChannelDetails.name, notificationChannelDetails.importance);
                notificationChannel.setDescription(notificationChannelDetails.description);
                notificationChannel.setGroup(notificationChannelDetails.groupId);
                if (notificationChannelDetails.playSound) {
                    AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build();
                    Uri uri = retrieveSoundResourceUri(context, notificationChannelDetails.sound, notificationChannelDetails.soundSource);
                    notificationChannel.setSound(uri, audioAttributes);
                } else {
                    notificationChannel.setSound(null, null);
                }
                notificationChannel.enableVibration(BooleanUtils.getValue(notificationChannelDetails.enableVibration));
                if (notificationChannelDetails.vibrationPattern != null && notificationChannelDetails.vibrationPattern.length > 0) {
                    notificationChannel.setVibrationPattern(notificationChannelDetails.vibrationPattern);
                }
                boolean enableLights = BooleanUtils.getValue(notificationChannelDetails.enableLights);
                notificationChannel.enableLights(enableLights);
                if (enableLights && notificationChannelDetails.ledColor != null) {
                    notificationChannel.setLightColor(notificationChannelDetails.ledColor);
                }
                notificationChannel.setShowBadge(BooleanUtils.getValue(notificationChannelDetails.showBadge));
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }

    private static Uri retrieveSoundResourceUri(Context context, String sound, SoundSource soundSource) {
        Uri uri = null;
        if (StringUtils.isNullOrEmpty(sound)) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else {
            // allow null as soundSource was added later and prior to that, it was assumed to be a raw resource
            if (soundSource == null || soundSource == SoundSource.RawResource) {
                int soundResourceId = context.getResources().getIdentifier(sound, "raw", context.getPackageName());
                uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + soundResourceId);
            } else if (soundSource == SoundSource.Uri) {
                uri = Uri.parse(sound);
            }
        }
        return uri;
    }

    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    private static boolean isValidDrawableResource(Context context, String name, Result result, String errorCode) {
        int resourceId = context.getResources().getIdentifier(name, DRAWABLE, context.getPackageName());
        if (resourceId == 0) {
            result.error(errorCode, String.format(INVALID_DRAWABLE_RESOURCE_ERROR_MESSAGE, name), null);
            return false;
        }
        return true;
    }

    static void showNotification(Context context, NotificationDetails notificationDetails) {
        Notification notification = createNotification(context, notificationDetails);
        NotificationManagerCompat notificationManagerCompat = getNotificationManager(context);

        if (notificationDetails.tag != null) {
            notificationManagerCompat.notify(notificationDetails.tag, notificationDetails.id, notification);
        } else {
            notificationManagerCompat.notify(notificationDetails.id, notification);
        }
    }

    static void zonedScheduleNextNotification(Context context, NotificationDetails notificationDetails) {
        initAndroidThreeTen(context);
        String nextFireDate = getNextFireDate(notificationDetails);
        if (nextFireDate == null) {
            return;
        }
        notificationDetails.scheduledDateTime = nextFireDate;
        zonedScheduleNotification(context, notificationDetails, true);
    }

    static void zonedScheduleNextNotificationMatchingDateComponents(Context context, NotificationDetails notificationDetails) {
        initAndroidThreeTen(context);
        String nextFireDate = getNextFireDateMatchingDateTimeComponents(notificationDetails);
        if (nextFireDate == null) {
            return;
        }
        notificationDetails.scheduledDateTime = nextFireDate;
        zonedScheduleNotification(context, notificationDetails, true);
    }

    static String getNextFireDate(NotificationDetails notificationDetails) {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            if (notificationDetails.scheduledNotificationRepeatFrequency == ScheduledNotificationRepeatFrequency.Daily) {
                LocalDateTime localDateTime = LocalDateTime.parse(notificationDetails.scheduledDateTime).plusDays(1);
                return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime);
            } else if (notificationDetails.scheduledNotificationRepeatFrequency == ScheduledNotificationRepeatFrequency.Weekly) {
                LocalDateTime localDateTime = LocalDateTime.parse(notificationDetails.scheduledDateTime).plusWeeks(1);
                return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime);
            }
        } else {
            if (notificationDetails.scheduledNotificationRepeatFrequency == ScheduledNotificationRepeatFrequency.Daily) {
                org.threeten.bp.LocalDateTime localDateTime = org.threeten.bp.LocalDateTime.parse(notificationDetails.scheduledDateTime).plusDays(1);
                return org.threeten.bp.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime);
            } else if (notificationDetails.scheduledNotificationRepeatFrequency == ScheduledNotificationRepeatFrequency.Weekly) {
                org.threeten.bp.LocalDateTime localDateTime = org.threeten.bp.LocalDateTime.parse(notificationDetails.scheduledDateTime).plusWeeks(1);
                return org.threeten.bp.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime);
            }
        }
        return null;
    }

    static String getNextFireDateMatchingDateTimeComponents(NotificationDetails notificationDetails) {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            ZoneId zoneId = ZoneId.of(notificationDetails.timeZoneName);
            ZonedDateTime scheduledDateTime = ZonedDateTime.of(LocalDateTime.parse(notificationDetails.scheduledDateTime), zoneId);
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            ZonedDateTime nextFireDate = ZonedDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), scheduledDateTime.getHour(), scheduledDateTime.getMinute(), scheduledDateTime.getSecond(), scheduledDateTime.getNano(), zoneId);
            while (nextFireDate.isBefore(now)) {
                // adjust to be a date in the future that matches the time
                nextFireDate = nextFireDate.plusDays(1);
            }
            if (notificationDetails.matchDateTimeComponents == DateTimeComponents.Time) {
                return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(nextFireDate);
            } else if (notificationDetails.matchDateTimeComponents == DateTimeComponents.DayOfWeekAndTime) {
                while (nextFireDate.getDayOfWeek() != scheduledDateTime.getDayOfWeek()) {
                    nextFireDate = nextFireDate.plusDays(1);
                }
                return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(nextFireDate);
            }
        } else {
            org.threeten.bp.ZoneId zoneId = org.threeten.bp.ZoneId.of(notificationDetails.timeZoneName);
            org.threeten.bp.ZonedDateTime scheduledDateTime = org.threeten.bp.ZonedDateTime.of(org.threeten.bp.LocalDateTime.parse(notificationDetails.scheduledDateTime), zoneId);
            org.threeten.bp.ZonedDateTime now = org.threeten.bp.ZonedDateTime.now(zoneId);
            org.threeten.bp.ZonedDateTime nextFireDate = org.threeten.bp.ZonedDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), scheduledDateTime.getHour(), scheduledDateTime.getMinute(), scheduledDateTime.getSecond(), scheduledDateTime.getNano(), zoneId);
            while (nextFireDate.isBefore(now)) {
                // adjust to be a date in the future that matches the time
                nextFireDate = nextFireDate.plusDays(1);
            }
            if (notificationDetails.matchDateTimeComponents == DateTimeComponents.Time) {
                return org.threeten.bp.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(nextFireDate);
            } else if (notificationDetails.matchDateTimeComponents == DateTimeComponents.DayOfWeekAndTime) {
                while (nextFireDate.getDayOfWeek() != scheduledDateTime.getDayOfWeek()) {
                    nextFireDate = nextFireDate.plusDays(1);
                }
                return org.threeten.bp.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(nextFireDate);
            }
        }
        return null;
    }


    private static NotificationManagerCompat getNotificationManager(Context context) {
        return NotificationManagerCompat.from(context);
    }

    private void setActivity(Activity flutterActivity) {
        this.mainActivity = flutterActivity;
        if (mainActivity != null) {
            launchIntent = mainActivity.getIntent();
        }
    }

    private void onAttachedToEngine(Context context, BinaryMessenger binaryMessenger) {
        this.applicationContext = context;
        this.channel = new MethodChannel(binaryMessenger, METHOD_CHANNEL);
        this.channel.setMethodCallHandler(this);
    }

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
    }

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        binding.addOnNewIntentListener(this);
        mainActivity = binding.getActivity();
        launchIntent = mainActivity.getIntent();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.mainActivity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        binding.addOnNewIntentListener(this);
        mainActivity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        this.mainActivity = null;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case INITIALIZE_METHOD: {
                initialize(call, result);
                break;
            }
            case GET_NOTIFICATION_APP_LAUNCH_DETAILS_METHOD: {
                getNotificationAppLaunchDetails(result);
                break;
            }
            case SHOW_METHOD: {
                show(call, result);
                break;
            }
            case SCHEDULE_METHOD: {
                schedule(call, result);
                break;
            }
            case ZONED_SCHEDULE_METHOD: {
                zonedSchedule(call, result);
                break;
            }
            case PERIODICALLY_SHOW_METHOD:
            case SHOW_DAILY_AT_TIME_METHOD:
            case SHOW_WEEKLY_AT_DAY_AND_TIME_METHOD: {
                repeat(call, result);
                break;
            }
            case CANCEL_METHOD:
                cancel(call, result);
                break;
            case CANCEL_ALL_METHOD:
                cancelAllNotifications(result);
                break;
            case PENDING_NOTIFICATION_REQUESTS_METHOD:
                pendingNotificationRequests(result);
                break;
            case CREATE_NOTIFICATION_CHANNEL_GROUP_METHOD:
                createNotificationChannelGroup(call, result);
                break;
            case DELETE_NOTIFICATION_CHANNEL_GROUP_METHOD:
                deleteNotificationChannelGroup(call, result);
                break;
            case CREATE_NOTIFICATION_CHANNEL_METHOD:
                createNotificationChannel(call, result);
                break;
            case DELETE_NOTIFICATION_CHANNEL_METHOD:
                deleteNotificationChannel(call, result);
                break;
            case GET_ACTIVE_NOTIFICATIONS_METHOD:
                getActiveNotifications(result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void pendingNotificationRequests(Result result) {
        ArrayList<NotificationDetails> scheduledNotifications = loadScheduledNotifications(applicationContext);
        List<Map<String, Object>> pendingNotifications = new ArrayList<>();

        for (NotificationDetails scheduledNotification : scheduledNotifications) {
            HashMap<String, Object> pendingNotification = new HashMap<>();
            pendingNotification.put("id", scheduledNotification.id);
            pendingNotification.put("title", scheduledNotification.title);
            pendingNotification.put("body", scheduledNotification.body);
            pendingNotification.put("payload", scheduledNotification.payload);
            pendingNotifications.add(pendingNotification);
        }
        result.success(pendingNotifications);
    }

    private void cancel(MethodCall call, Result result) {
        Map<String, Object> arguments = call.arguments();
        Integer id = (Integer) arguments.get(CANCEL_ID);
        String tag = (String) arguments.get(CANCEL_TAG);
        cancelNotification(id, tag);
        result.success(null);
    }

    private void repeat(MethodCall call, Result result) {
        Map<String, Object> arguments = call.arguments();
        NotificationDetails notificationDetails = extractNotificationDetails(result, arguments);
        if (notificationDetails != null) {
            repeatNotification(applicationContext, notificationDetails, true);
            result.success(null);
        }
    }

    private void schedule(MethodCall call, Result result) {
        Map<String, Object> arguments = call.arguments();
        NotificationDetails notificationDetails = extractNotificationDetails(result, arguments);
        if (notificationDetails != null) {
            scheduleNotification(applicationContext, notificationDetails, true);
            result.success(null);
        }
    }

    private void zonedSchedule(MethodCall call, Result result) {
        Map<String, Object> arguments = call.arguments();
        NotificationDetails notificationDetails = extractNotificationDetails(result, arguments);
        if (notificationDetails != null) {
            if (notificationDetails.matchDateTimeComponents != null) {
                notificationDetails.scheduledDateTime = getNextFireDateMatchingDateTimeComponents(notificationDetails);
            }
            zonedScheduleNotification(applicationContext, notificationDetails, true);
            result.success(null);
        }
    }

    private void show(MethodCall call, Result result) {
        Map<String, Object> arguments = call.arguments();
        NotificationDetails notificationDetails = extractNotificationDetails(result, arguments);
        if (notificationDetails != null) {
            showNotification(applicationContext, notificationDetails);
            result.success(null);
        }
    }

    private void getNotificationAppLaunchDetails(Result result) {
        Map<String, Object> notificationAppLaunchDetails = new HashMap<>();
        String payload = null;
        Boolean notificationLaunchedApp = mainActivity != null && SELECT_NOTIFICATION.equals(mainActivity.getIntent().getAction()) && !launchedActivityFromHistory(mainActivity.getIntent());
        notificationAppLaunchDetails.put(NOTIFICATION_LAUNCHED_APP, notificationLaunchedApp);
        if (notificationLaunchedApp) {
            payload = launchIntent.getStringExtra(PAYLOAD);
        }
        notificationAppLaunchDetails.put(PAYLOAD, payload);
        result.success(notificationAppLaunchDetails);
    }

    private void initialize(MethodCall call, Result result) {
        Map<String, Object> arguments = call.arguments();
        String defaultIcon = (String) arguments.get(DEFAULT_ICON);

        initAndroidThreeTen(applicationContext);

        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DEFAULT_ICON, defaultIcon);
        tryCommittingInBackground(editor, 3);

        if (mainActivity != null && !launchedActivityFromHistory(mainActivity.getIntent())) {
            sendNotificationPayloadMessage(mainActivity.getIntent());
        }
        result.success(true);
    }

    private static boolean launchedActivityFromHistory(Intent intent) {
        return intent != null && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY;
    }


    /// Extracts the details of the notifications passed from the Flutter side and also validates that some of the details (especially resources) passed are valid
    private NotificationDetails extractNotificationDetails(Result result, Map<String, Object> arguments) {
        NotificationDetails notificationDetails = NotificationDetails.from(arguments);
        if (hasInvalidLargeIcon(result, notificationDetails.largeIcon, notificationDetails.largeIconBitmapSource) || hasInvalidBigPictureResources(result, notificationDetails) || hasInvalidRawSoundResource(result, notificationDetails) || hasInvalidLedDetails(result, notificationDetails)) {
            return null;
        }

        return notificationDetails;
    }

    private boolean hasInvalidLedDetails(Result result, NotificationDetails notificationDetails) {
        if (notificationDetails.ledColor != null && (notificationDetails.ledOnMs == null || notificationDetails.ledOffMs == null)) {
            result.error(INVALID_LED_DETAILS_ERROR_CODE, INVALID_LED_DETAILS_ERROR_MESSAGE, null);
            return true;
        }
        return false;
    }

    private boolean hasInvalidRawSoundResource(Result result, NotificationDetails notificationDetails) {
        if (!StringUtils.isNullOrEmpty(notificationDetails.sound) && (notificationDetails.soundSource == null || notificationDetails.soundSource == SoundSource.RawResource)) {
            int soundResourceId = applicationContext.getResources().getIdentifier(notificationDetails.sound, "raw", applicationContext.getPackageName());
            if (soundResourceId == 0) {
                result.error(INVALID_SOUND_ERROR_CODE, String.format(INVALID_RAW_RESOURCE_ERROR_MESSAGE, notificationDetails.sound), null);
                return true;
            }
        }
        return false;
    }

    private boolean hasInvalidBigPictureResources(Result result, NotificationDetails notificationDetails) {
        if (notificationDetails.style == NotificationStyle.BigPicture) {
            BigPictureStyleInformation bigPictureStyleInformation = (BigPictureStyleInformation) notificationDetails.styleInformation;
            if (hasInvalidLargeIcon(result, bigPictureStyleInformation.largeIcon, bigPictureStyleInformation.largeIconBitmapSource))
                return true;
            return bigPictureStyleInformation.bigPictureBitmapSource == BitmapSource.DrawableResource && !isValidDrawableResource(applicationContext, bigPictureStyleInformation.bigPicture, result, INVALID_BIG_PICTURE_ERROR_CODE);
        }
        return false;
    }

    private boolean hasInvalidLargeIcon(Result result, String largeIcon, BitmapSource largeIconBitmapSource) {
        return !StringUtils.isNullOrEmpty(largeIcon) && largeIconBitmapSource == BitmapSource.DrawableResource && !isValidDrawableResource(applicationContext, largeIcon, result, INVALID_LARGE_ICON_ERROR_CODE);
    }

    private void cancelNotification(Integer id, String tag) {
        Intent intent = new Intent(applicationContext, ScheduledNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(applicationContext, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = getAlarmManager(applicationContext);
        alarmManager.cancel(pendingIntent);
        NotificationManagerCompat notificationManager = getNotificationManager(applicationContext);
        if (tag == null) {
            notificationManager.cancel(id);
        } else {
            notificationManager.cancel(tag, id);
        }
        removeNotificationFromCache(applicationContext, id);
    }

    private void cancelAllNotifications(Result result) {
        NotificationManagerCompat notificationManager = getNotificationManager(applicationContext);
        notificationManager.cancelAll();
        ArrayList<NotificationDetails> scheduledNotifications = loadScheduledNotifications(applicationContext);
        if (scheduledNotifications == null || scheduledNotifications.isEmpty()) {
            result.success(null);
            return;
        }

        Intent intent = new Intent(applicationContext, ScheduledNotificationReceiver.class);
        for (NotificationDetails scheduledNotification : scheduledNotifications) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(applicationContext, scheduledNotification.id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = getAlarmManager(applicationContext);
            alarmManager.cancel(pendingIntent);
        }

        saveScheduledNotifications(applicationContext, new ArrayList<NotificationDetails>());
        result.success(null);
    }

    @Override
    public boolean onNewIntent(Intent intent) {
        boolean res = sendNotificationPayloadMessage(intent);
        if (res && mainActivity != null) {
            mainActivity.setIntent(intent);
        }
        return res;
    }

    private Boolean sendNotificationPayloadMessage(Intent intent) {
        if (SELECT_NOTIFICATION.equals(intent.getAction())) {
            String payload = intent.getStringExtra(PAYLOAD);
            channel.invokeMethod("selectNotification", payload);
            return true;
        }
        return false;
    }

    private void createNotificationChannelGroup(MethodCall call, Result result) {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            Map<String, Object> arguments = call.arguments();
            NotificationChannelGroupDetails notificationChannelGroupDetails = NotificationChannelGroupDetails.from(arguments);
            NotificationManager notificationManager = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannelGroup notificationChannelGroup = new NotificationChannelGroup(notificationChannelGroupDetails.id, notificationChannelGroupDetails.name);
            if (VERSION.SDK_INT >= VERSION_CODES.P) {
                notificationChannelGroup.setDescription(notificationChannelGroupDetails.description);
            }
            notificationManager.createNotificationChannelGroup(notificationChannelGroup);
        }
        result.success(null);
    }

    private void deleteNotificationChannelGroup(MethodCall call, Result result) {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
            String groupId = call.arguments();
            notificationManager.deleteNotificationChannelGroup(groupId);
        }
        result.success(null);
    }

    private void createNotificationChannel(MethodCall call, Result result) {
        Map<String, Object> arguments = call.arguments();
        NotificationChannelDetails notificationChannelDetails = NotificationChannelDetails.from(arguments);
        setupNotificationChannel(applicationContext, notificationChannelDetails);
        result.success(null);
    }

    private void deleteNotificationChannel(MethodCall call, Result result) {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
            String channelId = call.arguments();
            notificationManager.deleteNotificationChannel(channelId);
        }
        result.success(null);
    }

    private void getActiveNotifications(Result result) {
        if (VERSION.SDK_INT < VERSION_CODES.M) {
            result.error(GET_ACTIVE_NOTIFICATIONS_ERROR_CODE, GET_ACTIVE_NOTIFICATIONS_ERROR_MESSAGE, null);
            return;
        }
        NotificationManager notificationManager = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
            List<Map<String, Object>> activeNotificationsPayload = new ArrayList<>();

            for (StatusBarNotification activeNotification : activeNotifications) {
                HashMap<String, Object> activeNotificationPayload = new HashMap<>();
                activeNotificationPayload.put("id", activeNotification.getId());
                Notification notification = activeNotification.getNotification();
                if (VERSION.SDK_INT >= VERSION_CODES.O) {
                    activeNotificationPayload.put("channelId", notification.getChannelId());
                }
                activeNotificationPayload.put("title", notification.extras.getString("android.title"));
                activeNotificationPayload.put("body", notification.extras.getString("android.text"));
                activeNotificationsPayload.add(activeNotificationPayload);
            }
            result.success(activeNotificationsPayload);
        } catch (Throwable e) {
            result.error(GET_ACTIVE_NOTIFICATIONS_ERROR_CODE, e.getMessage(), e.getStackTrace());
        }
    }
}
