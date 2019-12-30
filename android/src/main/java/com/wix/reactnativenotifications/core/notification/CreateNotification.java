package com.wix.reactnativenotifications.core.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.wix.reactnativenotifications.R;
import com.wix.reactnativenotifications.utils.PreferenceHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateNotification {
    private Context context;
    private PreferenceHolder pref;
    public CreateNotification(Context context) {
        pref = new PreferenceHolder(context);
    }

    public CreateNotification(Context context, Uri soundUri) {
        this.context = context;
        pref = new PreferenceHolder(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            removeNotification(notificationManager);
            List<NotificationData> notificationData;
            notificationData = getCreateNofificationData();

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                    .build();
            for (NotificationData noData : notificationData) {
                NotificationChannel channelMessage;
                channelMessage = new NotificationChannel(noData.getId(), noData.getName(), NotificationManager.IMPORTANCE_HIGH);
                channelMessage.enableVibration(false);
                channelMessage.setDescription(noData.getDescription().equals("") ? null : noData.getDescription());
                channelMessage.enableLights(true);
                channelMessage.setShowBadge(true);
                channelMessage.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.nagizi_sound), audioAttributes);
                channelMessage.setLightColor(Color.GREEN);
                channelMessage.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                notificationManager.createNotificationChannel(channelMessage);
            }
        }
    }

    public List<NotificationData> getNofificationData() {
        List<NotificationData> notificationData = new ArrayList<>();
        notificationData.add(new NotificationData(pref.getValue(pref.N_POST_CONTENT_ID,""), "내 글 알림", "내 고민글, 잡담글에 달린 댓글에 대한 알림을 받습니다."));
        notificationData.add(new NotificationData(pref.getValue(pref.N_POST_REPLY_ID,""), "내 댓글 알림", "내 댓글에 달린 답글에 대한 알림을 받습니다."));
        notificationData.add(new NotificationData(pref.getValue(pref.N_POST_TODAK_ID,""), "토닥토닥 알림", "내 고민글이 토닥토닥을 받았을 때 알림을 받습니다."));
        notificationData.add(new NotificationData(pref.getValue(pref.N_TALK_ID,""), "고민 대화 1:1 알림", "고민, 단체 대화와 관련된 알림을 받습니다."));
        notificationData.add(new NotificationData(pref.getValue(pref.N_RADIO_ID,""), "고민 라디오 알림", "고민 라디오와 관련된 알림을 받습니다."));
        notificationData.add(new NotificationData(pref.getValue(pref.N_OTHER_ID,""), "기타 알림", ""));
        return notificationData;
    }

    public List<NotificationData> getCreateNofificationData() {
        if(pref.getValue(pref.N_POST_CONTENT_ID,"").equals("")) {
            List<NotificationData> notificationData = new ArrayList<>();
            String postContent = UUID.randomUUID().toString();
            String postReply = UUID.randomUUID().toString();
            String postTodak = UUID.randomUUID().toString();
            String talk = UUID.randomUUID().toString();
            String radio = UUID.randomUUID().toString();
            String other = UUID.randomUUID().toString();
            pref.put(pref.N_POST_CONTENT_ID, postContent);
            pref.put(pref.N_POST_REPLY_ID, postReply);
            pref.put(pref.N_POST_TODAK_ID, postTodak);
            pref.put(pref.N_TALK_ID, talk);
            pref.put(pref.N_RADIO_ID, radio);
            pref.put(pref.N_OTHER_ID, other);
            notificationData.add(new NotificationData(postContent, "내 글 알림", "내 고민글, 잡담글에 달린 댓글에 대한 알림을 받습니다."));
            notificationData.add(new NotificationData(postReply, "내 댓글 알림", "내 댓글에 달린 답글에 대한 알림을 받습니다."));
            notificationData.add(new NotificationData(postTodak, "토닥토닥 알림", "내 고민글이 토닥토닥을 받았을 때 알림을 받습니다."));
            notificationData.add(new NotificationData(talk, "고민 대화 1:1 알림", "고민, 단체 대화와 관련된 알림을 받습니다."));
            notificationData.add(new NotificationData(radio, "고민 라디오 알림", "고민 라디오와 관련된 알림을 받습니다."));
            notificationData.add(new NotificationData(other, "기타 알림", ""));
            return notificationData;
        }else{
            return new ArrayList<>();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void removeNotification(NotificationManager notificationManager ) {
        notificationManager.deleteNotificationChannel("nagiji_1_postContent_1");
        notificationManager.deleteNotificationChannel("nagiji_2_postReply_1");
        notificationManager.deleteNotificationChannel("nagiji_3_todak_1");
        notificationManager.deleteNotificationChannel("nagiji_4_talk_1");
        notificationManager.deleteNotificationChannel("nagiji_5_groupTalk_1");
        notificationManager.deleteNotificationChannel("nagiji_6_radio_subscribe_1");
        notificationManager.deleteNotificationChannel("nagiji_7_radio_background_1");
        notificationManager.deleteNotificationChannel("nagiji_8_other_1");
    }
}
