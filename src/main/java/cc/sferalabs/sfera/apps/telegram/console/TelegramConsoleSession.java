/**
 * 
 */
package cc.sferalabs.sfera.apps.telegram.console;

import java.util.EventListener;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.eventbus.Subscribe;

import cc.sferalabs.libs.telegram.bot.api.types.User;
import cc.sferalabs.sfera.access.Access;
import cc.sferalabs.sfera.console.ConsoleSession;
import cc.sferalabs.sfera.drivers.telegram.Telegram;
import cc.sferalabs.sfera.drivers.telegram.events.TelegramMessageEvent;
import cc.sferalabs.sfera.events.Bus;

/**
 *
 * @author Giampiero Baggiani
 *
 * @version 1.0.0
 *
 */
public class TelegramConsoleSession extends ConsoleSession implements EventListener {

	private final BlockingQueue<String> cmdQ = new ArrayBlockingQueue<>(10);
	private final TelegramConsole tc;
	private final User user;
	private final Telegram driver;

	/**
	 * @param name
	 */
	protected TelegramConsoleSession(TelegramMessageEvent e, TelegramConsole tc) {
		super("Telegram Console Session (" + e.getValue().getFrom().getId() + ")");
		this.user = e.getValue().getFrom();
		this.tc = tc;
		this.driver = (Telegram) e.getSource();
	}

	@Override
	protected boolean init() {
		tc.getLogger().debug("Sterted console session user: {} ({})", user.getFirstName(),
				user.getId());
		Bus.register(this);
		try {
			doOutput("User:");
			String usr = cmdQ.poll(30, TimeUnit.SECONDS);
			if (usr == null) {
				doOutput("Timeout expired");
				return false;
			}
			doOutput("Password:");
			String pswd = cmdQ.poll(30, TimeUnit.SECONDS);
			if (pswd == null) {
				doOutput("Timeout expired");
				return false;
			}
			cc.sferalabs.sfera.access.User u = Access.authenticate(usr, pswd);
			if (u == null || !u.isInRole("admin")) {
				tc.getLogger().warn("Attempted console access - Telegram user: {} ({}) user: {}",
						user.getFirstName(), user.getId(), usr);
				doOutput("Nope!");
				return false;
			}

			tc.getLogger().info("Granted console access - Telegram user: {} ({}) user: {}",
					user.getFirstName(), user.getId(), usr);
			doOutput("Granted - Input your commands:");
			return true;

		} catch (Exception e) {
			return false;
		}
	}

	@Override
	protected void cleanUp() {
		doOutput("Console stopped");
		Bus.unregister(this);
	}

	@Override
	public String acceptCommand() {
		try {
			String cmd = cmdQ.take();
			if (!tc.isEnabled() || tc.stopCmd.equals(cmd) || tc.startCmd.equals(cmd)) {
				return null;
			}
			return cmd;
		} catch (InterruptedException e) {
			return null;
		}
	}

	@Subscribe
	public void addToQueue(TelegramMessageEvent e) {
		if (e.getSource().getId().equals(driver.getId())) {
			String text = e.getValue().getText();
			if (text != null) {
				if (!cmdQ.offer(text)) {
					tc.getLogger().warn("Coudn't process command '{}'", text);
				}
			}
		}
	}

	@Override
	protected void doOutput(String text) {
		try {
			driver.sendMessage(user.getId(), text);
		} catch (Exception e) {
			tc.getLogger().error("Coudn't send message '" + text + "' - Quitting session", e);
			quit();
		}
	}

}
