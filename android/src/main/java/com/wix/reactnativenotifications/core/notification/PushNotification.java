package com.wix.reactnativenotifications.core.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.facebook.react.bridge.ReactContext;
import com.wix.reactnativenotifications.core.AppLaunchHelper;
import com.wix.reactnativenotifications.core.AppLifecycleFacade;
import com.wix.reactnativenotifications.core.AppLifecycleFacade.AppVisibilityListener;
import com.wix.reactnativenotifications.core.AppLifecycleFacadeHolder;
import com.wix.reactnativenotifications.core.InitialNotificationHolder;
import com.wix.reactnativenotifications.core.JsIOHelper;
import com.wix.reactnativenotifications.core.NotificationIntentAdapter;
import com.wix.reactnativenotifications.core.ProxyService;

import java.util.List;

import static com.wix.reactnativenotifications.Defs.NOTIFICATION_OPENED_EVENT_NAME;
import static com.wix.reactnativenotifications.Defs.NOTIFICATION_RECEIVED_EVENT_NAME;
import static com.wix.reactnativenotifications.Defs.NOTIFICATION_RECEIVED_FOREGROUND_EVENT_NAME;

public class PushNotification implements IPushNotification {
    final protected Context mContext;
    final protected AppLifecycleFacade mAppLifecycleFacade;
    final protected AppLaunchHelper mAppLaunchHelper;
    final protected JsIOHelper mJsIOHelper;
    final protected PushNotificationProps mNotificationProps;
    final protected Bitmap largeIcon;
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
    }

    private static boolean verifyNotificationBundle(Bundle bundle) {
        if (bundle.getString("google.message_id") != null) {
            return true;
        }

        return false;
    }

    @Override
    public void onReceived() throws InvalidNotificationException {
        NotificationData notificationData = getNotification(mNotificationProps.asBundle().getInt("AlarmType"));
        postNotification(null, notificationData.getId(), notificationData.getName());
        notifyReceivedToJS();
        if (mAppLifecycleFacade.isAppVisible()) {
            notifiyReceivedForegroundNotificationToJS();
        }
    }

    private NotificationData getNotification(int alarmType) {
        CreateNotification createNotification = new CreateNotification();
        List<NotificationData> notificationDataList = createNotification.getNofificationData();
        switch (alarmType) {
            /**내고민에 토닥토닥 했을 때**/
            case 1:     //토닥토닥
                return notificationDataList.get(0);
            /**내고민,잡담에 답글이 달렸을 때**/
            case 2:
            case 16:
                return notificationDataList.get(1);
            /**잡담,고민에서 내 댓글에 추천 또는 답글이 달렸을 때**/
            case 3:
            case 4:
            case 17:
                return notificationDataList.get(2);

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
        NotificationData notificationData = getNotification(mNotificationProps.asBundle().getInt("AlarmType"));
        return postNotification(notificationId, notificationData.getId(), notificationData.getName());
    }

    @Override
    public PushNotificationProps asProps() {
        return mNotificationProps.copy();
    }

    protected int postNotification(Integer notificationId, String channelID, String channelName) {
        final PendingIntent pendingIntent = getCTAPendingIntent();
        final Notification notification = buildNotification(pendingIntent, channelID, channelName);
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

    protected Notification buildNotification(PendingIntent intent, String channelID, String channelName) {
        return getNotificationBuilder(intent, channelID, channelName).build();
    }

    protected Uri getSoundUri() {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + mContext.getPackageName() + "/raw/nagizi_sound");
    }

    protected int getIcon() {
        return mContext.getResources().getIdentifier("fcm_top_bar_icon", "drawable", mContext.getPackageName());
    }

    protected Notification.Builder getNotificationBuilder(PendingIntent intent, String CHANNEL_ID, String CHANNEL_NAME) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();

        final Notification.Builder notification = new Notification.Builder(mContext)
                .setContentTitle(mNotificationProps.getTitle())
                .setContentText(mNotificationProps.getContent())
                .setSmallIcon(getIcon())
                .setLargeIcon(largeIcon)
                .setContentIntent(intent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSound(getSoundUri(), audioAttributes)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(getSoundUri(), audioAttributes);

            final NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
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
