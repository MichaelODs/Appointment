package com.example.appointment.page;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.appointment.Adapter.ContentAdapter;
import com.example.appointment.Adapter.ContentModel;
import com.example.appointment.R;
import com.example.appointment.View.FeedbackActivity;
import com.example.appointment.View.LoginActivity;
import com.example.appointment.View.SearchActivity;
import com.example.appointment.View.SettingActivity;
import com.example.appointment.chart.ChartActivity;
import com.example.appointment.chart.UserMessage;
import com.example.appointment.core.AConnection;
import com.example.appointment.core.ImApp;
import com.example.appointment.group.GroupChart;
import com.example.appointment.group.GroupMessage;
import com.example.appointment.message.AMessage;
import com.example.appointment.message.AMessageType;
import com.example.appointment.message.ContactInfo;
import com.example.appointment.message.ContactInfoList;
import com.example.appointment.message.GroupInfo;
import com.example.appointment.message.GroupList;
import com.example.appointment.message.ThreadUtils;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.example.appointment.View.LoginActivity.t;

/**
 * Created by MichaelOD on 2017/12/28.
 */

public class Main extends FragmentActivity implements View.OnClickListener {

    private DrawerLayout drawerLayout;
    private List<ContentModel> list;
    private ContentAdapter adapter;
    private ImageView leftMenu, rightMenu;
    private ListView listView;
    private FragmentManager fm;
    private LinearLayout ll_tab1_message;
    private LinearLayout ll_tab1_friend;
    private LinearLayout ll_tab1_activity;

