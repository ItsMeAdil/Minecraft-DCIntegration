package com.poke.dcintegration.config;

import java.util.List;

public class CustomCommand {
    public String name;
    public String description;
    public boolean adminOnly;
    public String mcCommand;
    public List<CustomCommandArg> args;

    public static class CustomCommandArg {
        public String name;
        public String description;
        public boolean optional;
    }
}