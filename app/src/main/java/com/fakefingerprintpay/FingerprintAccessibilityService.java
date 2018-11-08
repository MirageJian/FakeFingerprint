package com.fakefingerprintpay;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.fakefingerprintpay.infos.Appsinfo;
import com.fakefingerprintpay.infos.Password;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class FingerprintAccessibilityService extends AccessibilityService {
    private Password password = new Password();
    private boolean flag = true;
    private String operatingPkg;

    @Override
    public void onServiceConnected() {
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        final View customView = inflater.inflate(R.layout.dialog_add_password, null);
        final EditText et = customView.findViewById(R.id.password);
        final AlertDialog dialog = new AlertDialog.Builder(getApplicationContext())
                .setTitle("初始化")
                .setView(customView)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String input = et.getText().toString();
                        if (input.equals("")) {
                            Toast.makeText(getApplicationContext(), "请输入密码", Toast.LENGTH_SHORT).show();
                            disableSelf();
                        } else {
                            password.setPassword(input);
                        }
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        disableSelf();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        disableSelf();
                    }
                })
                .create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        dialog.show();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        final String pkgName = event.getPackageName().toString();
        String className = event.getClassName().toString();
//        Toast.makeText(getApplicationContext(), className, Toast.LENGTH_SHORT).show();//测试classname
        final AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        switch (pkgName) {
//            case "com.fakefingerprintpay":
            case Appsinfo.alipayPkg:
                if (className.equals(Appsinfo.alipayTrigger)) {
                    List<AccessibilityNodeInfo> input = findNodeById(Appsinfo.alipayInput, nodeInfo);
//                    if (input == null || input.size() < 1)
//                        input = findNodeById(Appsinfo.alipayInput2, nodeInfo);
                    if (input != null && input.size() > 0 && flag) {
                        flag = false;
                        operatingPkg = pkgName;
                        showFingerprintDialog();
                    }
                }
                break;
            case Appsinfo.taobaoPkg:
                if (className.equals(Appsinfo.taobaoTrigger)) {
                    List<AccessibilityNodeInfo> input = findNodeById(Appsinfo.taobaoInput, nodeInfo);
                    if (input != null && input.size() > 0)
                        if (flag) {
                            flag = false;
                            operatingPkg = pkgName;
                            showFingerprintDialog();
                        }
                }
                break;
            case Appsinfo.wechatPkg:
                if (className.equals(Appsinfo.wechatTrigger)) {
                    List<AccessibilityNodeInfo> input = findNodeById(Appsinfo.wechatInput, nodeInfo);
                    if (input != null && input.size() > 0)
                        if (flag) {
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    flag = false;
                                    operatingPkg = pkgName;
//                            showFingerprintDialog();

                                }
                            }, 2000);
                        }
                }
                break;
        }
    }

    // 操作键盘
    private void operatePad() {
        final List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        for (String p : password.getPasswordPad(operatingPkg)) {
            List<AccessibilityNodeInfo> node = null;
            switch (operatingPkg) {
                case Appsinfo.alipayPkg:
                case Appsinfo.taobaoPkg:
                    node = findNodeById(p, nodeInfo);
                    System.out.println(node);
                    break;
                case Appsinfo.wechatPkg:
                    node = findNodeByText(p);
                    break;
            }
            if (node != null) {
                nodes.addAll(node);
            }
        }
        simulateClick(nodes);
    }

    private List<AccessibilityNodeInfo> findNodeById(String viewId, AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            return nodeInfo.findAccessibilityNodeInfosByViewId(viewId);
        }
        return null;
    }

    private List<AccessibilityNodeInfo> findNodeByText(String text) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            return nodeInfo.findAccessibilityNodeInfosByText(text);
        }
        return null;
    }

    private void simulateClick(List<AccessibilityNodeInfo> nodes) {
        for (AccessibilityNodeInfo node : nodes) {
            if (node.isEnabled()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    private void showFingerprintDialog() {
        // 指纹识别部分
        final FingerprintManager manager = (FingerprintManager) this.getSystemService(Context.FINGERPRINT_SERVICE);
        KeyguardManager mKeyManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
        final AlertDialog dialog = new AlertDialog.Builder(getApplicationContext())
//                .setTitle("指纹支付")
                .setMessage("请按压指纹")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                flag = true;
                            }
                        }, 3000);
                    }
                })
                .create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        dialog.show();
        //android studio 上，没有这个会报错
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "没有指纹识别权限", Toast.LENGTH_SHORT).show();
        }
        FingerprintManager.AuthenticationCallback mSelfCancelled = new FingerprintManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                //但多次指纹密码验证错误后，进入此方法；并且，不能短时间内调用指纹验证
                Toast.makeText(getApplicationContext(), errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                super.onAuthenticationHelp(helpCode, helpString);
                Toast.makeText(getApplicationContext(), helpString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(), "指纹识别成功", Toast.LENGTH_SHORT).show();
                dialog.cancel();
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        operatePad();
                    }
                }, 100);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "指纹识别失败", Toast.LENGTH_SHORT).show();
            }
        };
        if (manager != null)
            manager.authenticate(null, new CancellationSignal(), FingerprintManager.FINGERPRINT_ACQUIRED_GOOD, mSelfCancelled, null);
    }

    @Override
    public void onInterrupt() {
    }


    private boolean clickBackKey() {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    private void showFingerprintWindow() {
        final WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE); // 窗口管理者
        WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();// 窗口的属性
        mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;// 系统提示window
        mParams.format = PixelFormat.TRANSPARENT;// 支持透明
        // mParams.format = PixelFormat.RGBA_8888
        //mParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL ; WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE// 焦点
        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;// 窗口的宽和高
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        //mParams.gravity = Gravity.START ; Gravity.TOP
        mParams.dimAmount = 0.2f;
        mParams.windowAnimations = android.R.style.Animation_Toast;
        // mParams.alpha = 0.8f;//窗口的透明度
        final View windowView = LayoutInflater.from(getApplicationContext()).inflate(
                R.layout.dialog_fingerprint_popup, null);
        //单击View是关闭弹窗
        windowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (windowManager != null)
                    windowManager.removeView(windowView);
            }
        });
        if (Settings.canDrawOverlays(getApplicationContext())) {
            if (windowManager != null)
                windowManager.addView(windowView, mParams);// 添加窗口
        } else {
            String mAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION;
            try {
                this.startActivity(new Intent(mAction));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
