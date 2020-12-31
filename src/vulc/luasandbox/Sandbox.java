package vulc.luasandbox;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;

import javax.swing.KeyStroke;

import vulc.bitmap.Bitmap;
import vulc.bitmap.IntBitmap;
import vulc.bitmap.font.Font;
import vulc.luasandbox.input.InputHandler;
import vulc.luasandbox.input.InputHandler.Key;
import vulc.luasandbox.script.ScriptCore;
import vulc.vdf.VDFObject;

public class Sandbox extends Canvas implements Runnable {

	private static final long serialVersionUID = 1L;

	public static final String SCRIPT_FOLDER = "./scripts/";

	private static final Sandbox INSTANCE = new Sandbox();
	public static final VDFObject CONFIG = new VDFObject();

	// keys
	private static final InputHandler INPUT_HANDLER = new InputHandler();
	private static final Key RESTART_SCRIPT = INPUT_HANDLER.new Key();

	public static int width, height;
	private static int scale;

	public static Bitmap<Integer> screen;

	private static BufferedImage img;
	private static int[] pixels;

	private static int ticks = 0;

	public void run() {
		int frames = 0;

		long tps = CONFIG.getInt("tps");
		long fps = CONFIG.getInt("fps");

		long nanosPerTick = 1_000_000_000 / tps;
		long nanosPerFrame = 1_000_000_000 / fps;

		long lastTime = System.nanoTime();
		long unprocessedTime = 0;
		long unrenderedTime = 0;

		while(true) {
			long now = System.nanoTime();
			long passedTime = now - lastTime;
			lastTime = now;

			if(passedTime > 1_000_000_000) passedTime = 1_000_000_000;
			else if(passedTime < 0) passedTime = 0;

			unprocessedTime += passedTime;
			unrenderedTime += passedTime;

			while(unprocessedTime >= nanosPerTick) {
				unprocessedTime -= nanosPerTick;

				tick();
				ticks++;

				if(ticks % tps == 0) {
					System.out.println(frames + " fps");
					frames = 0;
				}
			}

			if(unrenderedTime >= nanosPerFrame) {
				unrenderedTime %= nanosPerFrame;

				render();
				frames++;
			}

			try {
				Thread.sleep(8);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void readConfig() {
		try(Reader in = new BufferedReader(new FileReader(SCRIPT_FOLDER + "config.vdf"))) {
			CONFIG.parse(in);
		} catch(Exception e) {
			e.printStackTrace();
		}

		// screen config
		VDFObject screenConfig = CONFIG.getObject("screen");

		width = screenConfig.getInt("width");
		height = screenConfig.getInt("height");
		scale = screenConfig.getInt("scale");

		screen = new IntBitmap(width, height);

		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
	}

	private void init() {
		requestFocus();
		INPUT_HANDLER.init(this);

		// register key bindings
		VDFObject keyBindings = CONFIG.getObject("keyBindings");
		RESTART_SCRIPT.setKeyBinding(InputHandler.KEYBOARD,
		                             KeyStroke.getKeyStroke(keyBindings.getString("restartScript")).getKeyCode());

		screen.setFont(new Font(Sandbox.class.getResourceAsStream("/tinyfont.fv4")));

		ScriptCore.init();
	}

	private void tick() {
		INPUT_HANDLER.tick();

		ScriptCore.tick.call();

		if(RESTART_SCRIPT.pressed()) {
			screen.clear(0x000000);
			ScriptCore.init();
		}
	}

	private void render() {
		BufferStrategy bs = getBufferStrategy();
		if(bs == null) {
			createBufferStrategy(3);
			bs = getBufferStrategy();
		}

		ScriptCore.render.call();

		for(int i = 0; i < pixels.length; i++) {
			pixels[i] = screen.raster.getPixel(i);
		}

		Graphics g = bs.getDrawGraphics();
		g.drawImage(img, 0, 0, width * scale, height * scale, null);
		g.dispose();
		bs.show();
	}

	private Frame getFrame() {
		Frame frame = new Frame(CONFIG.getString("title"));

		// add the canvas
		Dimension size = new Dimension(width * scale, height * scale);
		setMaximumSize(size);
		setPreferredSize(size);
		setMinimumSize(size);

		frame.add(this);
		frame.pack();

		// center frame
		frame.setLocationRelativeTo(null);

		// add close action
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		return frame;
	}

	public static void main(String[] args) {
		INSTANCE.readConfig();

		Frame frame = INSTANCE.getFrame();
		frame.setVisible(true);

		INSTANCE.init();
		new Thread(INSTANCE).run();

	}

}