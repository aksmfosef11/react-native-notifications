package com.wix.reactnativenotifications.core.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

public class CreateNotification {
    public CreateNotification() {
    }

    public CreateNotification(Context context, Uri soundUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            removeNotification(notificationManager);
            List<NotificationData> notificationData;
            notificationData = getNofificationData();

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();
            for (NotificationData noData : notificationData) {
                NotificationChannel channelMessage;
                if (noData.getId().equals("nagiji6_other1")) {
                    channelMessage = new NotificationChannel(noData.getId(), noData.getName(), NotificationManager.IMPORTANCE_LOW);
                } else {
                    channelMessage = new NotificationChannel(noData.getId(), noData.getName(), NotificationManager.IMPORTANCE_HIGH);
                }
                channelMessage.enableVibration(false);
                channelMessage.setDescription(noData.getDescription().equals("") ? null : noData.getDescription());
                channelMessage.enableLights(true);
                channelMessage.setShowBadge(true);
                channelMessage.setSound(soundUri, audioAttributes);
                channelMessage.setLightColor(Color.GREEN);
                channelMessage.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                notificationManager.createNotificationChannel(channelMessage);
            }
        }
    }

    public List<NotificationData> getNofificationData() {
        List<NotificationData> notificationData = new ArrayList<>();
        notificationData.add(new NotificationData("nagiji1_postContent1", "내 글 알림", "내 고민글, 잡담글에 달린 댓글에 대한 알림을 받습니다."));
        notificationData.add(new NotificationData("nagiji2_postReply1", "내 댓글 알림", "내 댓글에 달린 답글에 대한 알림을 받습니다."));
        notificationData.add(new NotificationData("nagiji3_todak1", "토닥토닥 알림", "내 고민글이 토닥토닥을 받았을 때 알림을 받습니다."));
        notificationData.add(new NotificationData("nagiji4_talk1", "고민 대화 1:1 알림", "고민, 단체 대화와 관련된 알림을 받습니다."));
        notificationData.add(new NotificationData("nagiji5_radio1", "고민 라디오 알림", "고민 라디오와 관련된 알림을 받습니다."));
        notificationData.add(new NotificationData("nagiji6_other1", "기타 알림", ""));
        return notificationData;
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
