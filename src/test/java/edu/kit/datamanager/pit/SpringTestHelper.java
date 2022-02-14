package edu.kit.datamanager.pit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.context.ApplicationContext;

public class SpringTestHelper {

    ApplicationContext app;

    public SpringTestHelper(ApplicationContext app) {
        this.app = app;
    }

    public void assertSingleBeanInstanceOf(Class<?> classType) {
        int propertyInstanceAmount = app
            .getBeansOfType(classType)
            .values()
            .size();
        assertEquals(propertyInstanceAmount, 1);
    }

    public void assertNoBeanInstanceOf(Class<?> classType) {
        int propertyInstanceAmount = app
            .getBeansOfType(classType)
            .values()
            .size();
        assertEquals(propertyInstanceAmount, 0);
    }
}
