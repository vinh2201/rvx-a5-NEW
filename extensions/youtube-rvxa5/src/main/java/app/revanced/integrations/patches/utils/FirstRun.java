package app.revanced.integrations.patches.utils;

import static app.revanced.integrations.utils.ReVancedUtils.runOnMainThreadDelayed;
import static app.revanced.integrations.utils.SharedPrefHelper.SharedPrefNames.YOUTUBE;
import static app.revanced.integrations.utils.SharedPrefHelper.saveString;
import static app.revanced.integrations.utils.SharedPrefHelper.getString;
import static app.revanced.integrations.utils.StringRef.str;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;

import java.util.Objects;

import app.revanced.integrations.utils.IntegrationVersion;
import app.revanced.integrations.patches.extended.VersionOverridePatch;
import app.revanced.integrations.sponsorblock.PlayerController;
import app.revanced.integrations.settings.SettingsEnum;

public class FirstRun {

    public static void initializationSB(Context context) {
        if (SettingsEnum.SB_FIRSTRUN.getBoolean()) return;

        // set dummy video-id to initialize Sponsorblock: TeamVanced's Sponsorblock tutorial (https://www.youtube.com/watch?v=sE2IzSn-hHU)
        PlayerController.setCurrentVideoId("sE2IzSn-hHU");
        SettingsEnum.SB_FIRSTRUN.saveValue(true);
    }

    /**
     * The new layout is not loaded when the app is first installed.
     * (Also reproduced on unPatched YouTube)
     * <p>
     * Side effects when new layout is not loaded:
     * - Button container's layout is broken
     * - Zoom to fill screen not working
     * - 8X zoom not working in fullscreen
     * <p>
     * To fix this, show the reboot dialog when the app is installed for the first time.
     * <p>
     * The version of the current integrations is saved to YouTube's SharedPreferences to identify if the app was first installed.
     */
    public static void initializationRVX(Context context) {
        var integrationVersion = getString(context, YOUTUBE, "integrations", null);
        if (!Objects.equals(integrationVersion, IntegrationVersion.VERSION_NAME))
            saveString(context, YOUTUBE, "integrations", IntegrationVersion.VERSION_NAME);
        if (integrationVersion != null) return;

        // show dialog
        final Activity activity = (Activity) context;

        // When the app version is spoofed the server answers with the layout of the newer app, which the guard ignores
        // (see LayoutUpdatesPatch). Ask to disconnect for a few seconds so that layout is not even downloaded and let
        // the Restart button count down, so the app is not restarted before that time has passed.
        final boolean spoofed = PatchStatus.SpoofAppVersion() && VersionOverridePatch.isSpoofActive();
        final String restartLabel = str("in_app_update_restart_button");

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setMessage(str(spoofed ? "revanced_reboot_first_run_spoof" : "revanced_reboot_first_run"))
                .setPositiveButton(restartLabel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int id) {
                        runOnMainThreadDelayed(() -> {
                            activity.finishAffinity();
                            activity.startActivity(activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName()));
                            System.exit(0);
                        }, 1000L);
                    }
                })
                .setNegativeButton(str("sign_in_cancel"), null)
                .setCancelable(false)
                .create();
        dialog.show();

        if (spoofed) {
            startRestartCountdown(dialog, restartLabel, RESTART_COUNTDOWN_SECONDS);
        }
    }

    /**
     * Seconds the Restart button of the first run dialog stays disabled while the app version is spoofed.
     */
    private static final int RESTART_COUNTDOWN_SECONDS = 10;

    private static void startRestartCountdown(final AlertDialog dialog, final String label, final int seconds) {
        try {
            final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (button == null) return;

            final Handler handler = new Handler(Looper.getMainLooper());
            button.setEnabled(false);

            handler.post(new Runnable() {
                private int remaining = seconds;

                @Override
                public void run() {
                    try {
                        if (!dialog.isShowing()) return;

                        if (remaining <= 0) {
                            button.setText(label);
                            button.setEnabled(true);
                            return;
                        }

                        button.setText(label + " (" + remaining + ")");
                        remaining--;
                        handler.postDelayed(this, 1000L);
                    } catch (Throwable ex) {
                        // Never leave the user without a working button.
                        button.setEnabled(true);
                    }
                }
            });
        } catch (Throwable ex) {
            // Same as above: the countdown is only a convenience.
            try {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            } catch (Throwable ignored) {
            }
        }
    }

}
