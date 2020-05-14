package com.supermap.process;

/**
 * @author wangchao
 * @DesktopJavaDocable disable
 */
public class main {
	public static void main(String[] args) {
		System.out.println("this is a original process!!!");
		ProcessUtilities.startProcess(new String[]{"SUCCESS"}, true, "com.supermap.process.TestNewProcess");
		System.out.println();
		ProcessUtilities.start32Process(new String[]{"32 SUCCESS"}, true, "com.supermap.process.Test32NewProcess");
	}
}
