package com.example.appointment.page;
//好友列表的页面


import java.util.ArrayList;
import java.util.HashMap;

import com.example.appointment.Adapter.UserInfoAdapter;
import com.example.appointment.R;
import com.example.appointment.chart.UserMessage;
import com.example.appointment.core.ImApp;
import com.example.appointment.group.GroupActivity;
import com.example.appointment.group.GroupMessage;
import com.example.appointment.message.ContactInfo;
import com.example.appointment.message.ContactInfoList;
import com.example.appointment.message.GroupInfo;
import com.example.appointment.message.GroupList;
import com.example.appointment.message.ThreadUtils;
import com.example.appointment.message.UserInfo;
import com.google.gson.Gson;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("ValidFragment")
public class TabTwo extends Fragment {

	Context t;

	public TabTwo(Context context) {
		super();
		t = context;
	}

	private ImageButton myfriend;
	private ImageButton group;
	private TextView groupt;
	int[] photoRes = {R.mipmap.ic_launcher};

	String[] groupFrom = {"groupImage", "groupName", "childCount"};
	int[] groupTo = {R.id.groupImage, R.id.groupName, R.id.childCount};
	String[] childFrom = {"userImage", "userName", "userSign", "userState", "userNumber"};
	int[] childTo = {R.id.ct_photo, R.id.ct_name, R.id.ct_sign, R.id.ct_online, R.id.ct_number};

	ArrayList<HashMap<String, Object>> groupData = null;
	ArrayList<ArrayList<HashMap<String, Object>>> childData = null;

	private ImApp app;

	int[] groupIndicator = {R.drawable.s_uncheck, R.drawable.s_check};//修改上下调图标
	ExpandableListView list = null;

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tabtwo, container, false);
	}

	@Override

	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
//		myfriend =  getActivity().findViewById(R.id.myfriend);
		group =  getActivity().findViewById(R.id.group);
		groupt=getActivity().findViewById(R.id.tv_group);



		group.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent w = new Intent(t, GroupActivity.class);
				startActivity(w);
			}
		});

		groupt.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent w = new Intent(t, GroupActivity.class);
				startActivity(w);
			}
		});

		groupData = new ArrayList<>();
		childData = new ArrayList<>();
		// 数据保存在application中
		app = (ImApp) getActivity().getApplication();
		// 获取长连接，往长连接里添加监听,时刻监听服务器返回来的消息,如果有消息到达，就执行onReceive
		// 好友列表的json串
		String buddyListJson = app.getBuddyListJson();
		Gson gson = new Gson();
		ContactInfoList Alist = gson.fromJson(buddyListJson,
				ContactInfoList.class);

		//解析列表后一个个添加到好友列表中
		for (ContactInfo a : Alist.buddyList) {
			String[] params = a.groupInfo.split("#");
			for (int i = 0; i < params.length; i++) {
				if (params[i].contains("" + app.getMyNumber())) {
					String inf = params[i].substring(6);
					addUser(new UserInfo(a, inf));
					break;
				}
			}
		}

		//不能用HashMap的实参赋给Map形参，只能new一个HashMap对象赋给Map的引用！
		UserInfoAdapter adapter = new UserInfoAdapter(getActivity(), groupData, R.layout.layout_group, groupFrom, groupTo, childData, R.layout.layout_child, childFrom, childTo, app);

		list = (ExpandableListView) getActivity().findViewById(R.id.qq_list);
		list.setAdapter(adapter);
		list.setGroupIndicator(null);
	}

	//添加到好友列表
	private void addUser(UserInfo user) {
		int i;
		for (i = 0; i < groupData.size(); i++) {
			if (groupData.get(i).get("groupName").toString().equals(user.groupInfo)) {
				break;
			}
		}
		if (i >= groupData.size()) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("groupImage", groupIndicator);
			map.put("groupName", user.groupInfo);
			map.put("childCount", 0);
			groupData.add(map);

			ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
			childData.add(list);
		}

		HashMap<String, Object> userData = new HashMap<String, Object>();
		userData.put("userImage", user.avatar);
		userData.put("userName", user.name);
		userData.put("userSign", user.sign);
		userData.put("userState", user.online);
		userData.put("userNumber", user.number);
		childData.get(i).add(userData);
		Integer count = (Integer) groupData.get(i).get("childCount") + 1;
		groupData.get(i).put("childCount", count);

	}
}