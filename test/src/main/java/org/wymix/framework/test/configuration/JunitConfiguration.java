package org.wymix.framework.test.configuration;

import org.wymix.framework.test.listener.DataSetSupportListener;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, DataSetSupportListener.class})
public class JunitConfiguration {
}
