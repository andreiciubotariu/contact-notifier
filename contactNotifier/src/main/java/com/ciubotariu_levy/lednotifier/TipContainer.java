package com.ciubotariu_levy.lednotifier;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;


public class TipContainer extends FragmentActivity {

    public static class TipDialog extends DialogFragment {


        public static TipDialog getInstance (Tip t) {
            Bundle args = new Bundle();
            args.putSerializable(KEY_TIP, t);
            TipDialog dialog = new TipDialog();
            dialog.setArguments(args);
            return dialog;
        }
        @Override
        public Dialog onCreateDialog (Bundle savedInstanceState) {
            Tip tip = (Tip) getArguments().getSerializable(KEY_TIP);
            return new AlertDialog.Builder(getActivity())
                    .setTitle(tip.title)
                    .setMessage(tip.message)
                    .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .create();
        }
    }

    private static final String DIALOG_TAG = "tips_dialog";
    private static final String KEY_TIP = "tip";

    public enum Tip {
        SET_SMS_APP (0,0),
        DUPLICATE_NOTIFS (0,0);

        public final int title;
        public final int message;

        Tip(int t, int m) {
          title = t;
          message = m;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getIntent().hasExtra(KEY_TIP)) {
            finish();
        }
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_TAG) == null) {
            TipDialog.getInstance((Tip)getIntent().getSerializableExtra(KEY_TIP))
                     .show(getSupportFragmentManager(), DIALOG_TAG);
        }
    }
}
