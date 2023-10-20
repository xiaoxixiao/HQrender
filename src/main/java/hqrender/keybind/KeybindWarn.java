/*
 * Copyright (c) 2015 Jerrell Fang
 * This project is Open Source and distributed under The MIT License (MIT)
 * (http://opensource.org/licenses/MIT)
 * You should have received a copy of the The MIT License along with
 * this project. If not, see <http://opensource.org/licenses/MIT>.
 */
package hqrender.keybind;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;

/**
 * Created by Fang0716 on 7/2/2014.
 * <p/>
 * Just a warning.
 *
 * @author Meow J
 */
public class KeybindWarn {

    public KeybindWarn() {
        ClientRegistry
            .registerKeyBinding(new KeyBinding(I18n.format("itemrender.key.error"), Keyboard.KEY_NONE, "Item Render"));
    }
}
