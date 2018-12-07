package de.upb.cs.dice.opal.conversion.converter;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LogMonitor {
    private static final Logger logger = LoggerFactory.getLogger(LogMonitor.class.getName());

    //
    @Before("execution(* de.upb.cs.dice.opal.conversion..*(..)) ")
    public void beforeLogger(JoinPoint joinPoint) {
        StringBuilder res = getPrototypeOfFunction(joinPoint);
        logger.debug("Before Call: " + joinPoint.getClass().getName() + "." + joinPoint.getSignature().getName() + ", input args: [" + res + "]");
    }

    @After("execution(* de.upb.cs.dice.opal.conversion..*(..)) ")
    public void afterLogger(JoinPoint joinPoint) {
        StringBuilder res = getPrototypeOfFunction(joinPoint);
        logger.debug("After Call: " + joinPoint.getClass().getName() + "." + joinPoint.getSignature().getName() + ", input args: [" + res + "]");
    }

    private StringBuilder getPrototypeOfFunction(JoinPoint joinPoint) {
        StringBuilder res = new StringBuilder();
        Arrays.stream(joinPoint.getArgs()).forEach((obj) -> res.append(obj).append(", "));
        int lastIndexOfComma = res.lastIndexOf(",");
        if (lastIndexOfComma != -1)
            res.delete(lastIndexOfComma, res.length());
        return res;
    }
}

