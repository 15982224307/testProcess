package com.supermap.process;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.*;
import com.sun.jna.win32.W32APIOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * @author wangchao
 * @DesktopJavaDocable disable
 * 启动进程通用类
 */

public class ProcessUtilities {
	private ProcessUtilities() {
		// Public method class does not provide a constructor
	}

	/**
	 * 启动一个新的进程
	 *
	 * @param params    传入口类参数
	 * @param isDebug   是否Debug模式
	 * @param className 主入口的类
	 * @return
	 */
	public static SubProcessThread startProcess(String[] params, boolean isDebug, String className) {
		SubProcessThread thread = new SubProcessThread(getArguments(params, isDebug, className, false));
		thread.start();
		return thread;
	}


	/**
	 * 启动一个32位的进程,其中jre和组件jar 都用32位的
	 *
	 * @param params     传入口类参数
	 * @param isDebug    是否Debug模式
	 * @param className  主入口的类
	 * @return
	 */


	private static String BIN_32_PATH = "C:\\Program Files\\Java\\jdk1.8_32\\jre";

	public static SubProcessThread start32Process(String[] params, boolean isDebug, String className) {
		SubProcessThread thread = new SubProcessThread(getArguments(params, isDebug, className, true), BIN_32_PATH);
		thread.start();
		return thread;
	}

	/**
	 * 将启动进程的传入参数组合
	 *
	 * @param params    传入口类参数
	 * @param isDebug   是否Debug模式
	 * @param className 主入口的类
	 * @return
	 */
	private static ArrayList<String> getArguments(String[] params, boolean isDebug, String className, boolean bin32) {
		ArrayList<String> arguments = new ArrayList<>();
		try {

			String javaExeHome;
			String javaExe = "java.exe";
			if (!bin32) {
				javaExeHome = System.getProperty("java.home");
			} else {
				javaExeHome = BIN_32_PATH;
			}
			if (javaExeHome.isEmpty()) {
				javaExeHome = "." + File.separator + "jre" + File.separator + "bin";
				javaExeHome = javaExeHome + File.separator + "java.exe";
			} else if (!javaExeHome.endsWith("bin")) {
				javaExeHome = javaExeHome + File.separator + "bin" + File.separator + javaExe;
			}
			arguments.add(javaExeHome);
			//开启debug模式，可以在idea中使用remote来调试新进程
			if (isDebug) {
				//需要一个没有被占用的端口
				String portStr = String.valueOf((int) ((Math.random() * 9 + 1) * 10000));
				System.out.println("debug " + className + " at " + portStr + " port");
				arguments.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" + portStr);
			}

			File classpathJarFile = createClasspathJarFile("D:\\testProcess\\target\\classes");
			String jarFilePath = classpathJarFile.getAbsolutePath();
			arguments.add("-classpath");
			arguments.add(jarFilePath);
			arguments.add(className);
			if (null != params) {
				for (String param1 : params) {
					String param = param1.endsWith(File.separator) ? param1.substring(0, param1.length() - 1) : param1;
					arguments.add("\"" + param + "\"");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return arguments;
	}

	//将编译好的class文件打成jar包
	private static File createClasspathJarFile(String classPath) throws IOException {
		JarOutputStream out = null;
		final File jar = File.createTempFile("storm-", ".jar", new File(System.getProperty("java.io.tmpdir")));
		System.out.println("jar包路径:" + jar.getAbsolutePath());
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				//运行完成后删除jar包
				jar.delete();
			}
		});
		try {
			File path = new File(classPath);
			Manifest manifest = new Manifest();
			manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
			manifest.getMainAttributes().putValue("Created-By", "JarPackageUtil");
			out = new JarOutputStream(new FileOutputStream(jar), manifest);
			writeBaseFile(out, path, "");
		} finally {
			out.flush();
			out.close();
		}
		return jar;
	}

	private static void writeBaseFile(JarOutputStream out, File file, String base) throws IOException {
		if (file.isDirectory()) {
			File[] fl = file.listFiles();
			if (base.length() > 0) {
				base = base + "/";
			}
			for (File value : fl) {
				writeBaseFile(out, value, base + value.getName());
			}
		} else {
			out.putNextEntry(new JarEntry(base));
			try (FileInputStream in = new FileInputStream(file)) {
				byte[] buffer = new byte[1024];
				int n = in.read(buffer);
				while (n != -1) {
					out.write(buffer, 0, n);
					n = in.read(buffer);
				}
			}
		}
	}


