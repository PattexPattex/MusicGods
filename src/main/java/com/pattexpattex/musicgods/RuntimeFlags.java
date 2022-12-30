package com.pattexpattex.musicgods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RuntimeFlags {
    
    private static final Logger log = LoggerFactory.getLogger(RuntimeFlags.class);
    
    private final List<Flags> flags;
    
    RuntimeFlags(String[] args) {
        this.flags = readArgs(args);
    }
    
    public int getRawFlags() {
        return flags.stream().mapToInt(flag -> 1 << flag.offset).sum();
    }
    
    public List<Flags> getFlags() {
        return flags;
    }
    
    private List<Flags> readArgs(String[] args) {
        return Arrays.stream(args)
                .map(Flags::fromInput)
                .filter(Objects::nonNull)
                .toList();
    }
    
    public enum Flags {
        UPDATE(0, "up", "update"),
        LAZY(1, "l", "lazy"),
        VERBOSE(2, "v", "verbose")
        ;
        
        public final int offset;
        public final String shortFlag;
        public final String longFlag;
        
        Flags(int offset, String shortFlag, String longFlag) {
            this.offset = offset;
            this.shortFlag = shortFlag;
            this.longFlag = longFlag;
        }
        
        @Nullable
        public static Flags fromInput(String string) {
            return Arrays.stream(Flags.values())
                    .filter(flag -> ("-" + flag.shortFlag).equals(string) || ("--" + flag.longFlag).equals(string))
                    .findFirst().orElseGet(() -> {
                        log.warn("Unknown argument '{}'", string);
                        return null;
                    });
        }
        
        public boolean isActive() {
            return Bot.getRuntimeFlags().contains(this);
        }
    }
}
