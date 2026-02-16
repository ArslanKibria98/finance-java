package com.ksa.islamic.finance.test.temporal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Helper class to create mock activities for Temporal testing
 */
public class ActivityMocker {

    /**
     * Create a mock activity implementation
     */
    @SuppressWarnings("unchecked")
    public static <T> T mock(Class<T> activityInterface, TemporalTestHarness.MockActivity<T> mockActivity) {
        return (T) Proxy.newProxyInstance(
            activityInterface.getClassLoader(),
            new Class[]{activityInterface},
            new MockActivityInvocationHandler(mockActivity)
        );
    }

    /**
     * Invocation handler for mock activities
     */
    private static class MockActivityInvocationHandler implements InvocationHandler {
        private final TemporalTestHarness.MockActivity<?> mockActivity;

        public MockActivityInvocationHandler(TemporalTestHarness.MockActivity<?> mockActivity) {
            this.mockActivity = mockActivity;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            // Record invocation
            mockActivity.getInvocations().add(
                new TemporalTestHarness.ActivityInvocation(methodName, args)
            );

            // Check for exception
            if (mockActivity.exceptions.containsKey(methodName)) {
                throw mockActivity.exceptions.get(methodName);
            }

            // Return configured response
            if (mockActivity.responses.containsKey(methodName)) {
                return mockActivity.responses.get(methodName);
            }

            // Return default value for primitive types
            Class<?> returnType = method.getReturnType();
            if (returnType.isPrimitive()) {
                if (returnType == boolean.class) return false;
                if (returnType == int.class) return 0;
                if (returnType == long.class) return 0L;
                if (returnType == double.class) return 0.0;
                if (returnType == float.class) return 0.0f;
                if (returnType == short.class) return (short) 0;
                if (returnType == byte.class) return (byte) 0;
                if (returnType == char.class) return '\0';
            }

            return null;
        }
    }
}