package com.ksa.financing.identity.infrastructure.casbin;

import com.googlecode.aviator.runtime.type.AviatorBoolean;
import com.googlecode.aviator.runtime.type.AviatorObject;
import org.casbin.jcasbin.util.function.CustomFunction;
import org.springframework.util.AntPathMatcher;

import java.util.Map;

public class PathMatchFunction extends CustomFunction {

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        String requestPath = arg1.getValue(env).toString();
        String policyPattern = arg2.getValue(env).toString();
        if ("*".equals(policyPattern)) {
            return AviatorBoolean.TRUE;
        }
        boolean match = ANT_PATH_MATCHER.match(policyPattern, requestPath);
        return AviatorBoolean.valueOf(match);
    }

    @Override
    public String getName() {
        return "pathMatch";
    }
}
