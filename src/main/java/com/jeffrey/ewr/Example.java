package com.jeffrey.ewr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "sayhi")
public class Example extends AbstractMojo {

	public void execute() throws MojoExecutionException, MojoFailureException {
		// TODO Auto-generated method stub

		getLog().info("Hello daily HSF provided version check");

		try {

			File f = new File("antx.properties");
			if (!f.isFile()) {
				f = new File("/home/" + System.getProperty("user.name") + "/antx.properties");
				if (!f.isFile()) {
					getLog().info(" HSF provided version check skiped: can't find antx.properties");
					return;//
				}
			}
			Properties prop = new Properties();
			prop.load(new FileInputStream(f));

			// antx 中 hsf.daiy.check = false 表示无需check; 预发和线上设置为false.
			// 在日常和项目环境中： hsf.daiy.check=
			// ${hsfprovidename1}:${hsfprovidename2}:...@${projectDailyIp!}:${projectDailyIp2}:...
			// 例如cndcp 提供的hsf 服务有两个相关的配置，日常只有一台机器：
			// 则hsf.daily.check= hsf.daiy.check=hsf.cnschedule.provider.version:hsf.common.provider.version@10.125.2.200
			String checkValue = prop.getProperty("hsf.daiy.check");
			String[] dailyIps = null;
			String[] provideds = null;
			if (checkValue == null || checkValue.indexOf("@") <= 0 || checkValue.indexOf("@") == checkValue.length()) {
				if (checkValue == null || checkValue.equals("false")) {
					getLog().info(" HSF provided version check skiped: you set no need to check");
				} else {
					getLog().info(" HSF provided version check skiped: you set error format: [" + checkValue + "]");

				}
				return;

			} else {
				String[] ss = checkValue.split("@");
				provideds = ss[0].split(":");
				dailyIps = ss[1].split(":");

			}
			if (!isDailyIpCheck(dailyIps)) {
				// 非日常机器。需要校验提供的hsf版本号不能以 daily结尾
				this.wizardCheck(provideds, prop);
			}
			getLog().info("daily HSF provided version check end");
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private boolean isDailyIpCheck(String[] ips) throws SocketException {
		Set<String> dailyIps = new HashSet<String>();
		for (String ip : dailyIps) {
			dailyIps.add(ip);
		}
		Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();// 所有网络接口
		while (nets.hasMoreElements()) {
			NetworkInterface net = nets.nextElement();
			Enumeration<InetAddress> ias = net.getInetAddresses();
			while (ias.hasMoreElements()) {
				InetAddress ia = ias.nextElement();
				if (ia.getHostAddress() == null)
					continue;// 非ip
				else if (dailyIps.contains(ia.getHostAddress())) {
					getLog().info(" HSF provided version check skiped: daily manchine no need to check");
					return true; // 是日常机器IP,无需进行hsf版本号监测。
				}

			}
		}
		return false;
	}

	private void wizardCheck(String[] provideds, Properties prop) throws IOException {
		BufferedReader in = null;
		PrintWriter out = null;

		for (String provided : provideds) {
			String checkValue = prop.getProperty(provided);
			if (checkValue == null || !checkValue.endsWith("daily")) {
				getLog().info("HSF provided version check pass :" + provided + "=" + checkValue);
			} else {
				if (in == null) {
					in = new BufferedReader(new InputStreamReader(System.in));
				}
				if (out == null) {
					out = new PrintWriter(new OutputStreamWriter(System.out), true);
				}
				String input = "";

				while (true) {// 这里除非输入y或yes, 否则只能强制结束。
					StringBuffer buffer = new StringBuffer();
					buffer.append("╭──────┬─ ").append("HSF provided [:").append(provided + "=" + checkValue)
							.append("] may conflict with daily");

					print(buffer, out);
					print("│    you need to change this verison manual!  ", out);
					print("│    or you can input [yes] to skip this check! ", out);
					print("|_____________________________________Do you want to skip this check ? [yes]", out);

					input = in.readLine();
					input = input == null ? "" : input.trim().toLowerCase();
					if (input.equals("y") || input.equals("yes")) {
						print("----" + provided + " version check skip ", out);
						break;
					}
				}
			}

		}

	}

	private void print(Object message, PrintWriter out) {
		String messageString = message == null ? "" : message.toString();
		out.println(messageString);
		out.flush();

	}
}
