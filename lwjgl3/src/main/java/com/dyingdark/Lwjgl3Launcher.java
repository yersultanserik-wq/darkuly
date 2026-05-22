package com.dyingdark;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Dying Dark");
        config.setWindowedMode(960, 640);
        config.setForegroundFPS(60);
        config.setResizable(false);
        new Lwjgl3Application(new DyingDarkGame(), config);
    }
}
