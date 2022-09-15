package org.fairdatasociety.fairos;

import android.content.Context;

import fairos.Fairos;

public class Utils {
    public static void init(String dataDir, Context context) throws Exception {
        Fairos.connect(
                dataDir,
                context.getResources().getString(R.string.bee),
                context.getResources().getString(R.string.batch),
                context.getResources().getString(R.string.network),
                context.getResources().getString(R.string.rpc),
                true,
                5
        );
    }
}
