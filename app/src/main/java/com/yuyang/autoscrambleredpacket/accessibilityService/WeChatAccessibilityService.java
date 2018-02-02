package com.yuyang.autoscrambleredpacket.accessibilityService;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yuyang.autoscrambleredpacket.MainActivity;
import com.yuyang.autoscrambleredpacket.utils.LogUtils;
import com.yuyang.autoscrambleredpacket.utils.SensitiveWordEngine;
import com.yuyang.autoscrambleredpacket.utils.SensitiveWordInit;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.yuyang.autoscrambleredpacket.ShieldKeyWordActivity.KEY_WORDS;

/**
 * AccessibilityService就是一个后台监控服务，设计用来帮助使用障碍的人士
 *
 * 在手机无障碍设置中开启
 * 微信6.6.2
 */

public class WeChatAccessibilityService extends AccessibilityService {
    private static final String TAG = "MyAccessibilityService";
    private boolean inProgress = false;//是否在开红包的过程中
    private List<String> keyWords = new ArrayList<>();
    // 初始化敏感词库对象
    private SensitiveWordInit sensitiveWordInit = new SensitiveWordInit();
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener mListener = new SharedPreferences
            .OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            String keyWordsJson = sharedPreferences.getString(KEY_WORDS, null);
            if (keyWordsJson != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<String>>() {}.getType();
                keyWords.clear();
                keyWords.addAll((Collection<? extends String>) gson.fromJson(keyWordsJson, type));
                initSensitiveWords();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Gson gson = new Gson();
        sharedPreferences = getSharedPreferences(KEY_WORDS, MODE_PRIVATE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(mListener);
        String keyWordsJson = sharedPreferences.getString(KEY_WORDS, null);
        if (keyWordsJson != null) {
            Type type = new TypeToken<List<String>>() {}.getType();
            keyWords.addAll((Collection<? extends String>) gson.fromJson(keyWordsJson, type));
        }
        initSensitiveWords();
    }

    private void initSensitiveWords(){
        // 构建敏感词库
        Map sensitiveWordMap = sensitiveWordInit.initKeyWord(keyWords);
        // 传入SensitiveWordEngine类中的敏感词库
        SensitiveWordEngine.sensitiveWordMap = sensitiveWordMap;
    }

    /**
     * 当启动服务的时候被调用
     */
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Intent intent = new Intent();
        intent.setAction(MainActivity.ACTION);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(MainActivity.START_OR_STOP, 1);
        startActivity(intent);
        LogUtils.e(TAG, "抢红包服务已启动！");
    }

