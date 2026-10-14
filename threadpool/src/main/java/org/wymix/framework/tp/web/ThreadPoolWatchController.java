package org.wymix.framework.tp.web;


import org.wymix.framework.tp.supports.ThreadPoolContext;
import org.wymix.framework.tp.supports.ThreadPoolMonitor;

import java.util.List;

public class ThreadPoolWatchController {

    public List<ThreadPoolContext> showAllThreadPoolContext() {
        return ThreadPoolMonitor.allThreadPoolContext();
    }

}
