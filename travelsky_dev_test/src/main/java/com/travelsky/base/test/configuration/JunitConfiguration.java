package com.travelsky.base.test.configuration;

import com.travelsky.base.test.listener.DataSetSupportListener;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, DataSetSupportListener.class})
public class JunitConfiguration {
}
