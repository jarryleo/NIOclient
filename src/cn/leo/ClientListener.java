package cn.leo;

public interface ClientListener {
	/**
	 * ���ӳɹ�
	 */
	void onConnectSuccess();

	/**
	 * ����ʧ��
	 */
	void onConnectFailed();

	/**
	 * �����ж�
	 */
	void onIntercept();

	/**
	 * ���ݵ���
	 * 
	 * @param data
	 */
	void onDataArrived(byte[] data);
}
