package com.dodopress.ui;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class InstantCopyTileService extends TileService {

    public static final String ACTION_INSTANT_PHOTOCOPY = "com.dodopress.action.INSTANT_PHOTOCOPY";
    public static final String EXTRA_INSTANT_PHOTOCOPY = "extra_instant_photocopy";

    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setLabel("Instant Copy");
            tile.setSubtitle("1-Touch Print");
            tile.setState(Tile.STATE_ACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(ACTION_INSTANT_PHOTOCOPY);
        intent.putExtra(EXTRA_INSTANT_PHOTOCOPY, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (Build.VERSION.SDK_INT >= 34) {
            PendingIntent pi = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            startActivityAndCollapse(pi);
        } else {
            startActivityAndCollapse(intent);
        }
    }
}
