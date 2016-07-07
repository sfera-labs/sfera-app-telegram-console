package cc.sferalabs.sfera.apps.telegram.console;

import org.slf4j.Logger;

import com.google.common.eventbus.Subscribe;

import cc.sferalabs.sfera.apps.Application;
import cc.sferalabs.sfera.core.Configuration;
import cc.sferalabs.sfera.drivers.telegram.events.TelegramMessageEvent;

public class TelegramConsole extends Application {

	String startCmd;
	String stopCmd;

	@Override
	protected void onEnable(Configuration config) {
		setConfigParams(config);
	}

	/**
	 * @param config
	 */
	private void setConfigParams(Configuration config) {
		startCmd = config.get("start_cmd", "/console_start");
		stopCmd = config.get("stop_cmd", "/console_stop");
	}

	@Subscribe
	public void handleMessage(TelegramMessageEvent e) {
		if (startCmd.equals(e.getValue().getText())) {
			TelegramConsoleSession tcs = new TelegramConsoleSession(e, this);
			tcs.start();
		}
	}

	@Override
	protected void onDisable() {
	}

	@Override
	protected void onConfigChange(Configuration config) {
		setConfigParams(config);
	}

	/**
	 * @return
	 */
	Logger getLogger() {
		return log;
	}

}
