package com.ciubotariu_levy.lednotifier.ui.fragment;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;

import com.ciubotariu_levy.lednotifier.R;
import com.ciubotariu_levy.lednotifier.preferences.Keys;
import com.ciubotariu_levy.lednotifier.preferences.Prefs;
import com.ciubotariu_levy.lednotifier.ui.widget.SMSAppAdapter;
import com.ciubotariu_levy.lednotifier.ui.widget.SMSAppAdapter.IconPackagePair;

import java.util.List;

public class SmsAppChooserDialog extends DialogFragment {
    public static final String KEY_SMS_APP_PACKAGE = "sms_app_package";

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + Uri.encode("0")));
        final PackageManager pm = getActivity().getPackageManager();
        final List<ResolveInfo> smsApps = pm.queryIntentActivities(intent, 0);
        IconPackagePair[] userList = new IconPackagePair[smsApps.size()];

        for (int x = 0; x < userList.length; x++) {
            ResolveInfo info = smsApps.get(x);
            IconPackagePair pair = new IconPackagePair();
            pair.icon = info.loadIcon(pm);
            pair.appName = info.loadLabel(pm).toString();
            userList[x] = pair;
        }

        Context context = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ?
                new ContextThemeWrapper(getActivity(), R.style.Theme_ContactNotifierBase) :
                getActivity();

        SMSAppAdapter adapter = new SMSAppAdapter(getActivity(), userList);
        return new AlertDialog.Builder(context)
                .setTitle(R.string.choose_sms_app)
                .setAdapter(adapter, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Prefs.getInstance(getActivity())
                                .putString(Keys.SMS_APP_PACKAGE, smsApps.get(which).activityInfo.packageName);
                    }
                })
                .create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        finishHostActivity();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        finishHostActivity();
    }

    private void finishHostActivity() {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
