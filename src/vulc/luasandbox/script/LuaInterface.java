package vulc.luasandbox.script;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import vulc.luasandbox.Sandbox;

public final class LuaInterface {

	private LuaInterface() {
	}

	private static Globals env;

	protected static void init(Globals env) {
		LuaInterface.env = env;

		env.set("scr_w", Sandbox.width);
		env.set("scr_h", Sandbox.height);

		// utils
		env.set("loadscript", new loadscript());

		// screen
		env.set("settransparent", new settransparent());
		env.set("clear", new clear());
		env.set("pix", new pix());
		env.set("write", new write());
	}

	// --- FUNCTIONS --- \\

	// utils

	private static class loadscript extends OneArgFunction {
		public LuaValue call(LuaValue script) {
			try(InputStream in =
			        new BufferedInputStream(new FileInputStream(Sandbox.SCRIPT_FOLDER + script.toString()))) {
				env.load(in, "@" + script, "t", env).call();
			} catch(IOException e) {
				e.printStackTrace();
			}
			return NIL;
		}
	}

	// screen
	private static class settransparent extends OneArgFunction {
		public LuaValue call(LuaValue color) {
			Sandbox.screen.setTransparent(color.checkint());
			return NIL;
		}
	}

	private static class clear extends OneArgFunction {
		public LuaValue call(LuaValue color) {
			Sandbox.screen.clear(color.checkint());
			return NIL;
		}
	}

	private static class pix extends VarArgFunction {
		public Varargs invoke(Varargs args) {
			int x = args.arg(1).checkint();
			int y = args.arg(2).checkint();
			int color = args.arg(3).checkint();
			int w = args.arg(4).isnil() ? 1 : args.arg(4).checkint();
			int h = args.arg(5).isnil() ? 1 : args.arg(5).checkint();

			Sandbox.screen.fill(x, y, x + w - 1, y + h - 1, color);
			return NIL;
		}
	}

	private static class write extends VarArgFunction {
		public Varargs invoke(Varargs args) {
			String text = args.arg(1).toString();
			int color = args.arg(2).checkint();
			int x = args.arg(3).checkint();
			int y = args.arg(4).checkint();

			Sandbox.screen.write(text, color, x, y);
			return NIL;
		}
	}

}
