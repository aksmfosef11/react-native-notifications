package com.wix.reactnativenotifications.core.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.ReactContext;
import com.wix.reactnativenotifications.core.AppLaunchHelper;
import com.wix.reactnativenotifications.core.AppLifecycleFacade;
import com.wix.reactnativenotifications.core.AppLifecycleFacade.AppVisibilityListener;
import com.wix.reactnativenotifications.core.AppLifecycleFacadeHolder;
import com.wix.reactnativenotifications.core.InitialNotificationHolder;
import com.wix.reactnativenotifications.core.JsIOHelper;
import com.wix.reactnativenotifications.core.NotificationIntentAdapter;
import com.wix.reactnativenotifications.core.ProxyService;
import com.wix.reactnativenotifications.utils.PreferenceHolder;

import java.util.List;

import static com.wix.reactnativenotifications.Defs.NOTIFICATION_OPENED_EVENT_NAME;
import static com.wix.reactnativenotifications.Defs.NOTIFICATION_RECEIVED_EVENT_NAME;
import static com.wix.reactnativenotifications.Defs.NOTIFICATION_RECEIVED_FOREGROUND_EVENT_NAME;
import static com.wix.reactnativenotifications.RNNotificationsModule.groupTalkUID;
import static com.wix.reactnativenotifications.RNNotificationsModule.isFocusRadio;
import static com.wix.reactnativenotifications.RNNotificationsModule.radioUID;
import static com.wix.reactnativenotifications.RNNotificationsModule.talkUID;

public class PushNotification implements IPushNotification {
    final protected Context mContext;
    final protected AppLifecycleFacade mAppLifecycleFacade;
    final protected AppLaunchHelper mAppLaunchHelper;
    final protected JsIOHelper mJsIOHelper;
    final protected PushNotificationProps mNotificationProps;
    final protected Bitmap largeIcon;
    final protected PreferenceHolder pref;
    final protected AppVisibilityListener mAppVisibilityListener = new AppVisibilityListener() {
        @Override
        public void onAppVisible() {
            mAppLifecycleFacade.removeVisibilityListener(this);
            dispatchImmediately();
        }

        @Override
        public void onAppNotVisible() {
        }
    };

    public static IPushNotification get(Context context, Bundle bundle) {
        if (verifyNotificationBundle(bundle) == false) {
            return null;
        }

        Context appContext = context.getApplicationContext();
        if (appContext instanceof INotificationsApplication) {
            return ((INotificationsApplication) appContext).getPushNotification(context, bundle, AppLifecycleFacadeHolder.get(), new AppLaunchHelper());
        }
        return new PushNotification(context, bundle, AppLifecycleFacadeHolder.get(), new AppLaunchHelper(), new JsIOHelper(), null);
    }

    protected PushNotification(Context context, Bundle bundle, AppLifecycleFacade appLifecycleFacade, AppLaunchHelper appLaunchHelper, JsIOHelper JsIOHelper, Bitmap largeIcon) {
        mContext = context;
        mAppLifecycleFacade = appLifecycleFacade;
        mAppLaunchHelper = appLaunchHelper;
        mJsIOHelper = JsIOHelper;
        mNotificationProps = createProps(bundle);
        this.largeIcon = largeIcon;
        this.pref = new PreferenceHolder(mContext);
    }

    private static boolean verifyNotificationBundle(Bundle bundle) {
        if (bundle.getString("google.message_id") != null) {
            return true;
        }

        return false;
    }

