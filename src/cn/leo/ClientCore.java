package cn.leo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class ClientCore extends Thread {
	private static final int INT_LENGTH = 4; // һ��int ռ4��byte
	private static final int BUFFER_CACHE = 1024; // ��������С
	private static final int TIME_OUT = 3000; // Ƶ��������ʱʱ��
	private String mIp; // ������IP��ַ
	private int mPort; // �������˿ں�
	private ClientListener mListener; // �ӿڻص�
	private Selector selector;
	private ByteBuffer buffer;
	private SocketChannel socketChannel;

	public static ClientCore startClient(String ip, int port, ClientListener listener) {

		ClientCore clientCore = new ClientCore(ip, port, listener);
		clientCore.start();
		return clientCore;
	}

	private ClientCore(String ip, int port, ClientListener listener) {
		mIp = ip;
		mPort = port;
		mListener = listener;
		buffer = ByteBuffer.allocate(BUFFER_CACHE);
		try {
			selector = Selector.open(); // ����ѡ����
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		client();
	}

	private void client() {
		try {
			socketChannel = SocketChannel.open(); // ����Ƶ��
			socketChannel.configureBlocking(false); // Ƶ������������
			socketChannel.connect(new InetSocketAddress(mIp, mPort));// ���ӷ�����
			socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(BUFFER_CACHE)); // ��Ƶ����ѡ����

			while (!socketChannel.finishConnect()) {
				try {
					TimeUnit.SECONDS.sleep(1); // 1����һ���Ƿ�����
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (socketChannel.finishConnect()) { // ������ӳɹ�����ѭ��������Ϣ
				if (mListener != null) {
					mListener.onConnectSuccess();// ������
					excuteSelector();
				}
			}

		} catch (Exception e) {

		} finally {
			if (mListener != null) {
				mListener.onConnectFailed();// ����ʧ��
			}
			close();
		}
	}

	public void excuteSelector() {

		try {
			while (true) {
				if (selector.select(TIME_OUT) == 0) {
					continue;
				}
				Iterator<SelectionKey> iter = selector.selectedKeys().iterator(); // ����Ƶ��ѡ����������֪ͨ�������Ӿͻ�ȡ
				while (iter.hasNext()) {
					SelectionKey key = iter.next();
					if (key.isReadable()) { // ����Ƕ�ȡ����
						handleRead(key);
					} else if (key.isWritable() && key.isValid()) { // ��д����
						handleWrite(key);
					} else if (key.isConnectable()) { // ���ӳɹ�

					}
					iter.remove(); // �����Ӷ����Ƴ�
				}
			}
		} catch (IOException e) {
			if (mListener != null) {
				mListener.onIntercept();
				mListener = null;
			}
			close();
		}
	}

	/**
	 * ��������
	 * 
	 * @param bytes
	 */
	public void sendMsg(byte[] bytes) {

		try {
			if (socketChannel.isConnected()) { // ������ӳɹ�����ѭ��������Ϣ
				int length = bytes.length; // Ҫ�������ݵĳ��ȣ�������ȴ��ڻ������ͷֶη���
				int start = 0; // �ֶ���ʼ��
				while (length > 0) {
					int part = 0; // ÿ�δ�С
					if (length >= (BUFFER_CACHE - INT_LENGTH)) {
						part = (BUFFER_CACHE - INT_LENGTH);
					} else {
						part = length % (BUFFER_CACHE - INT_LENGTH); // ���һ�δ�С��
					}
					byte[] b = new byte[part]; // �ֶ�����

					System.arraycopy(bytes, start, b, 0, part);// ���Ʒֶ�����
					// д����������
					buffer.clear(); // ���������
					if (start == 0) {
						buffer.putInt(length); // ��һ�ηֶ�ͷд�������ݳ���
					}
					buffer.put(b);// ���ַ������ֽ�����д�뻺����
					buffer.flip();// ���û�����limit
					while (buffer.hasRemaining()) {
						socketChannel.write(buffer); // ����������д��Ƶ��
					}
					start += part;
					length -= part;
				}
			}
		} catch (Exception e) {
			close();
		}
	}

	/**
	 * �����ȡ����
	 * 
	 * @param key
	 * @throws IOException
	 */
	private void handleRead(SelectionKey key) throws IOException {
		SocketChannel sc = (SocketChannel) key.channel(); // ��ȡkey��Ƶ��
		ByteBuffer headBuffer = ByteBuffer.allocate(INT_LENGTH); // 1��intֵ��ͷ�ֽڴ洢���ݳ���
		ByteBuffer buf = (ByteBuffer) key.attachment(); // ��ȡkey�ĸ��Ӷ�����Ϊ���ӵĻ�������
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		while (sc.read(headBuffer) == INT_LENGTH) {
			int dataLength = headBuffer.getInt(0); // ��ȡ����ͷ��4���ֽڵ�intֵ���������ݳ���
			headBuffer.clear();
			byte[] bytes;
			int receiveLength = 0; // �ѽ��ܳ���
			int bytesRead = 0;// ��ȡƵ���ڵ����ݵ�������
			while (receiveLength < dataLength) {
				if (dataLength - receiveLength < buf.capacity()) {
					buf.limit(dataLength - receiveLength);
				}
				bytesRead = sc.read(buf); // TODO ������BUGҪ����
				buf.flip();// ���û�����limit

				if (bytesRead < 1) { // ��ȡ���������˳�
					break;
				}

				if (receiveLength + bytesRead > dataLength) { // ������ܵ����ݴ���ָ������
					bytes = new byte[dataLength - receiveLength]; // ���µ�����Ϊʣ�����ݳ���
				} else {
					bytes = new byte[bytesRead]; // ����Ϊ��ȡ����
				}
				buf.get(bytes);
				baos.write(bytes);
				buf.clear();// ��ջ�����
				receiveLength += bytes.length; // �Ѷ�ȡ�����ݳ���
			}
			if (mListener != null) {
				mListener.onDataArrived(baos.toByteArray());
			}
			baos.reset();
			if (bytesRead == -1) { // �������Ͽ����ӣ��ر�Ƶ��
				sc.close();
			}
		}

	}

	/**
	 * ����д������
	 * 
	 * @param key
	 * @throws IOException
	 */
	private void handleWrite(SelectionKey key) throws IOException {
		ByteBuffer buf = (ByteBuffer) key.attachment(); // ��ȡkey���ӵĻ�����
		SocketChannel sc = (SocketChannel) key.channel(); // ��ȡkey��Ƶ��

		buf.flip();// ���û�����limit
		while (buf.hasRemaining()) {
			sc.write(buf); // д�뻺�������ݵ�Ƶ��
		}
		buf.compact(); //

	}

	// �쳣�ر�����
	private void close() {
		try {
			if (selector != null) {
				selector.close();
			}
			if (socketChannel != null) {
				socketChannel.close();
			}
		} catch (IOException e1) {
			// e1.printStackTrace();
		}
	}
}
