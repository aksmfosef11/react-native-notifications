package com.wix.reactnativenotifications;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.wix.reactnativenotifications.core.AppLifecycleFacadeHolder;
import com.wix.reactnativenotifications.core.InitialNotificationHolder;
import com.wix.reactnativenotifications.core.ReactAppLifecycleFacade;
import com.wix.reactnativenotifications.core.notification.IPushNotification;
import com.wix.reactnativenotifications.core.notification.NotificationData;
import com.wix.reactnativenotifications.core.notification.PushNotification;
import com.wix.reactnativenotifications.core.notification.PushNotificationProps;
import com.wix.reactnativenotifications.core.notificationdrawer.IPushNotificationsDrawer;
import com.wix.reactnativenotifications.core.notificationdrawer.PushNotificationsDrawer;
import com.wix.reactnativenotifications.gcm.FcmInstanceIdRefreshHandlerService;
import com.wix.reactnativenotifications.utils.PreferenceHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.wix.reactnativenotifications.Defs.LOGTAG;

public class RNNotificationsModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private PreferenceHolder pref;

    public RNNotificationsModule(Application application, ReactApplicationContext reactContext) {
        super(reactContext);
        if (AppLifecycleFacadeHolder.get() instanceof ReactAppLifecycleFacade) {
            ((ReactAppLifecycleFacade) AppLifecycleFacadeHolder.get()).init(reactContext);
        }
        pref = new PreferenceHolder(getReactApplicationContext().getApplicationContext());
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "WixRNNotifications";
    }

    @Override
    public void initialize() {
        Log.d(LOGTAG, "Native module init");
        startGcmIntentService(FcmInstanceIdRefreshHandlerService.EXTRA_IS_APP_INIT);

        final IPushNotificationsDrawer notificationsDrawer = PushNotificationsDrawer.get(getReactApplicationContext().getApplicationContext());
        notificationsDrawer.onAppInit();
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void onNewIntent(Intent intent) {
        Bundle notificationData = intent.getExtras();
        if (notificationData != null) {
            final IPushNotification notification = PushNotification.get(getReactApplicationContext().getApplicationContext(), notificationData);
            if (notification != null) {
                notification.onOpened();
            }
        }
    }

    @ReactMethod
    public void refreshToken() {
        Log.d(LOGTAG, "Native method invocation: refreshToken()");
        startGcmIntentService(FcmInstanceIdRefreshHandlerService.EXTRA_MANUAL_REFRESH);
    }

    @ReactMethod
    public void getInitialNotification(final Promise promise) {
        Log.d(LOGTAG, "Native method invocation: getInitialNotification");
        Object result = null;

        try {
            final PushNotificationProps notification = InitialNotificationHolder.getInstance().get();
            if (notification == null) {
                return;
            }

            result = Arguments.fromBundle(notification.asBundle());
        } finally {
            promise.resolve(result);
        }
    }

    @ReactMethod
    public void postLocalNotification(ReadableMap notificationPropsMap, int notificationId) {
        Log.d(LOGTAG, "Native method invocation: postLocalNotification");
        final Bundle notificationProps = Arguments.toBundle(notificationPropsMap);
        final IPushNotification pushNotification = PushNotification.get(getReactApplicationContext().getApplicationContext(), notificationProps);
        pushNotification.onPostRequest(notificationId);
    }

    @ReactMethod
    public void cancelLocalNotification(int notificationId) {
        IPushNotificationsDrawer notificationsDrawer = PushNotificationsDrawer.get(getReactApplicationContext().getApplicationContext());
        notificationsDrawer.onNotificationClearRequest(notificationId);
    }

    @ReactMethod
    public void isRegisteredForRemoteNotifications(Promise promise) {
        boolean hasPermission = NotificationManagerCompat.from(getReactApplicationContext()).areNotificationsEnabled();
        promise.resolve(new Boolean(hasPermission));
    }

    @ReactMethod
    public void changeAlarmSetting(String alarmType, boolean isAlarm) {
        pref = new PreferenceHolder(getReactApplicationContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String uuid = UUID.randomUUID().toString();
            NotificationManager notificationManager = (NotificationManager) getReactApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.deleteNotificationChannel(pref.getValue(alarmType, ""));
            NotificationChannel channelMessage;
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();

            switch (alarmType) {
                case "N_POST_CONTENT_ID":
                    pref.put(pref.N_POST_CONTENT_ID, uuid);
                    channelMessage = new NotificationChannel(uuid, "내 글 알림", isAlarm ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_NONE);

                    channelMessage.enableVibration(false);
                    channelMessage.setDescription("내 고민글, 잡담글에 달린 댓글에 대한 알림을 받습니다.");
                    channelMessage.enableLights(true);
                    channelMessage.setShowBadge(true);
//                    channelMessage.setSound(, audioAttributes);
                    channelMessage.setLightColor(Color.GREEN);
                    channelMessage.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                    notificationManager.createNotificationChannel(channelMessage);
                    break;
                case "N_POST_REPLY_ID":
                    pref.put(pref.N_POST_REPLY_ID, uuid);
                    channelMessage = new NotificationChannel(uuid, "내 댓글 알림",isAlarm ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_NONE);
                    channelMessage.enableVibration(false);
                    channelMessage.setDescription("내 댓글에 달린 답글에 대한 알림을 받습니다.");
                    channelMessage.enableLights(true);
                    channelMessage.setShowBadge(true);
//                    channelMessage.setSound(, audioAttributes);
                    channelMessage.setLightColor(Color.GREEN);
                    channelMessage.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                    notificationManager.createNotificationChannel(channelMessage);
                    break;

                case "N_POST_TODAK_ID":
                    pref.put(pref.N_POST_TODAK_ID, uuid);
                    channelMessage = new NotificationChannel(uuid, "토닥토닥 알림",isAlarm ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_NONE);
                    channelMessage.enableVibration(false);
                    channelMessage.setDescription("내 고민글이 토닥토닥을 받았을 때 알림을 받습니다.");
                    channelMessage.enableLights(true);
                    channelMessage.setShowBadge(true);
//                    channelMessage.setSound(, audioAttributes);
                    channelMessage.setLightColor(Color.GREEN);
                    channelMessage.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                    notificationManager.createNotificationChannel(channelMessage);
                    break;

                case "N_TALK_ID":
                    pref.put(pref.N_TALK_ID, uuid);
                    channelMessage = new NotificationChannel(uuid, "고민 대화 1:1 알림", isAlarm ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_NONE);
                    channelMessage.enableVibration(false);
                    channelMessage.setDescription("고민, 단체 대화와 관련된 알림을 받습니다.");
                    channelMessage.enableLights(true);
                    channelMessage.setShowBadge(true);
//                    channelMessage.setSound(, audioAttributes);
                    channelMessage.setLightColor(Color.GREEN);
                    channelMessage.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                    notificationManager.createNotificationChannel(channelMessage);
                    break;

                case "N_RADIO_ID":
                    pref.put(pref.N_RADIO_ID, uuid);
                    channelMessage = new NotificationChannel(uuid, "고민 라디오 알림", isAlarm ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_NONE);
                    channelMessage.enableVibration(false);
                    channelMessage.setDescription("고민 라디오와 관련된 알림을 받습니다.");
                    channelMessage.enableLights(true);
                    channelMessage.setShowBadge(true);
//                    channelMessage.setSound(, audioAttributes);
                    channelMessage.setLightColor(Color.GREEN);
                    channelMessage.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                    notificationManager.createNotificationChannel(channelMessage);
                    break;

                case "N_OTHER_ID":
                    pref.put(pref.N_OTHER_ID, uuid);
                    channelMessage = new NotificationChannel(uuid, "기타 알림", isAlarm ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_NONE);
                    channelMessage.enableVibration(false);
                    channelMessage.setDescription("");
                    channelMessage.enableLights(true);
                    channelMessage.setShowBadge(true);
//                    channelMessage.setSound(, audioAttributes);
                    channelMessage.setLightColor(Color.GREEN);
                    channelMessage.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                    notificationManager.createNotificationChannel(channelMessage);
                    break;
                default:
                    break;
            }
        } else {

        }
    }

    protected void startGcmIntentService(String extraFlag) {
        final Context appContext = getReactApplicationContext().getApplicationContext();
        final Intent tokenFetchIntent = new Intent(appContext, FcmInstanceIdRefreshHandlerService.class);
        tokenFetchIntent.putExtra(extraFlag, true);
        appContext.startService(tokenFetchIntent);
    }
}
