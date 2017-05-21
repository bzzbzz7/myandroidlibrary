package com.zz.android.utils;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Created by zhou.zheng on 2017/5/15.
 */

public class PermissionsUtils {
    private final Context mContext;

    public PermissionsUtils(Context context) {
        mContext = context.getApplicationContext();
    }

    // 判断权限集合
    public boolean lacksPermissions(String... permissions) {
        for (String permission : permissions) {
            if (lacksPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    // 判断是否缺少权限
    private boolean lacksPermission(String permission) {
        PackageManager pm = mContext.getPackageManager();
        return (PackageManager.PERMISSION_GRANTED == pm.checkPermission(permission, mContext.getPackageName()));
    }


}
