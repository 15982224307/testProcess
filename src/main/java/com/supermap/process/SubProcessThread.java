package com.supermap.process;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.Executors;

/**
 * @author wangchao
 * @DesktopJavaDocable disable
 */
public class SubProcessThread extends Thread {

	private ArrayList<String> arguments;
	private String environmentPath = "";

	private Process process;

	/**
	 * @param arguments
	 */
	public SubProcessThread(ArrayList<String> arguments) {
		this.arguments = arguments;
	}


	public SubProcessThread(ArrayList<String> arguments, String environmentPath) {
		this.arguments = arguments;
		this.environmentPath = environmentPath;
	}

	@Override
	public void run() {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder();
			//// 修改下环境变量中地址的值，调用32位组件架需要
			if (!environmentPath.isEmpty()) {
				processBuilder.environment().put("path", environmentPath);
			}
			processBuilder.directory(new File("d:\\"));
			processBuilder.command(arguments);
			processBuilder.redirectErrorStream(false);
			process = processBuilder.start();
			final InputStream errorStream = process.getErrorStream();
			final InputStream intputStream = process.getInputStream();
			Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 2).execute(() -> {
				while (true) {
					try {
						int value = errorStream.read();
						if (value != -1) {
							System.out.print((char) value);
						} else {
							break;
						}
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
				}
			});
			Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 2).execute(() -> {
				while (true) {
					try {
						BufferedInputStream in = new BufferedInputStream(intputStream);
						BufferedReader br = new BufferedReader(new InputStreamReader(in));
						String s;
						while ((s = br.readLine()) != null) {
							System.out.println(s);
						}

						int value = intputStream.read();
						if (value == -1) {
							break;
						}
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
				}
			});

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
