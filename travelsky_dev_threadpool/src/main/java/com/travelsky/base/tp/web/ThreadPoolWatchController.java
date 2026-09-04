package com.travelsky.base.tp.web;


import com.travelsky.base.tp.supports.ThreadPoolContext;
import com.travelsky.base.tp.supports.ThreadPoolMonitor;

import java.util.List;

public class ThreadPoolWatchController {

    public List<ThreadPoolContext> showAllThreadPoolContext() {
        return ThreadPoolMonitor.allThreadPoolContext();
    }

}
