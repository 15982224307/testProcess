package com.supermap.process;

/**
 * @author wangchao
 * @DesktopJavaDocable disable
 */
public class Test32NewProcess {
	public static void main(String[] args) {
		System.out.println("this is a new process!!!");
		System.out.println("paramter is a " + args[0]);
		System.out.println(System.getProperty("sun.arch.data.model"));
	}
}
