package com.yuyang.autoscrambleredpacket;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.yuyang.autoscrambleredpacket.keepAlive.ScreenManager;
import com.yuyang.autoscrambleredpacket.keepAlive.receiver.ScreenReceiverUtil;
import com.yuyang.autoscrambleredpacket.keepAlive.services.DaemonService;
import com.yuyang.autoscrambleredpacket.keepAlive.services.PlayerMusicService;
import com.yuyang.autoscrambleredpacket.utils.LogUtils;
import com.yuyang.autoscrambleredpacket.utils.SystemUtils;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 保活界面
 */
public class MainActivity extends AppCompatActivity {
    public static final String ACTION = "com.yuyang.autoscrambleredpacket";
    public static final String START_OR_STOP = "start_or_stop";//1-start 2-stop
    private static final String TAG = "MainActivity";

    private TextView mTvRunTime;
    private Button mBtnRun;

    private int timeSec;
    private int timeMin;
    private int timeHour;
    private Timer mRunTimer;
    private boolean isRunning;
    // 动态注册锁屏等广播
    private ScreenReceiverUtil mScreenListener;
    // 1像素Activity管理类
    private ScreenManager mScreenManager;
    // 电源管理器
    private PowerManager mPowerManager;
    // 唤醒锁
    private PowerManager.WakeLock mWakeLock;

    private ScreenReceiverUtil.ScreenStateListener mScreenListenerer = new ScreenReceiverUtil.ScreenStateListener() {
        @Override
        public void onScreenOn() {//开屏
            // 移除"1像素"
            //mScreenManager.finishActivity();
        }

        @Override
        public void onScreenOff() {//锁屏
            // 制造个"1像素"Activity
            mScreenManager.startActivity();
        }

        @Override
        public void onUserPresent() {
            // 解锁，暂不用，保留
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int status = intent.getIntExtra(START_OR_STOP, 0);
        if (status == 1 && !isRunning) {
            startRunning();
        }else if (status == 2 && isRunning){
            stopRunning();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        initView();
        //注册锁屏广播监听
        mScreenListener = new ScreenReceiverUtil(this);
        mScreenManager = ScreenManager.getScreenManagerInstance(this);
        mScreenListener.setScreenReceiverListener(mScreenListenerer);
        // 绑定此Activity到ScreenManager
        ScreenManager.getScreenManagerInstance(this).setActivity(this);

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        //如果抢红包辅助功能已启动
        if (SystemUtils.isAccessibilitySettingsOn(this)){
            startRunning();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtils.e(TAG,"----> 点亮亮屏");
        mWakeLock = mPowerManager.newWakeLock
                (PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "Tag");
        mWakeLock.acquire();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("屏蔽关键词").setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(this, ShieldKeyWordActivity.class);
        startActivity(intent);
        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        getSupportActionBar().setSubtitle(R.string.sub_title);
        mTvRunTime = (TextView)findViewById(R.id.tv_run_time);
        mBtnRun = (Button)findViewById(R.id.btn_run);
    }

    public void onScrambleingClick(View v){
        if (SystemUtils.isAccessibilitySettingsOn(this)) {//如果辅助功能已开启
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");
            builder.setMessage("请直接关闭\"微信自动抢红包\"辅助功能");
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            });
            builder.setPositiveButton("去关闭", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                }
            });
            builder.create().show();
        }else {//如果辅助功能未开启
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");
            builder.setMessage("请先开启\"微信自动抢红包\"辅助功能");
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            });
            builder.setPositiveButton("去开启", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                }
            });
            builder.create().show();
        }
    }

    private void startRunning(){
        mBtnRun.setText("停止抢红包");
        startRunTimer();
        //启动前台Service
        startDaemonService();
        //启动播放音乐Service
        startPlayMusicService();
        isRunning = true;
    }

    private void stopRunning(){
        mBtnRun.setText("开始抢红包");
        stopRunTimer();
        //关闭前台Service
        stopDaemonService();
        //关闭启动播放音乐Service
        stopPlayMusicService();
        isRunning = false;
    }

    /**
     * 开始播放无声音乐的Service
     */
    private void startPlayMusicService() {
        Intent intent = new Intent(this,PlayerMusicService.class);
        startService(intent);
    }

    /**
     * 停止音乐
     */
    private void stopPlayMusicService() {
        Intent intent = new Intent(this, PlayerMusicService.class);
        stopService(intent);
    }

    /**
     * 启动前台Service
     */
    private void startDaemonService() {
        Intent intent = new Intent(this, DaemonService.class);
        startService(intent);
    }

    /**
     * 停止前台Service
     */
    private void stopDaemonService() {
        Intent intent = new Intent(this, DaemonService.class);
        stopService(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 禁用返回键
        if(keyCode == KeyEvent.KEYCODE_BACK){
            if(isRunning){
                Toast.makeText(this,"正在抢红包，如果要退出请使用HOME键。",Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void startRunTimer() {
        TimerTask mTask = new TimerTask() {
            @Override
            public void run() {
                timeSec++;
                if(timeSec == 60){
                    timeSec = 0;
                    timeMin++;
                }
                if(timeMin == 60){
                    timeMin = 0;
                    timeHour++;
                }
                if(timeHour == 24){
                    timeSec = 0;
                    timeMin = 0;
                    timeHour = 0;
                }
                // 更新UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvRunTime.setText(timeHour+" : "+timeMin+" : "+timeSec);
                    }
                });
            }
        };
        mRunTimer = new Timer();
        // 每隔1s更新一下时间
        mRunTimer.schedule(mTask,1000,1000);
    }

    private void stopRunTimer(){
        if(mRunTimer != null){
            mRunTimer.cancel();
            mRunTimer = null;
        }
        timeSec = 0;
        timeMin = 0;
        timeHour = 0;
        mTvRunTime.setText(timeHour+" : "+timeMin+" : "+timeSec);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtils.e(TAG,"--->onDestroy");
        if (mWakeLock != null) {
            LogUtils.e(TAG,"----> 释放唤醒锁");
            mWakeLock.release();
            mWakeLock = null;
        }
        stopRunTimer();
        mScreenListener.stopScreenReceiverListener();

        if(!SystemUtils.isAPPALive(this,SystemUtils.getCurrentProcessName(this))){
            Intent intentAlive = new Intent(this, MainActivity.class);
            intentAlive.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentAlive);
            LogUtils.e(TAG,"SinglePixelActivity---->APP被干掉了，我要重启它");
        }
    }
}