    @Override
    public void onReceived() throws InvalidNotificationException {
        int alarmType = Integer.parseInt(mNotificationProps.asBundle().getString("AlarmType"));
        boolean isPass = false;
        if (alarmType == 6) {
            long _talkUID = Long.parseLong(mNotificationProps.asBundle().getString("AdviceUID"));
            if (talkUID == _talkUID) {
                isPass = true;
            }
        } else if (alarmType == 41) {
            long _talkUID = Long.parseLong(mNotificationProps.asBundle().getString("TalkUID"));
            if (groupTalkUID == _talkUID) {
                isPass = true;
            }
        } else if (alarmType == 27) {
            long _broadUID = Long.parseLong(mNotificationProps.asBundle().getString("BroadUID"));
            if (radioUID == _broadUID && isFocusRadio) {
                isPass = true;
            }
        }
        if (!isPass) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationData notificationData = getNotification(alarmType);
                postNotification(null, notificationData.getId(), notificationData.getName(), alarmType);
            }else{
                if(isOnAlarm(alarmType)){
                    postNotification(null, "", "", alarmType);
                }
            }
        }
        notifyReceivedToJS();
        if (mAppLifecycleFacade.isAppVisible()) {
            notifiyReceivedForegroundNotificationToJS();
        }
    }

    private boolean isOnAlarm(int alarmType){
        if(pref != null) {
            switch (alarmType) {
                /**내고민에 토닥토닥 했을 때**/
                case 1:     //토닥토닥
                    return pref.getValue(pref.IS_POST_TODAK_ALARM, true);
                /**내고민,잡담에 답글이 달렸을 때**/
                case 2:
                case 16:
                    return pref.getValue(pref.IS_POST_ALARM, true);
                /**잡담,고민에서 내 댓글에 추천 또는 답글이 달렸을 때**/
                case 3:
                case 4:
                case 17:
                    return pref.getValue(pref.IS_POST_REPLY_ALARM, true);
                /**고민대화 알림**/
                case 6:
                case 7:
                case 9:
                case 10:
                case 39:
                case 40:
                case 41:
                    return pref.getValue(pref.IS_TALK_ALARM, true);
                /**고민라디오 알림**/
                case 22:
                case 23:
                case 24:
                case 26:
                case 27:
                case 28:
                case 30:
                    return pref.getValue(pref.IS_RADIO_ALARM, true);
                /**단체대화방 알림**/

                default:
                    return pref.getValue(pref.IS_OTHER_ALARM, true);
            }
        }else{
            return true;
        }
    }

    private NotificationData getNotification(int alarmType) {
        CreateNotification createNotification = new CreateNotification(mContext);
        List<NotificationData> notificationDataList = createNotification.getNofificationData();
        switch (alarmType) {
            /**내고민에 토닥토닥 했을 때**/
            case 1:     //토닥토닥
                return notificationDataList.get(2);
            /**내고민,잡담에 답글이 달렸을 때**/
            case 2:
            case 16:
                return notificationDataList.get(0);
            /**잡담,고민에서 내 댓글에 추천 또는 답글이 달렸을 때**/
            case 3:
            case 4:
            case 17:
                return notificationDataList.get(1);

            /**고민대화 알림**/
            case 6:
            case 7:
            case 9:
            case 10:
            case 39:
            case 40:
            case 41:
                return notificationDataList.get(3);
            /**고민라디오 알림**/
            case 22:
            case 23:
            case 24:
            case 26:
            case 27:
            case 28:
            case 30:
                return notificationDataList.get(4);
            /**단체대화방 알림**/

            default:
                return notificationDataList.get(5);
        }
    }

    @Override
    public void onOpened() {
        digestNotification();
        clearAllNotifications();
    }

    @Override
    public int onPostRequest(Integer notificationId) {
        int alarmType = Integer.parseInt(mNotificationProps.asBundle().getString("AlarmType"));
        NotificationData notificationData = getNotification(alarmType);
        return postNotification(notificationId, notificationData.getId(), notificationData.getName(), alarmType);
    }

    @Override
    public PushNotificationProps asProps() {
        return mNotificationProps.copy();
    }

    protected int postNotification(Integer notificationId, String channelID, String channelName, int alarmType) {
        final PendingIntent pendingIntent = getCTAPendingIntent();
        final Notification notification = buildNotification(pendingIntent, channelID, channelName, alarmType);
        return postNotification(notification, notificationId);
    }

    protected void digestNotification() {
        if (!mAppLifecycleFacade.isReactInitialized()) {
            setAsInitialNotification();
            launchOrResumeApp();
            return;
        }

        final ReactContext reactContext = mAppLifecycleFacade.getRunningReactContext();
        if (reactContext.getCurrentActivity() == null) {
            setAsInitialNotification();
        }

        if (mAppLifecycleFacade.isAppVisible()) {
            dispatchImmediately();
        } else {
            dispatchUponVisibility();
        }
    }

    protected PushNotificationProps createProps(Bundle bundle) {
        return new PushNotificationProps(bundle);
    }

    protected void setAsInitialNotification() {
        InitialNotificationHolder.getInstance().set(mNotificationProps);
    }

    protected void dispatchImmediately() {
        notifyOpenedToJS();
    }

    protected void dispatchUponVisibility() {
        mAppLifecycleFacade.addVisibilityListener(getIntermediateAppVisibilityListener());

        // Make the app visible so that we'll dispatch the notification opening when visibility changes to 'true' (see
        // above listener registration).
        launchOrResumeApp();
    }

    protected AppVisibilityListener getIntermediateAppVisibilityListener() {
        return mAppVisibilityListener;
    }

    protected PendingIntent getCTAPendingIntent() {
        final Intent cta = new Intent(mContext, ProxyService.class);
        return NotificationIntentAdapter.createPendingNotificationIntent(mContext, cta, mNotificationProps);
    }

    protected Notification buildNotification(PendingIntent intent, String channelID, String channelName, int alarmType) {
        return getNotificationBuilder(intent, channelID, channelName, alarmType).build();
    }

    protected Uri getSoundUri() {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + mContext.getPackageName() + "/raw/nagizi_sound");
    }

    protected int getIcon() {
        return mContext.getResources().getIdentifier("fcm_top_bar_icon", "drawable", mContext.getPackageName());
    }

    protected Notification.Builder getNotificationBuilder(PendingIntent intent, String CHANNEL_ID, String CHANNEL_NAME, int alarmType) {
        final Notification.Builder notification = new Notification.Builder(mContext)
                .setContentTitle(mNotificationProps.getTitle())
                .setContentText(mNotificationProps.getContent())
                .setSmallIcon(getIcon())
                .setLargeIcon(largeIcon)
                .setContentIntent(intent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification.setChannelId(CHANNEL_ID);
        }
        return notification;
    }

    protected int postNotification(Notification notification, Integer notificationId) {
        int id = notificationId != null ? notificationId : createNotificationId(notification);
        postNotification(id, notification);
        return id;
    }

    protected void postNotification(int id, Notification notification) {
        final NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    protected void clearAllNotifications() {
        final NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (largeIcon != null) {
            largeIcon.recycle();
        }
        notificationManager.cancelAll();
    }

    protected int createNotificationId(Notification notification) {
        return (int) System.nanoTime();
    }

    private void notifyReceivedToJS() {
        mJsIOHelper.sendEventToJS(NOTIFICATION_RECEIVED_EVENT_NAME, mNotificationProps.asBundle(), mAppLifecycleFacade.getRunningReactContext());
    }

    private void notifiyReceivedForegroundNotificationToJS() {
        mJsIOHelper.sendEventToJS(NOTIFICATION_RECEIVED_FOREGROUND_EVENT_NAME, mNotificationProps.asBundle(), mAppLifecycleFacade.getRunningReactContext());
    }

    private void notifyOpenedToJS() {
        mJsIOHelper.sendEventToJS(NOTIFICATION_OPENED_EVENT_NAME, mNotificationProps.asBundle(), mAppLifecycleFacade.getRunningReactContext());
    }

    protected void launchOrResumeApp() {
        final Intent intent = mAppLaunchHelper.getLaunchIntent(mContext);
        mContext.startActivity(intent);
    }
}
