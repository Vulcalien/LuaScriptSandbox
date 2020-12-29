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

import vulc.bitmap.Bitmap;
import vulc.bitmap.IntBitmap;
import vulc.bitmap.font.Font;
import vulc.luasandbox.script.ScriptCore;

public class Game extends Canvas implements Runnable {

	private static final long serialVersionUID = 1L;

	private static final Game INSTANCE = new Game();

	public static final int WIDTH = 256, HEIGHT = 256;
	public static final Bitmap<Integer> SCREEN = new IntBitmap(WIDTH, HEIGHT);

	private static final int SCALE = 2;
	private static final BufferedImage IMG = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
	private static final int[] PIXELS = ((DataBufferInt) IMG.getRaster().getDataBuffer()).getData();

	private static int ticks = 0;

	public void run() {
		int frames = 0;

		long tps = 60;
		long fps = 60;

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

	private void init() {
		SCREEN.setFont(new Font(Game.class.getResourceAsStream("/tinyfont.fv4")));

		ScriptCore.init();
	}

	private void tick() {
		ScriptCore.tick.call();
	}

	private void render() {
		BufferStrategy bs = getBufferStrategy();
		if(bs == null) {
			createBufferStrategy(3);
			bs = getBufferStrategy();
		}

		ScriptCore.render.call();

		for(int i = 0; i < PIXELS.length; i++) {
			PIXELS[i] = SCREEN.raster.getPixel(i);
		}

		Graphics g = bs.getDrawGraphics();
		g.drawImage(IMG, 0, 0, WIDTH * SCALE, HEIGHT * SCALE, null);
		g.dispose();
		bs.show();
	}

	private Frame getFrame() {
		Frame frame = new Frame();

		// add the canvas
		Dimension size = new Dimension(WIDTH * SCALE, HEIGHT * SCALE);
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
		Frame frame = INSTANCE.getFrame();
		frame.setVisible(true);

		INSTANCE.init();
		new Thread(INSTANCE).run();
	}

}
