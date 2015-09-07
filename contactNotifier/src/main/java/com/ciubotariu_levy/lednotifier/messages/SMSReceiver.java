package com.ciubotariu_levy.lednotifier.messages;

public class SMSReceiver {/*extends BroadcastReceiver {
    public static final String TAG = SMSReceiver.class.getName();
    public static final int ACTIVITY_REQUEST_CODE = 0;
    public static final int DEL_REQUEST_CODE = 2;
    public static final String KEY_TIMEOUT_LED = "led_timeout";

    protected static final String SHOW_ALL_NOTIFS = "show_all_notifications";
    private static final String DEF_VIBRATE = "def_vibrate";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        LinkedHashMap<String, MessageInfo> infoMap = new LinkedHashMap<>();
        List<String> customMessages = new ArrayList<>();

        if (intent.getAction().equals(Sms.Intents.WAP_PUSH_RECEIVED_ACTION)
                && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            Log.v(TAG, "onReceive: received PUSH Intent: " + intent);
            infoMap = MessageUtils.createMessageInfosFromPushIntent(intent, context);
        } else if (intent.getAction().equals(Sms.Intents.SMS_RECEIVED_ACTION)) {
            Log.v(TAG, "onReceive: received SMS: " + intent);
            infoMap = MessageUtils.createMessageInfosFromSmsIntent(intent, context);
        }
        onMessagesReceived(context, infoMap, customMessages);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT) //needs changin
    public void onMessagesReceived(Context context, LinkedHashMap<String, MessageInfo> infoMap, List<String> customMessages) {
        Log.v(TAG, "onMessagesReceived:");
        if (infoMap.isEmpty()) {
            Log.w(TAG, "onMessagesReceived: infoMap is empty");
            return;
        }

        MessageHistory.addMessages(infoMap);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean showAllNotifs = preferences.getBoolean(SHOW_ALL_NOTIFS, false);
        boolean showNotification = showAllNotifs;

        int color = preferences.getInt(DefaultColorChooserContainer.DEFAULT_COLOR, Color.GRAY);
        String vibratePattern = preferences.getBoolean("notifications_new_message_vibrate", false) ? DEF_VIBRATE : null;
        String ringtone = "";

        if (preferences.getBoolean("notif_and_sound", false)) {
            ringtone = preferences.getString("notifications_new_message_ringtone", Settings.System.DEFAULT_NOTIFICATION_URI.toString());
        }

        if (MessageHistory.getCustomColor() != Color.GRAY) {
            color = MessageHistory.getCustomColor();
            showNotification = true;
        }
        if (MessageHistory.getCustomRingtone() != null) {
            ringtone = MessageHistory.getCustomRingtone();
            showNotification = true;
        }
        if (MessageHistory.getCustomRingtone() != null) {
            vibratePattern = MessageHistory.getCustomRingtone();
            showNotification = true;
        }


        if (showNotification) {
            Intent i = new Intent(context, MainActivity.class);

            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            String smsAppPackageName = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ? preferences.getString(SmsAppChooserDialog.KEY_SMS_APP_PACKAGE, this.getClass().getPackage().getName())
                    : Sms.getDefaultSmsPackage(context);

            Intent smsAppIntent = context.getPackageManager().getLaunchIntentForPackage(smsAppPackageName);
            if (smsAppIntent == null) {
                smsAppIntent = context.getPackageManager().getLaunchIntentForPackage(this.getClass().getPackage().getName());
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(context, ACTIVITY_REQUEST_CODE, smsAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            boolean foundNotifPhoto = false;
            LinkedHashMap<String, MessageInfo> allMessages = MessageHistory.getMessages();
            NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context);
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            int counter = 0;
            int numMessages = allMessages.size();
            StringBuilder body = new StringBuilder(), summaryText = new StringBuilder();
            MessageInfo firstMessage = null;
            for (MessageInfo message : allMessages.values()) {
                if (message.isCustom() || (showAllNotifs && message.address != null)) {
                    if (counter == 0) {
                        firstMessage = message;
                    }
                    if (message.contactUriString != null) {
                        notifBuilder.addPerson(message.contactUriString);
                    }

                    body.append(message.getNameOrAddress()).append(": ").append(message.getContentString()).append(" ");
                    summaryText.append(message.getNameOrAddress()).append(" "); //TODO formatting, comma maybe?
                    inboxStyle.addLine(message.getNameOrAddress() + ": " + message.getContentString());

                    if (!foundNotifPhoto) {
                        Bitmap b = loadContactPhotoThumbnail(context, message.contactUriString);
                        if (b != null) {
                            Bitmap large = b;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                large = new RoundedTransformationBuilder().oval(true).build().transform(b);
                            }
                            notifBuilder.setLargeIcon(large);
                            foundNotifPhoto = true;
                        }
                    }
                    counter++;
                }
            }

            if (counter == 0) {
                Log.d(TAG, "onMessagesReceived: No notifications created");
                return;
            }

            String title = "";
            if (counter == 1) {
                body = new StringBuilder(firstMessage.getContentString());
                title = firstMessage.getNameOrAddress();
            } else if (counter >= 1) {
                title = "Multiple Senders";
                inboxStyle.setBigContentTitle(title);
                inboxStyle.setSummaryText(summaryText.toString());
                notifBuilder.setStyle(inboxStyle);
            }

            notifBuilder.setContentTitle(title)
                    .setContentText(body.toString())
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.ic_stat_new_msg)
                    .setLights(color, 1000, 1000) //flash
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE) //TODO option
                    .setAutoCancel(true);

            if (preferences.getBoolean("status_bar_preview", false)) {
                notifBuilder.setTicker(title + ": " + body.toString());
            } else {
                notifBuilder.setTicker("New message");
            }
            if (ringtone != null && ringtone.length() > 0) {
                notifBuilder.setSound(Uri.parse(ringtone));
            }
            if (!TextUtils.isEmpty(vibratePattern) && !vibratePattern.equals(DEF_VIBRATE)) {
                notifBuilder.setVibrate(LedContactInfo.getVibratePattern(vibratePattern));
            }
            NotificationUtils.title = title;
            NotificationUtils.message = body.toString();
            NotificationUtils.contentIntent = pendingIntent;

            Intent delIntent = new Intent(context, NotificationDismissReceiver.class);
            PendingIntent deletePendIntent = PendingIntent.getBroadcast(context, DEL_REQUEST_CODE, delIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notifBuilder.setDeleteIntent(deletePendIntent);

            Notification notif = notifBuilder.build();
            if (DEF_VIBRATE.equals(vibratePattern)) {
                notif.defaults |= Notification.DEFAULT_VIBRATE;
            }
            onNotificationReady(context, notif, preferences.getBoolean(KEY_TIMEOUT_LED, false));
        }
    }

    public void onNotificationReady(Context context, Notification notif, boolean ledTimeout) {
        boolean isServiceOn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && NotificationService.isNotificationListenerServiceOn;
        if (!isServiceOn && context != null && notif != null) {
            NotificationUtils.notify(context, notif, ledTimeout);
        }
    }

    private Bitmap loadContactPhotoThumbnail(Context context, String contactUri) {
        if (contactUri == null) {
            return null;
        }
        Cursor mCursor = context.getContentResolver().query((Uri.parse(contactUri)), new String[]{Contacts._ID,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? Contacts.PHOTO_THUMBNAIL_URI : Contacts._ID}, null, null, null);

        if (mCursor == null || !mCursor.moveToFirst()) {
            Log.e(TAG, "loadContactPhotoThumbnail: cursor error | " + mCursor);
            return null;
        }

        int mThumbnailColumn;
        int mIdColumn = mCursor.getColumnIndex(Contacts._ID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mThumbnailColumn = mCursor.getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI);
        } else {
            mThumbnailColumn = mIdColumn;
        }

        String photoData = mCursor.getString(mThumbnailColumn);
        if (photoData == null) {
            return null;
        }
        Log.v(TAG, "loadContactPhotoThumbnail: photoData is " + photoData);
        InputStream is = null;
        try {
            try {
                Uri thumbUri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    thumbUri = Uri.parse(photoData);
                } else {
                    final Uri contactPhotoUri = Uri.withAppendedPath(Contacts.CONTENT_URI, photoData);
                    thumbUri = Uri.withAppendedPath(contactPhotoUri, Contacts.Photo.CONTENT_DIRECTORY);
                }

                is = context.getContentResolver().openInputStream(thumbUri);
                if (is != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    int height = (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_height);
                    int width = (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_width);
                    Bitmap bm = BitmapFactory.decodeStream(is);
                    return Bitmap.createScaledBitmap(bm, width, height, false);
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "loadContactPhotoThumbnail: could not find file", e);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "loadContactPhotoThumbnail: could not close input stream", e);
                }
            }
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return null;
    } */
}