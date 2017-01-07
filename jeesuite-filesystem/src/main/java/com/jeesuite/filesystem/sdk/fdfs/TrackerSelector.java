package com.jeesuite.filesystem.sdk.fdfs;

import java.util.List;
import java.util.Random;


public enum TrackerSelector {

    ROUND_ROBIN {
        private int idx;

        @Override
        public TrackerServer select(List<TrackerServer> list) {
            idx %= list.size();
            return list.get(idx++);
        }

    },
    RANDOM {
        private final Random random = new Random();

        @Override
        public TrackerServer select(List<TrackerServer> list) {
            return list.get(random.nextInt(list.size()));
        }

    },
    FIRST {
        @Override
        TrackerServer select(List<TrackerServer> list) {
            return list.get(0);
        }

    };

    abstract TrackerServer select(List<TrackerServer> list);
}