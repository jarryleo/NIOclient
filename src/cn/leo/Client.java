package cn.leo;

import java.util.concurrent.TimeUnit;

public class Client {

	public static void main(String[] args) {
		ClientManager clientManager = new ClientManager();
		// ���²��Դ���ѭ��������Ϣ��������
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
