
//客户端的链接类，是连接功能的核心

package com.example.appointment.core;

import com.example.appointment.message.AMessage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

//对核心代码进行抽取，一共有四个公共的方法，分别是连接，断开连接，发送消息，接收消息
public class AConnection
{

	private String host = "";
	private int port = 8088;
	Socket client;
	private DataInputStream reader;
	private DataOutputStream writer;
	private WaitThread waitThread;
	public boolean isWaiting;

	// new出QQConnection对象的时候初始化IP地址和端口
	public AConnection(String host, int port) {
		super();
		this.host = host;
		this.port = port;
	}


	//创建与服务器之间的连接
	public void connect() throws IOException ,UnknownHostException{
		// 创建连接
		client = new Socket(host, port);
		reader = new DataInputStream(client.getInputStream());
		writer = new DataOutputStream(client.getOutputStream());
		// 创建连接的时候开启等待线程
		isWaiting = true;
		waitThread = new WaitThread();
		waitThread.start();

	}

	//断开与服务期间的连接
	public void disConnect() throws IOException {
		// 关闭连接就是释放资源
		client.close();
		reader.close();
		writer.close();
		isWaiting = false;
	}

	//发送xml消息
	public void sendMessage(String xml) throws IOException {
		// 发送消息要用到输入输出流，讲流作为类的成员变量，在创建连接的时候初始化，断开连接的时候释放资源
		// 发送消息其实就是把消息写出去
		writer.writeUTF(xml);

	}

	//发送java对象消息
	public void sendMessage(AMessage msg) throws IOException {
		writer.writeUTF(msg.toXml());
	}

	//等待线程 接收消息,由于不知道消息什么时候到达，需要一直等待着，等待消息的到达
	private class WaitThread extends Thread {

		@Override
		public void run() {
			super.run();
			while (isWaiting) {
				// 接收消息其实就是将消息读取到
				try {
					String xml = reader.readUTF();// 获取消息
					// 将消息转成Java对象
					AMessage msg = new AMessage();
					msg = (AMessage) msg.fromXml(xml);
					// 这里接收到消息，根据消息中保存的type字段来处理登录，获取联系人列表，登出等操作，将这一部分操作抽取出来一个接口，类似于按钮的点击事件那样，接收到消息就做操作
					/*
					 * 接收到消息之后，依次调用每个监听器的onReceive方法
					 */
					for (OnMessageListener listener : listeners) {
						listener.onReveive(msg);
					}

				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}

		}
	}

	// 服务器会经常给客户端发送消息，客户端会有不同的消息到来，所以新建一个监听器的集合，往集合中添加一个监听器就调用一次onReveive方法，
	//集合中有就调用，集合中没有就不调用
	private List<OnMessageListener> listeners = new ArrayList<OnMessageListener>();

	public void addOnMessageListener(OnMessageListener listener) {
		listeners.add(listener);
	}

	public void removeOnMessageListener(OnMessageListener listener) {
		listeners.remove(listener);
	}

	//消息的监听器接口，当有消息到来的时候就调用一次onReceive方法
	public static interface OnMessageListener {
		public void onReveive(AMessage msg);
	}
}
