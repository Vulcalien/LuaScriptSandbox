package vulc.luasandbox.script;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

public final class ScriptCore {

	private ScriptCore() {
	}

	public static final String SCRIPT_FOLDER = "./scripts/";

	public static LuaFunction tick;
	public static LuaFunction render;

	public static void init() {
		Globals env = new Globals();

		// must load
		env.load(new JseBaseLib());
		env.load(new PackageLib());

		// math
		env.load(new Bit32Lib());
		env.load(new JseMathLib());

		// utils
		env.load(new TableLib());
		env.load(new StringLib());

		// these are not loaded by default
//		env.load(new CoroutineLib());
//		env.load(new JseIoLib());
//		env.load(new JseOsLib());
//		env.load(new LuajavaLib());

		// disable require - use loadscript instead
		env.set("require", LuaValue.NIL);

		LoadState.install(env);
		LuaC.install(env);

		LuaInterface.init(env);

		// read main.lua
		try(InputStream in = new BufferedInputStream(new FileInputStream(SCRIPT_FOLDER + "main.lua"))) {
			env.load(in, "@main.lua", "t", env).call();
		} catch(IOException e) {
			e.printStackTrace();
		}

		// get tick and render functions
		tick = env.get("tick").checkfunction();
		render = env.get("render").checkfunction();

		// call init function
		env.get("init").call();
	}

}
