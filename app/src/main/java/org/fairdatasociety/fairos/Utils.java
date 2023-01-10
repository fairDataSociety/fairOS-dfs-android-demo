package org.fairdatasociety.fairos;

import android.content.Context;

import fairos.Fairos;

public class Utils {
    public static void init(Context context) throws Exception {
        Fairos.connect(
                context.getResources().getString(R.string.bee),
                context.getResources().getString(R.string.batch),
                context.getResources().getString(R.string.network),
                context.getResources().getString(R.string.rpc),
                5
        );
    }
}