	/**
	 * @param command
	 * @param args
	 * @DesktopJavaDocable disable
	 */
	public static void executeAsAdministrator(String command, String args) {
		Shell32X.SHELLEXECUTEINFO execInfo = new Shell32X.SHELLEXECUTEINFO();
		execInfo.lpFile = new WString(command);
		if (args != null) {
			execInfo.lpParameters = new WString(args);
		}
		execInfo.nShow = Shell32X.SW_SHOWDEFAULT;
		execInfo.fMask = Shell32X.SEE_MASK_NOCLOSEPROCESS;
		execInfo.lpVerb = new WString("runas");
		boolean result = Shell32X.INSTANCE.ShellExecuteEx(execInfo);

		if (!result) {
			int lastError = Kernel32.INSTANCE.GetLastError();
			String errorMessage = Kernel32Util.formatMessageFromLastErrorCode(lastError);
			throw new RuntimeException("Error performing elevation: " + lastError + ": " + errorMessage + " (apperror=" + execInfo.hInstApp + ")");
		}
	}

	public interface Shell32X extends Shell32 {

		// https://stackoverflow.com/questions/11041509/elevating-a-processbuilder-process-via-uac
		// 不要删除无用的字段，给反射用的

		Shell32X INSTANCE = Native.loadLibrary("shell32", Shell32X.class, W32APIOptions.UNICODE_OPTIONS);

		int SW_HIDE = 0;
		int SW_MAXIMIZE = 3;
		int SW_MINIMIZE = 6;
		int SW_RESTORE = 9;
		int SW_SHOW = 5;
		int SW_SHOWDEFAULT = 10;
		int SW_SHOWMAXIMIZED = 3;
		int SW_SHOWMINIMIZED = 2;
		int SW_SHOWMINNOACTIVE = 7;
		int SW_SHOWNA = 8;
		int SW_SHOWNOACTIVATE = 4;
		int SW_SHOWNORMAL = 1;

		/**
		 * File not found.
		 */
		int SE_ERR_FNF = 2;

		/**
		 * Path not found.
		 */
		int SE_ERR_PNF = 3;

		/**
		 * Access denied.
		 */
		int SE_ERR_ACCESSDENIED = 5;

		/**
		 * Out of memory.
		 */
		int SE_ERR_OOM = 8;

		/**
		 * DLL not found.
		 */
		int SE_ERR_DLLNOTFOUND = 32;

		/**
		 * Cannot share an open file.
		 */
		int SE_ERR_SHARE = 26;


		int SEE_MASK_NOCLOSEPROCESS = 0x00000040;


		int ShellExecute(int i, String lpVerb, String lpFile, String lpParameters, String lpDirectory, int nShow);

		boolean ShellExecuteEx(SHELLEXECUTEINFO lpExecInfo);


		class SHELLEXECUTEINFO extends Structure {
        /*
  DWORD     cbSize;
  ULONG     fMask;
  HWND      hwnd;
  LPCTSTR   lpVerb;
  LPCTSTR   lpFile;
  LPCTSTR   lpParameters;
  LPCTSTR   lpDirectory;
  int       nShow;
  HINSTANCE hInstApp;
  LPVOID    lpIDList;
  LPCTSTR   lpClass;
  HKEY      hkeyClass;
  DWORD     dwHotKey;
  union {
    HANDLE hIcon;
    HANDLE hMonitor;
  } DUMMYUNIONNAME;
  HANDLE    hProcess;
         */

			public int cbSize = size();
			public int fMask;
			public WinDef.HWND hwnd;
			public WString lpVerb;
			public WString lpFile;
			public WString lpParameters;
			public WString lpDirectory;
			public int nShow;
			public WinDef.HINSTANCE hInstApp;
			public Pointer lpIDList;
			public WString lpClass;
			public WinReg.HKEY hKeyClass;
			public int dwHotKey;

			/*
			 * Actually:
			 * union {
			 *  HANDLE hIcon;
			 *  HANDLE hMonitor;
			 * } DUMMYUNIONNAME;
			 */
			public WinNT.HANDLE hMonitor;
			public WinNT.HANDLE hProcess;

			protected List getFieldOrder() {
				return Arrays.asList("cbSize", "fMask", "hwnd", "lpVerb", "lpFile", "lpParameters",
						"lpDirectory", "nShow", "hInstApp", "lpIDList", "lpClass",
						"hKeyClass", "dwHotKey", "hMonitor", "hProcess");
			}
		}

	}
}
