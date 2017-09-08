package cn.leo;

import java.util.concurrent.TimeUnit;

public class ClientManager implements ClientListener {
//	private static String ip = "119.29.253.156";
	private static String ip = "127.0.0.1";
	private static int port = 25627;
	private ClientCore client;

	public ClientManager() {
		client = ClientCore.startClient(ip, port, this); //���ӷ�����
	}

	public void sendMsg(String msg) {
		if (client != null) {
			client.sendMsg(msg.getBytes());
		}
	}

	@Override
	public void onIntercept() {
		Logger.d("�����쳣�Ͽ���5����Զ���������");
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		client = ClientCore.startClient(ip, port, this);// �������ӷ�����
	}

	@Override
	public void onDataArrived(byte[] data) {
		String msg = new String(data);
		Logger.i(msg);
	}

	@Override
	public void onConnectSuccess() {
		Logger.d("���ӷ������ɹ���");
	}

	@Override
	public void onConnectFailed() {
		Logger.d("���ӷ�����ʧ�ܣ�5����Զ���������");
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		client = ClientCore.startClient(ip, port, this);// �������ӷ�����
	}

}