    private TextView message;
    private TextView friend;
    private TextView activity;
    private TextView maintext;
    private TextView usersign;
    private LinearLayout user_message;
    ImApp app;
    public int h = R.id.ll_tab1_message;//目前显示的页面变量
    public static boolean on1 = false; //私聊界面开启的开关
    public static boolean on2 = false; //群聊界面开启的开关
    public static boolean on3 = false; //其他界面开启的开关
    private boolean exit = false;        //退出的开关

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        leftMenu = (ImageView) findViewById(R.id.leftmenu);
        rightMenu = (ImageView) findViewById(R.id.rightmenu);
        user_message = (LinearLayout) findViewById(R.id.user_message);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerlayout);
        fm = getSupportFragmentManager();
        ChartActivity.u = this;
        GroupChart.u = this;

        ll_tab1_message = (LinearLayout) findViewById(R.id.ll_tab1_message);
        ll_tab1_friend = (LinearLayout) findViewById(R.id.ll_tab1_friend);
        ll_tab1_activity = (LinearLayout) findViewById(R.id.ll_tab1_activity);
        message = (TextView) findViewById(R.id.tv_message);
        friend = (TextView) findViewById(R.id.tv_friend);
        activity = (TextView) findViewById(R.id.tv_activity);
        maintext = (TextView) findViewById(R.id.maintext);
        message.setTextColor(getResources().getColor(R.color.SDUred));

        ll_tab1_message.setOnClickListener(this);
        ll_tab1_friend.setOnClickListener(this);
        ll_tab1_activity.setOnClickListener(this);

        // 数据保存在application中
        app = (ImApp) getApplication();
        // 获取长连接，往长连接里添加监听,时刻监听服务器返回来的消息,如果有消息到达，就执行onReceive
        app.getMyConn().addOnMessageListener(listener);

        //心跳包线程开启
        ThreadUtils.runInSubThread(new Runnable() {
            public void run() {
                try {
                    while (!exit) {
                        AMessage msg = new AMessage();
                        msg.type = AMessageType.MSG_TYPE_ONLINE;
                        msg.from = app.getMyNumber();
                        app.getMyConn().sendMessage(msg);
                        Thread.sleep(3000);
                    }
                } catch (IOException e) {
                    if (!exit) {
                        ThreadUtils.runInUiThread(new Runnable() {

                            public void run() {
                                app.setstate(false);
                                Intent p = new Intent(Main.this, LoginActivity.class);
                                Toast.makeText(getBaseContext(), "您的网络出现异常，请重新登录！", Toast.LENGTH_SHORT).show();
                                startActivity(p);
                                finish();
                            }
                        });
                    }
                } catch (Exception e) {
                    Toast.makeText(getBaseContext(), "抱歉，程序出现意外异常！", Toast.LENGTH_SHORT).show();
                }
            }
        });

        TextView textView = (TextView) findViewById(R.id.username);
        textView.setText(app.getMyName());
        usersign = (TextView) findViewById(R.id.user_sign);
        String newBuddyListJson = app.getBuddyListJson();
        Gson gson = new Gson();
        ContactInfoList newList = gson.fromJson(newBuddyListJson, ContactInfoList.class);
        usersign.setText(newList.get(app.getMyNumber()).sign);

        //默认加载第一个fragment
        getSupportFragmentManager().beginTransaction()
                .add(R.id.content, new TabOne(Main.this))
                .commit();

        //以下这段给ListView赋值
        listView = (ListView) findViewById(R.id.left_listview);
        initData();
        adapter = new ContentAdapter(this, list);
        listView.setAdapter(adapter);

        //调出左边菜单的按钮设置
        leftMenu.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(Gravity.LEFT);
            }
        });

        user_message.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(Main.this, UserMessage.class);
                intent.putExtra("number", app.getMyNumber());
                startActivity(intent);
            }
        });

        rightMenu.setOnClickListener(new View.OnClickListener() {

                        @Override
            public void onClick(View v) {
                startActivity(new Intent(Main.this,SearchActivity.class));

            }
            //查找按钮
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()

    {

//			每次点击之后，就用相应的Fragment替换主界面的LinearLayout，
//			当然，替换完成之后要记得关闭左边的侧拉菜单，传入的参数为Gravity.LEFT表示关闭左边的侧拉菜单，。

        @Override
        public void onItemClick (AdapterView < ? > parent, View view,
        int position, long id){
        FragmentTransaction bt = fm.beginTransaction();
        switch ((int) id) {
            case 1:
                Intent intent = new Intent(Main.this, UserMessage.class);
                // 将账号和个性签名带到下一个activity
                intent.putExtra("number", app.getMyNumber());
                startActivity(intent);
                break;
            case 2:
                startActivity(new Intent(Main.this, FeedbackActivity.class));
                break;
            case 3:
                startActivity(new Intent(Main.this, SettingActivity.class));
                break;
            default:
                break;
        }
        bt.commit();
        drawerLayout.closeDrawer(Gravity.LEFT);
    }
    });

}

    //主界面消息监听器
    private AConnection.OnMessageListener listener = new AConnection.OnMessageListener() {

        public void onReveive(final AMessage msg) {
            // 接收服务器返回的结果.处理数据的显示,运行在主线程中
            ThreadUtils.runInSubThread(new Runnable() {

                public void run() {
                    if (AMessageType.MSG_TYPE_BUDDYLIST.equals(msg.type)) {

                        // 如有服务器返回好友列表，则说明有好友上线\
                        // 有好友上线// 新上线好友的信息json串
                        String newBuddyListJson = msg.content;
                        app.setBuddyListJson(newBuddyListJson);
                        ThreadUtils.runInUiThread(new Runnable() {

                            public void run() {
                                if (!on3 && !on1 && !on2) {
                                    if (h == R.id.ll_tab1_friend) {
                                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                        transaction.replace(R.id.content, new TabTwo(Main.this));
                                        transaction.commit();
                                    }
                                }
                                String newBuddyListJson = app.getBuddyListJson();
                                Gson gson = new Gson();
                                ContactInfoList newList = gson.fromJson(newBuddyListJson, ContactInfoList.class);
                                usersign.setText(newList.get(app.getMyNumber()).sign);
                            }
                        });

                    } else if (AMessageType.MSG_TYPE_CHAT_P2P.equals(msg.type)) {
                        ThreadUtils.runInUiThread(new Runnable() {

                            public void run() {
                                if (!on1) {
                                    app.addMessage(msg);
                                    if (h == R.id.ll_tab1_message && !on2 && !on3) {
                                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                        transaction.replace(R.id.content, new TabOne(Main.this));
                                        transaction.commit();
                                    }
                                }
                            }
                        });

                    } else if (AMessageType.MSG_TYPE_CHAT_ROOM.equals(msg.type)) {
                        ThreadUtils.runInUiThread(new Runnable() {

                            public void run() {
                                if (!on2) {
                                    app.addMessage(msg);
                                    if (h == R.id.ll_tab1_message && !on1 && !on3) {
                                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                        transaction.replace(R.id.content, new TabOne(Main.this));
                                        transaction.commit();
                                    }
                                }
                            }
                        });
                    } else if (AMessageType.MSG_TYPE_ADDFRIEND.equals(msg.type)) {
                        ThreadUtils.runInUiThread(new Runnable() {

                            public void run() {
                                app.addMessage(msg);
                                if (h == R.id.ll_tab1_message && !on2 && !on1 && !on3) {
                                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                    transaction.replace(R.id.content, new TabOne(Main.this));
                                    transaction.commit();
                                }
                            }
                        });

                    } else if (AMessageType.MSG_TYPE_GROUPLIST.equals(msg.type)) {
                        app.setGroupListJson(msg.content);
                    } else if (AMessageType.MSG_TYPE_PLANLIST.equals(msg.type)) {
                        app.setPlanListJson(msg.content);
                    }
                }
            });
        }
    };

    //底部按钮切换的点击监听器
    public void onClick(View view) {
        setTabImageNormal();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        h = view.getId();
        switch (h) {
            case R.id.ll_tab1_message:
                transaction.replace(R.id.content, new TabOne(Main.this));
                message.setTextColor(getResources().getColor(R.color.SDUred));
                maintext.setText("消息");
                transaction.commit();

                break;
            case R.id.ll_tab1_friend:
                transaction.replace(R.id.content, new TabTwo(Main.this));
                friend.setTextColor(getResources().getColor(R.color.SDUred));
                maintext.setText("好友");
                transaction.commit();

                break;
            case R.id.ll_tab1_activity:
                transaction.replace(R.id.content, new TabThree(Main.this));
                activity.setTextColor(getResources().getColor(R.color.SDUred));
                maintext.setText("活动");
                transaction.commit();
                break;
            default:
                break;
        }
    }
    //将底部按钮设置回正常
    public void setTabImageNormal() {

        int normalColor = getResources().getColor(R.color.black);
        message.setTextColor(normalColor);
        friend.setTextColor(normalColor);
        activity.setTextColor(normalColor);
    }

    //设置侧拉菜单
    private void initData() {
        list = new ArrayList<ContentModel>();

        //初始化左侧菜单的选项
        list.add(new ContentModel(R.drawable.imformationicon, "个人信息", 1));
        list.add(new ContentModel(R.drawable.feedbackicon, "意见反馈", 2));
        list.add(new ContentModel(R.drawable.settingicon, "设置", 3));
        list.add(new ContentModel(R.drawable.administratoricon, "管理员", 4));

    }


    //按返回键的确认窗口
    public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle("确定要退出吗？")
                .setIcon(android.R.drawable.ic_menu_help)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“确认”后的操作
                        ThreadUtils.runInSubThread(new Runnable() {
                            public void run() {
                                try {
                                    AMessage msg = new AMessage();
                                    msg.type = AMessageType.MSG_TYPE_LOGOUT;
                                    msg.from = app.getMyNumber();
                                    app.getMyConn().sendMessage(msg);
                                    exit = true;
                                    try {
                                        app.getMyConn().disConnect();
                                    } catch (IOException e) {
                                    }
                                    app.clearList();
                                    finish();
                                } catch (Exception e) {
                                    finish();
                                }
                            }
                        });

                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 点击“返回”后的操作,这里不设置没有任何操作
                    }
                }).show();
    }


    protected void onDestroy() {
        super.onDestroy();
        // activity销毁的时候取消监听
        if (!exit) {
            ThreadUtils.runInSubThread(new Runnable() {
                public void run() {
                    try {
                        AMessage msg = new AMessage();
                        msg.type = AMessageType.MSG_TYPE_LOGOUT;
                        msg.from = app.getMyNumber();
                        app.getMyConn().sendMessage(msg);
                        app.getMyConn().disConnect();
                        app.clearList();
                    } catch (Exception e) {
                        finish();
                    }
                }
            });
        }
    }

    //切回时刷新页面
    protected void onStart() {
        super.onStart();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        switch (h) {
            case R.id.ll_tab1_message:
                transaction.replace(R.id.content, new TabOne(Main.this));
                message.setTextColor(getResources().getColor(R.color.SDUred));
                maintext.setText("消息");
                transaction.commit();

                break;
            case R.id.ll_tab1_friend:
                transaction.replace(R.id.content, new TabTwo(Main.this));
                friend.setTextColor(getResources().getColor(R.color.SDUred));
                maintext.setText("好友");
                transaction.commit();
                break;
            case R.id.ll_tab1_activity:
                transaction.replace(R.id.content, new TabThree(Main.this));
                activity.setTextColor(getResources().getColor(R.color.SDUred));
                maintext.setText("活动");
                transaction.commit();
                break;
            default:
                break;
        }
    }

}