    /**
     * 窗口事件回调
     * @param event
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        LogUtils.e(TAG, "进入了事件处理");
        int eventType = event.getEventType();
        switch (eventType){
            //当通知栏发生改变时
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                LogUtils.e(TAG, "通知栏发生变化-可能有微信红包的通知");
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        //包含"[微信红包]" 并且 不包含敏感词
                        if (content.contains("[微信红包]") && !SensitiveWordEngine.isContainSensitiveWord(content, 1)) {
                            //模拟打开通知栏消息，即打开微信
                            if (event.getParcelableData() != null &&
                                    event.getParcelableData() instanceof Notification) {
                                Notification notification = (Notification) event.getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                try {
                                    pendingIntent.send();
                                    LogUtils.e(TAG, "进入微信");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                break;
            //页面内容发生变化（比如有人发信息）
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                LogUtils.e(TAG, "TYPE_WINDOW_CONTENT_CHANGED："+event.getClassName().toString());
                LogUtils.e(TAG, event.getText().toString());
                getLastRedPacket();
                break;
            //当窗口的状态发生改变时（比如PopupWindow／Dialog弹出）
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                String className = event.getClassName().toString();
                LogUtils.e(TAG, "TYPE_WINDOW_STATE_CHANGED："+className);
                if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")) {//ZHU yuyang 版本更换需要修改的地方
                    //开红包
                    LogUtils.e(TAG,"开红包");
                    inputClick(0);
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")) {//ZHU yuyang 版本更换需要修改的地方
                    //退出红包
                    LogUtils.e(TAG,"退出红包");
                    inputClick(1);
                }
                break;
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                LogUtils.e(TAG, "TYPE_WINDOWS_CHANGED："+"页面改变了");
                break;
        }
    }

    /**
     * 通过ID获取控件，并进行模拟点击
     * @param actionType
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void inputClick(int actionType) {
        inProgress = true;
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list;//节点集合
            switch (actionType){
                case 0://开红包
                    list = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/c4j");//ZHU yuyang 版本更换需要修改的地方
                    for (AccessibilityNodeInfo item : list) {
                        item.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        item.recycle();
                    }
                    break;
                case 1://退出红包
                    list = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/hx");//ZHU yuyang 版本更换需要修改的地方
                    for (AccessibilityNodeInfo item : list) {
                        item.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        item.recycle();
                    }
                    break;
            }
        }
    }

    /**
     * 获取List中最后一个红包，并进行模拟点击
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void getLastRedPacket() {
        if (inProgress){//正在抢红包
            inProgress = false;
            return;
        }
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            //聊天界面：com.tencent.mm:id/ad8是红包的item
            List<AccessibilityNodeInfo> listChat = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ad8");//ZHU yuyang 版本更换需要修改的地方
            if (listChat != null && listChat.size() > 0) {//如果是在聊天页面，那肯定能找到唯一的一个node
                traversalChatNodes(listChat);
            }
            //微信列表界面：com.tencent.mm:id/app 每个人的item
            List<AccessibilityNodeInfo> listWeixin = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/app");//ZHU yuyang 版本更换需要修改的地方
            if (listWeixin != null && listWeixin.size() > 0) {
                traversalWeixinNodes(listWeixin);
            }
            rootNode.recycle();
        }
    }

    /**
     * 遍历所有聊天人的item，确认是否有未打开的红包
     * @param list
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void traversalWeixinNodes(List<AccessibilityNodeInfo> list) {
        AccessibilityNodeInfo unOpenPerson = null;
        for (int i=0; i<list.size(); i++){
            AccessibilityNodeInfo nodeInfo = list.get(i);
            List<AccessibilityNodeInfo> childContents = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/apt");//信息消息内容TextView ZHU yuyang 版本更换需要修改的地方
            List<AccessibilityNodeInfo> childRedDots = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/j4");//新信息提醒红点 ZHU yuyang 版本更换需要修改的地方
            if (childRedDots != null && childRedDots.size() > 0
                    && childContents != null && childContents.size() > 0){
                String content = childContents.get(0).getText().toString();
                //新消息不包含敏感词
                if (content.contains("[微信红包]") && !SensitiveWordEngine.isContainSensitiveWord(content, 1)){
                    unOpenPerson = nodeInfo;
                }else {
                    for (AccessibilityNodeInfo nodeInfo1:childContents){
                        nodeInfo1.recycle();
                    }
                    for (AccessibilityNodeInfo nodeInfo2:childRedDots){
                        nodeInfo2.recycle();
                    }
                }
            }else {
                nodeInfo.recycle();
            }
        }
        if (unOpenPerson != null) {
            LogUtils.e(TAG, "点击微信列表上的某个人");
            unOpenPerson.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            unOpenPerson.recycle();
        }
    }

    /**
     * 遍历所有红包item，确认是否有未打开的红包
     * @param list
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void traversalChatNodes(List<AccessibilityNodeInfo> list) {
        AccessibilityNodeInfo unOpenRedPackage = null;
        for (int i=0; i<list.size(); i++){
            AccessibilityNodeInfo nodeInfo = list.get(i);
            List<AccessibilityNodeInfo> getRedPacketNodes = nodeInfo.findAccessibilityNodeInfosByText("领取红包");
            if (getRedPacketNodes != null && getRedPacketNodes.size() > 0){
                List<AccessibilityNodeInfo> remarkNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ae9");//红包备注TextView ZHU yuyang 版本更换需要修改的地方
                if (remarkNodes != null && remarkNodes.size()>0){
                    String remarkStr = remarkNodes.get(0).getText().toString();
                    //红包备注不包含敏感词
                    if (!SensitiveWordEngine.isContainSensitiveWord(remarkStr, 1)){
                        unOpenRedPackage = nodeInfo;
                    }
                    for (AccessibilityNodeInfo nodeInfo1:remarkNodes){
                        nodeInfo1.recycle();
                    }
                }else {
                    unOpenRedPackage = nodeInfo;
                }
                for (AccessibilityNodeInfo nodeInfo1:getRedPacketNodes){
                    nodeInfo1.recycle();
                }
            }else {
                nodeInfo.recycle();
            }
        }
        if (unOpenRedPackage != null) {
            LogUtils.e(TAG, "点击聊天列表上的红包item");
            unOpenRedPackage.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            inProgress = true;
            unOpenRedPackage.recycle();
        }
    }

    @Override
    public void onInterrupt() {
        Intent intent = new Intent();
        intent.setAction(MainActivity.ACTION);
        intent.putExtra(MainActivity.START_OR_STOP, 2);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(mListener);
        Intent intent = new Intent();
        intent.setAction(MainActivity.ACTION);
        intent.putExtra(MainActivity.START_OR_STOP, 2);
        startActivity(intent);
    }
}
