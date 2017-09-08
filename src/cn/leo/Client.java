package cn.leo;

import java.util.concurrent.TimeUnit;

public class Client {

	public static void main(String[] args) {
		ClientManager clientManager = new ClientManager();
		// 以下测试代码循环发送消息到服务器
		int i = 0;
		try {
			while (true) {
				TimeUnit.MILLISECONDS.sleep(1000);
				String info = "I'm " + i++ + "-th information from client";
				clientManager.sendMsg(info);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
